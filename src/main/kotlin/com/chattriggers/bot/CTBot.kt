package com.chattriggers.bot

import com.chattriggers.bot.messages.*
import com.chattriggers.bot.types.*
import com.google.gson.Gson
import com.jessecorbett.diskord.api.model.ChannelType
import com.jessecorbett.diskord.api.model.GuildMember
import com.jessecorbett.diskord.api.rest.client.ChannelClient
import com.jessecorbett.diskord.dsl.*
import com.jessecorbett.diskord.util.words
import com.vdurmont.emoji.EmojiManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.yield
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@KtorExperimentalAPI
suspend fun main() {
    CTBot.init()
}

var formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("'['MM/dd/yy']' '['hh:mm:ss:SSS a z']' ").withZone(ZoneId.of("UTC"))

fun logInfo(message: String) {
    print(LocalDateTime.now().format(formatter))
    print("[INFO] ")
    println(message)
}

fun logWarn(message: String) {
    print(LocalDateTime.now().format(formatter))
    print("\u001b[38;2;255;0;102m")
    print("[WARNING] ")
    print(message)
    println("\u001b[0m")
}

@KtorExperimentalAPI
object CTBot {
    const val PRODUCTION = true

    const val MESSAGE_COLOR = 0x7b2fb5
    private const val MODULES_CHANNEL = "366740283943157760"
    private const val BOTLAND_CHANNEL = "435654238216126485"
    private const val NO_EMOJI_ROLE = "745047588381917216"
    private const val NO_QUOTES_ROLE = "746096240978296953"

    lateinit var searchTerms: List<SearchTerm>
    private val gson = Gson()
    private val client = HttpClient(CIO) { install(WebSockets) }
    private lateinit var channel: ChannelClient
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

                            channel.onCreateModule(module)
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

