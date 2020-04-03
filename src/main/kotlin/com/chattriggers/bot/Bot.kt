package com.chattriggers.bot

import com.chattriggers.bot.types.*
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.yield
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.utils.MemberCachePolicy
import java.io.File
import java.time.Instant
import java.util.*

@KtorExperimentalAPI
suspend fun main() {
    CTBot.init()
}

@KtorExperimentalAPI
object CTBot {
    private const val MESSAGE_COLOR = 0x7b2fb5
    private const val CHANNEL_ID = 366740283943157760

    private val gson = Gson()
    private val client = HttpClient(CIO) { install(WebSockets) }
    private val channel: TextChannel

    init {
        val token = Properties()
            .apply { load(File(".env.properties").reader()) }
            .getProperty("bot_token")

        val bot = JDABuilder.create(token, listOf())
            .setMemberCachePolicy(MemberCachePolicy.NONE)
            .setEnabledCacheFlags(null)
            .build()
            .awaitReady()

        channel = bot.getGuildChannelById(CHANNEL_ID) as TextChannel
    }

    suspend fun init() {
        client.ws(
            method = HttpMethod.Get,
            host = "chattriggers.com",
            path = "/api/events"
        ) {
            pingIntervalMillis = 60000

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
                    is CreateModuleEvent -> channel.onCreateModule(event.module)
                    is CreateReleaseEvent -> channel.onCreateRelease(event.module, event.release)
                    is DeleteModuleEvent -> channel.onDeleteModule(event.module)
                }

                yield()
            }
        }
    }

    private fun TextChannel.onCreateModule(module: Module) {
        EmbedBuilder().apply {
            setTitle("Module created: ${module.name}", "https://www.chattriggers.com/modules/v/${module.name}")
            addField("Author", module.owner.name, true)

            if (module.tags.isNotEmpty())
                addField("Tags", module.tags.joinToString(), true)

            addField("Description", module.description, false)

            if (module.image.isNotBlank())
                setImage(module.image)

            setColor(MESSAGE_COLOR)
            setTimestamp(Instant.now())
        }.build().let(::sendMessage).queue()
    }

    private fun TextChannel.onCreateRelease(module: Module, release: Release) {
        EmbedBuilder().apply {
            setTitle(
                "Release created for module: ${module.name}",
                "https://www.chattriggers.com/modules/v/${module.name}"
            )

            addField("Author", module.owner.name, true)
            addField("Release Version", release.releaseVersion, true)
            addField("Mod Version", release.modVersion, true)
            addField("Changelog", release.changelog, false)

            setColor(MESSAGE_COLOR)
        }.build().let(::sendMessage).queue()
    }

    private fun TextChannel.onDeleteModule(module: Module) {
        EmbedBuilder().apply {
            setTitle("Module deleted: ${module.name}")

            setColor(MESSAGE_COLOR)
        }.build().let(::sendMessage).queue()
    }
}