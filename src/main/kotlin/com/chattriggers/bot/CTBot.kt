package com.chattriggers.bot

import com.chattriggers.bot.messages.*
import com.chattriggers.bot.types.*
import com.google.gson.Gson
import com.jessecorbett.diskord.api.channel.ChannelClient
import com.jessecorbett.diskord.api.common.DM
import com.jessecorbett.diskord.api.common.GuildMember
import com.jessecorbett.diskord.api.common.NamedChannel
import com.jessecorbett.diskord.bot.bot
import com.jessecorbett.diskord.bot.classicCommands
import com.jessecorbett.diskord.bot.events
import com.jessecorbett.diskord.util.words
import com.vdurmont.emoji.EmojiManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.*
import io.ktor.util.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.yield
import java.io.File
import java.util.*

suspend fun main() {
    CTBot.init()
}

object CTBot {
    const val PRODUCTION = true

    const val MESSAGE_COLOR = 0x7b2fb5
    private const val MODULES_CHANNEL_ID = "366740283943157760"
    private const val BOTLAND_CHANNEL_ID = "435654238216126485"
    private const val NO_EMOJI_ROLE_ID = "745047588381917216"
    private const val NO_QUOTES_ROLE_ID = "746096240978296953"

    lateinit var searchTerms: List<SearchTerm>
    private val gson = Gson()
    private val client = HttpClient(CIO) { install(WebSockets) }
    private lateinit var modulesChannel: ChannelClient
    private var areWebsocketsSetup = false

    private val customEmojiRegex = "<a?:\\w+:\\d+>".toRegex()
    private val quoteRegex = "> .+\\n<@[!&#]?[0-9]{16,18}>".toRegex()

    private val allowedRoles = listOf(
        "436707819752783872", // Admin
        "119493795434856448", // Developer
        "271357115006713858", // Moderator
        "270252320611106817", // Creator
        "420668245725413377"  // Patreon Supporter
    )

    suspend fun init() {
        logInfo("Generating KDocs")
        searchTerms = KDocGenerator.getSearchTerms()
        logInfo("KDocs generated")

        logInfo("Initializing MCPService")
        MCPService.init()
        logInfo("MCPService initialized")

        logInfo("Starting bot")
        buildBot()
    }

    private suspend fun setupWebsockets() {
        if (areWebsocketsSetup) {
            logInfo("Websockets already setup, returning")
            return
        }
        areWebsocketsSetup = true

        client.ws(
            method = HttpMethod.Get,
            host = "chattriggers.com",
            path = "/api/events"
        ) {
            pingIntervalMillis = 60000

            try {
                while (true) {
                    val frame = incoming.receive() as Frame.Text
                    val text = frame.readText()

                    val event: Any = when {
                        text.contains("release_created") -> gson.fromJson(text, CreateReleaseEvent::class.java)
                        text.contains("module_created") -> gson.fromJson(text, CreateModuleEvent::class.java)
                        text.contains("module_deleted") -> gson.fromJson(text, DeleteModuleEvent::class.java)
                        else -> throw IllegalStateException("Unrecognized websocket response")
                    }

                    when (event) {
                        is CreateModuleEvent -> {
                            val module = event.module

                            logInfo("Newly created module: ${module.name} (${module.id})")
                            logInfo("    Owner: ${module.owner.name} (${module.owner.id}, rank ${module.owner.rank})")
                            logInfo("    Description: ${module.description}")
                            logInfo("    Image: ${module.image}")
                            logInfo("    Downloads: ${module.downloads}")
                            logInfo("    Tags: ${module.tags.joinToString()}")
                            logInfo("    Number of releases: ${module.releases}") // Should always be zero

                            modulesChannel.onCreateModule(module)
                        }
                        is CreateReleaseEvent -> {
                            val module = event.module
                            val release = event.release

                            logInfo("Release created for module ${module.name} (${module.id})")
                            logInfo("    Id: ${release.id}")
                            logInfo("    Release version: ${release.releaseVersion}")
                            logInfo("    Mod version: ${release.modVersion}")
                            logInfo("    Changelog: ${release.changelog}")
                            logInfo("    Downloads: ${release.downloads}") // Should always be zero

                            modulesChannel.onCreateRelease(module, release)
                        }
                        is DeleteModuleEvent -> {
                            logInfo("Deleted module ${event.module.name} (${event.module.id})")
                            modulesChannel.onDeleteModule(event.module)
                        }
                    }

                    yield()
                }
            } catch (e: Exception) {
                areWebsocketsSetup = false
                cancel("Internal error", e)
                setupWebsockets()
            }
        }
    }