                            channel.onCreateRelease(module, release)
                        }
                        is DeleteModuleEvent -> {
                            logInfo("Deleted module ${event.module.name} (${event.module.id})")
                            channel.onDeleteModule(event.module)
                        }
                    }

                    yield()
                }
            } catch (e: Exception) {
                areWebsocketsSetup = false
                close(e)
                setupWebsockets()
            }
        }
    }

    private suspend fun buildBot() {
        val token = Properties()
            .apply { load(File(".env.properties").reader()) }
            .getProperty("bot_token")

        bot(token) {
            started {
                channel = clientStore.channels[MODULES_CHANNEL]

                logInfo("Setting up websockets")
                setupWebsockets()
                logInfo("Websockets setup")
            }

            messageCreated { message ->
                if (message.content == "bot" && message.channelId == BOTLAND_CHANNEL) {
                    logInfo("botland")
                    message.reply("land")
                } else if (message.partialMember?.roleIds?.contains(NO_EMOJI_ROLE) == true && containsEmoji(message.content)) {
                    logInfo("Deleting message from user ${message.partialMember!!.nickname}: ${message.content}")
                    message.delete()
                } else if (message.partialMember?.roleIds?.contains(NO_QUOTES_ROLE) == true && containsQuote(message.content)) {
                    logInfo("Deleting quote from user ${message.partialMember!!.nickname}: ${message.content}")
                    message.delete()
                }
            }

            messageUpdated { message ->
                val channel = clientStore.channels[message.channelId].get()
                if (channel.guildId == null || message.author == null)
                    return@messageUpdated
                val user = clientStore.guilds[channel.guildId!!].getMember(message.author!!.id)

                if (user.roleIds.contains(NO_EMOJI_ROLE) && message.content?.let(::containsEmoji) == true) {
                    logInfo("Deleting edited message from user ${user.nickname}: ${message.content}")
                    message.delete()
                } else if (user.roleIds.contains(NO_QUOTES_ROLE) && message.content?.let(::containsQuote) == true) {
                    logInfo("Deleting quote message from user ${user.nickname}: ${message.content}")
                    message.delete()
                }
            }

            commands(prefix = "!") {
                command("javadocs") {
                    if (!allowedInChannel(partialMember, channel, "javadocs")) return@command

                    logInfo("Searching javadocs for ${words[1]}")

                    docsMessage(this)
                }

                command("docs") {
                    if (!allowedInChannel(partialMember, channel, "docs")) return@command

                    logInfo("Searching javadocs for ${words[1]}")

                    docsMessage(this)
                }

                command("mcp") {
                    if (!allowedInChannel(partialMember, channel, "mcp")) return@command

                    if (words.size < 3) {
                        logWarn("User provided ${words.size} arguments to !mcp, sending error message")

                        channel.helpMessage(author.username, "Too few arguments provided to `!mcp` command")
                        return@command
                    }

                    val type = when (val t = words[1].toLowerCase()) {
                        "field", "method", "class" -> t
                        else -> {
                            logWarn("User provided unrecognized type to !mcp: $t")
                            channel.helpMessage(author.username, "Unrecognized type `$t`. Valid types are: `method`, `field`, `class`")
                            return@command
                        }
                    }

                    val isObf = words[2].startsWith("func_") || words[2].startsWith("field_")
                    val third = if (words.size > 3) words[3] else null

                    when (type) {
                        "field" -> {
                            val fields = MCPService.fieldsFromName(words[2], isObf)
                            channel.mcpFieldMessage(words[2], isObf, fields, author.username, third)
                        }
                        "method" -> {
                            val methods = MCPService.methodsFromName(words[2], isObf)
                            channel.mcpMethodMessage(words[2], isObf, methods, author.username, third)
                        }
                        "class" -> {
                            val classes = MCPService.classesFromName(words[2])
                            channel.mcpClassMessage(words[2], classes, author.username)
                        }
                    }
                }

                command("migrate") {
                    if (!allowedInChannel(partialMember, channel, "migrate")) return@command

                    logInfo("Sending migrate message to ${author.username}")
                    channel.migrateMessage(author.username)
                }

                command("help") {
                    if (!allowedInChannel(partialMember, channel, "help")) return@command

                    logInfo("Sending help message to ${author.username}")
                    channel.helpMessage(author.username)
                }

                command("links") {
                    if (!allowedInChannel(partialMember, channel, "links")) return@command

                    logInfo("Sending links message to ${author.username}")
                    channel.linkMessage(author.username)
                }

                command("notworking") {
                    if (!allowedInChannel(partialMember, channel, "notworking")) return@command

                    logInfo("Sending not working message to ${channel.channelId}")
                    channel.notWorkingMessage(author.username)
                }

                command("learnjs") {
                    if (!allowedInChannel(partialMember, channel, "learnjs")) return@command

                    logInfo("Sending learnjs message to ${channel.channelId}")
                    channel.learnJsMessage(author.username)
                }
            }
        }
    }

    private suspend fun allowedInChannel(member: GuildMember?, channel: ChannelClient, commandName: String): Boolean {
        val isAllowed = let {
            if (!PRODUCTION) return@let true

            val type = channel.get().type
            if (type == ChannelType.DM || type == ChannelType.GROUP_DM) return@let true

            if (member == null) return@let false
            if (channel.channelId == BOTLAND_CHANNEL) return@let true

            return@let member.roleIds.any {
                it in allowedRoles
            }
        }


        if (!isAllowed) {
            val username = member?.nickname ?: "<null>"
            val id = member?.user?.id ?: "<null>"

            logInfo("Getting channel name for channel ${channel.channelId}")
            val channelName = channel.get().name

            logWarn("Member $username ($id) is not allowed to use !$commandName in channel $channelName (${channel.channelId})")
        }

        return isAllowed
    }

    private fun containsEmoji(message: String) = message.contains(customEmojiRegex) || EmojiManager.containsEmoji(message)

    private fun containsQuote(message: String) = message.contains(quoteRegex)
}
