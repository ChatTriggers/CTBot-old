package com.chattriggers.bot

import com.chattriggers.bot.types.*
import com.copperleaf.kodiak.common.DocElement
import com.copperleaf.kodiak.kotlin.models.KotlinModuleDoc
import com.google.gson.Gson
import com.jessecorbett.diskord.api.rest.client.ChannelClient
import com.jessecorbett.diskord.dsl.*
import com.jessecorbett.diskord.util.sendMessage
import com.jessecorbett.diskord.util.words
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.readText
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.yield
import me.xdrop.fuzzywuzzy.FuzzySearch
import java.io.File
import java.time.Instant
import java.util.*

@KtorExperimentalAPI
suspend fun main() {
    CTBot.init()
}

data class SearchTerm(
    val element: DocElement,
    val url: String,
    val descriptor: String
)

@KtorExperimentalAPI
object CTBot {
    private const val MESSAGE_COLOR = 0x7b2fb5
    private const val CHANNEL_ID = "366740283943157760"

    private val gson = Gson()
    private val client = HttpClient(CIO) { install(WebSockets) }
    private lateinit var channel: ChannelClient

    private lateinit var docs: KotlinModuleDoc
    private val searchTerms = mutableListOf<SearchTerm>()

    suspend fun init() {
        println("Generating KDocs...")

        docs = KDocGenerator.getDocs()

        docs.classes.filter { clazz ->
            clazz.modifiers.publicMember()
        }.forEach { clazz ->
            val name = clazz.id.replace("${clazz.`package`}.", "")
            val pkg = clazz.`package`.replace('.', '/')

            val urlBase = "https://chattriggers.com/javadocs/$pkg/$name.html"

            SearchTerm(
                clazz,
                urlBase,
                "${clazz.kind.toLowerCase()} $name"
            ).run(searchTerms::add)

            clazz.methods.filter { method ->
                method.modifiers.publicMember()
            }.map { method ->
                val url = StringBuilder(urlBase).apply {
                    append("#${method.name}-")

                    if (method.receiver != null) {
                        append(":Dreceiver-")
                    }

                    method.parameters.joinToString("-") { it.name }.run(::append)

                    append('-')
                }.toString()

                val returnType = when (method.returnValue.name) {
                    "()" -> "Unit"
                    else -> method.returnValue.signature.joinToString("") { it.text }
                }

                val descriptor = StringBuilder().apply {
                    append(clazz.name)

                    when (clazz.kind) {
                        "Object", "Enum" -> append(".")
                        "Class", "Interface" -> append("#")
                        else -> throw IllegalStateException("Unrecognized class kind: ${clazz.kind}")
                    }

                    append(method.name)
                    append("(")

                    method.parameters.joinToString {
                        it.signature.joinToString("") { c -> c.text }
                    }.run(::append)

                    append("): ")
                    append(returnType)
                }.toString()

                SearchTerm(
                    method,
                    url,
                    descriptor
                )
            }.run(searchTerms::addAll)

            clazz.fields.filter { field ->
                field.modifiers.publicMember()
            }.map { field ->
                SearchTerm(
                    field,
                    "$urlBase#${field.name}",
                    "field ${field.name}"
                )
            }.run(searchTerms::addAll)
        }

        println("KDocs generated")

        println("Building bot...")
        buildBot()
        println("Bot built")

        println("Setting up websockets...")
        setupWebsockets()
        println("Websocket setup")
    }

    private suspend fun setupWebsockets() {
        client.ws(
            method = HttpMethod.Get,
            host = "chattriggers.com",
            path = "/api/events"
        ) {
            pingIntervalMillis = 60000

            while (true) {
                val frame = incoming.receive() as io.ktor.http.cio.websocket.Frame.Text
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

    private suspend fun buildBot() {
        val token = Properties()
            .apply { load(File(".env.properties").reader()) }
            .getProperty("bot_token")

        bot(token) {
            started {
                channel = clientStore.channels[CHANNEL_ID]
            }

            commands(prefix = "!") {
                command("javadocs") {
                    val top = FuzzySearch.extractTop(words[1], searchTerms, { it.element.name }, 5)
                        .map { it.referent }

                    reply("") {
                        title = "Search results for \"${words[1]}\""

                        description = top.joinToString("\n") {
                            "[${it.descriptor}](${it.url})"
                        }
                    }
                }
            }
        }
    }

    private suspend fun ChannelClient.onCreateModule(module: Module) {
        sendMessage("") {
            title = "Module created: ${module.name}"
            url = "https://www.chattriggers.com/modules/v/${module.name}"
            field("Author", module.owner.name, true)

            if (module.tags.isNotEmpty())
                field("Tags", module.tags.joinToString(), true)

            field("Description", module.description, false)

            if (module.image.isNotBlank())
                image(module.image)

            color = MESSAGE_COLOR
            timestamp = Instant.now().toString()
        }
    }

    private suspend fun ChannelClient.onCreateRelease(module: Module, release: Release) {
        sendMessage("") {
            title = "Release created for module: ${module.name}"
            url = "https://www.chattriggers.com/modules/v/${module.name}"

            field("Author", module.owner.name, true)
            field("Release Version", release.releaseVersion, true)
            field("Mod Version", release.modVersion, true)
            field("Changelog", release.changelog, false)

            color = MESSAGE_COLOR
        }
    }

    private suspend fun ChannelClient.onDeleteModule(module: Module) {
        sendMessage("") {
            title = "Module deleted: ${module.name}"
            color = MESSAGE_COLOR
        }
    }

    private fun List<String>.publicMember() = !contains("internal") && !contains("private")
}