    private suspend fun buildBot() {
        val token = Properties()
            .apply { load(File(".env.properties").reader()) }
            .getProperty("bot_token")

        bot(token) {
            events {
                modulesChannel = channel(MODULES_CHANNEL_ID)

                onReady {
                    if (PRODUCTION) {
                        logInfo("Setting up websockets")
                        setupWebsockets()
                        logInfo("Websockets setup")
                    }
                }

                onMessageCreate {  message ->
                    if (message.content == "bot" && message.channelId == BOTLAND_CHANNEL_ID) {
                        logInfo("botland")
                        message.reply("land")
                    } else if (message.partialMember?.roleIds?.contains(NO_EMOJI_ROLE_ID) == true && containsEmoji(message.content)) {
                        logInfo("Deleting message from user ${message.partialMember!!.nickname}: ${message.content}")
                        message.delete()
                    } else if (message.partialMember?.roleIds?.contains(NO_QUOTES_ROLE_ID) == true && containsQuote(message.content)) {
                        logInfo("Deleting quote from user ${message.partialMember!!.nickname}: ${message.content}")
                        message.delete()
                    }
                }

                onMessageUpdate {  message ->
                    val user = message.partialMember ?: return@onMessageUpdate

                    if (user.roleIds.contains(NO_EMOJI_ROLE_ID) && containsEmoji(message.content)) {
                        logInfo("Deleting edited message from user ${user.nickname}: ${message.content}")
                        message.delete()
                    } else if (user.roleIds.contains(NO_QUOTES_ROLE_ID) && containsQuote(message.content)) {
                        logInfo("Deleting quote message from user ${user.nickname}: ${message.content}")
                        message.delete()
                    }
                }
            }

            classicCommands(commandPrefix = "!") {
                command("javadocs") {
                    if (!allowedInChannel(it.partialMember, modulesChannel, "javadocs"))
                        return@command

                    logInfo("Searching javadocs for ${it.words[1]}")

                    docsMessage(it)
                }

                command("docs") {
                    if (!allowedInChannel(it.partialMember, modulesChannel, "docs"))
                        return@command

                    logInfo("Searching javadocs for ${it.words[1]}")

                    docsMessage(it)
                }

                command("mcp") {
                    if (!allowedInChannel(it.partialMember, modulesChannel, "mcp"))
                        return@command

                    val words = it.words
                    val authorUsername = it.author.username

                    if (words.size < 3) {
                        logWarn("User provided ${words.size} arguments to !mcp, sending error message")

                        modulesChannel.helpMessage(authorUsername, "Too few arguments provided to `!mcp` command")
                        return@command
                    }

                    val type = when (val word = words[1].lowercase()) {
                        "field", "method", "class" -> word
                        else -> {
                            logWarn("User provided unrecognized type to !mcp: $word")
                            val sanitized = word.replace("`", "\\`")
                            modulesChannel.helpMessage(authorUsername, "Unrecognized type `$sanitized`. Valid types " +
                                "are: `method`, `field`, `class`")
                            return@command
                        }
                    }

                    val isObf = words[2].startsWith("func_") || words[2].startsWith("field_")
                    val third = if (words.size > 3) words[3] else null

                    when (type) {
                        "field" -> {
                            val fields = MCPService.fieldsFromName(words[2], isObf)
                            modulesChannel.mcpFieldMessage(words[2], isObf, fields, authorUsername, third)
                        }
                        "method" -> {
                            val methods = MCPService.methodsFromName(words[2], isObf)
                            modulesChannel.mcpMethodMessage(words[2], isObf, methods, authorUsername, third)
                        }
                        "class" -> {
                            val classes = MCPService.classesFromName(words[2])
                            modulesChannel.mcpClassMessage(words[2], classes, authorUsername)
                        }
                    }
                }

                command("migrate") {
                    val authorUsername = it.author.username

                    if (!allowedInChannel(it.partialMember, modulesChannel, "migrate"))
                        return@command

                    logInfo("Sending migrate message to $authorUsername")
                    modulesChannel.migrateMessage(authorUsername)
                }

                command("help") {
                    val authorUsername = it.author.username

                    if (!allowedInChannel(it.partialMember, modulesChannel, "help"))
                        return@command

                    logInfo("Sending help message to $authorUsername")
                    modulesChannel.helpMessage(authorUsername)
                }

                command("links") {
                    val authorUsername = it.author.username

                    if (!allowedInChannel(it.partialMember, modulesChannel, "links"))
                        return@command

                    logInfo("Sending links message to $authorUsername")
                    modulesChannel.linkMessage(authorUsername)
                }

                command("notworking") {
                    val authorUsername = it.author.username

                    if (!allowedInChannel(it.partialMember, modulesChannel, "notworking"))
                        return@command

                    logInfo("Sending not working message to ${modulesChannel.channelId}")
                    modulesChannel.notWorkingMessage(authorUsername)
                }

                command("learnjs") {
                    val authorUsername = it.author.username

                    if (!allowedInChannel(it.partialMember, modulesChannel, "learnjs"))
                        return@command

                    logInfo("Sending learnjs message to ${modulesChannel.channelId}")
                    modulesChannel.learnJsMessage(authorUsername)
                }
            }
        }
    }

    private suspend fun allowedInChannel(member: GuildMember?, channelClient: ChannelClient, commandName: String): Boolean {
        if (member == null)
            return false

        val channel = channelClient.getChannel()

        val isAllowed = !PRODUCTION
            || channel.id == BOTLAND_CHANNEL_ID
            || channel is DM
            || member.roleIds.any { it in allowedRoles }

        if (!isAllowed) {
            val username = member.nickname
            val id = member.user?.id

            logInfo("Getting channel name for channel ${channel.id}")
            val channelName = (channel as? NamedChannel)?.name ?: "<null>"

            logWarn("Member $username ($id) is not allowed to use !$commandName in channel $channelName (${channel.id})")
        }

        return isAllowed
    }

    private fun containsEmoji(message: String) = message.contains(customEmojiRegex) || EmojiManager.containsEmoji(message)

    private fun containsQuote(message: String) = message.contains(quoteRegex)
}
