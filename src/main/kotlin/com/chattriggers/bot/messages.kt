package com.chattriggers.bot

import com.chattriggers.bot.types.Module
import com.chattriggers.bot.types.Release
import com.jessecorbett.diskord.api.rest.client.ChannelClient
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.dsl.footer
import com.jessecorbett.diskord.dsl.image
import com.jessecorbett.diskord.util.sendMessage
import io.ktor.util.KtorExperimentalAPI
import me.xdrop.fuzzywuzzy.FuzzySearch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@KtorExperimentalAPI
suspend fun ChannelClient.onCreateModule(module: Module) {
    sendMessage("") {
        title = "Module created: ${module.name}"
        url = "https://www.chattriggers.com/modules/v/${module.name}"
        field("Author", module.owner.name, true)

        if (module.tags.isNotEmpty())
            field("Tags", module.tags.joinToString(), true)

        field("Description", module.description, false)

        if (module.image.isNotBlank())
            image(module.image)

        color = CTBot.MESSAGE_COLOR
        timestamp = Instant.now().toString()
    }
}

@KtorExperimentalAPI
suspend fun ChannelClient.onCreateRelease(module: Module, release: Release) {
    sendMessage("") {
        title = "Release created for module: ${module.name}"
        url = "https://www.chattriggers.com/modules/v/${module.name}"

        field("Author", module.owner.name, true)
        field("Release Version", release.releaseVersion, true)
        field("Mod Version", release.modVersion, true)
        field("Changelog", release.changelog, false)

        color = CTBot.MESSAGE_COLOR
    }
}

@KtorExperimentalAPI
suspend fun ChannelClient.onDeleteModule(module: Module) {
    sendMessage("") {
        title = "Module deleted: ${module.name}"
        color = CTBot.MESSAGE_COLOR
    }
}

@KtorExperimentalAPI
suspend fun ChannelClient.mcpFieldMessage(
    name: String, obf: Boolean, fields: List<Field>, username: String, ownerClass: String? = null
) {
    sendMessage("") {
        title = "MCP field search results for \"$name\""
        color = CTBot.MESSAGE_COLOR
        footer("Query by $username")
        timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)

        val sorted = if (ownerClass != null) {
            fields.sortedBy {
                FuzzySearch.ratio(it.owner, ownerClass)
            }.reversed()
        } else fields

        field("\u200B", sorted.joinToString("\n") {
            val dispName = if (obf) it.name else it.obfName
            val subName = if (obf) it.obfName else it.name
            val subTitle = if (obf) "Name" else "Deobf name"

            "**•** `${it.owner}.\u200B$dispName`" +
            "\n\u2002\u2002 $subTitle: `$subName`"
        }, false)
    }
}

@KtorExperimentalAPI
suspend fun ChannelClient.mcpMethodMessage(name: String, obf: Boolean, methods: List<Method>, username: String, ownerClass: String? = null) {
    sendMessage("") {
        title = "MCP method search results for \"$name\""
        color = CTBot.MESSAGE_COLOR
        footer("Query by $username")
        timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)

        val sorted = if (ownerClass != null) {
            methods.sortedBy {
                FuzzySearch.ratio(it.owner, ownerClass)
            }.reversed()
        } else methods

        field("\u200B", sorted.joinToString("\n") {
            val separator = if (it.static) '.' else '#'
            val n = if (obf) it.name else it.obfName

            "**•** `${it.owner}\u200B$separator\u200B$n`" +
            "\n\u2002\u2002 Signature: `${it.signature}`"
        }, false)
    }
}

@KtorExperimentalAPI
suspend fun ChannelClient.mcpClassMessage(name: String, classes: List<Class>, username: String) {
    sendMessage("") {
        title = "MCP class search results for \"$name\""
        color = CTBot.MESSAGE_COLOR
        footer("Query by $username")
        timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)

        field("\u200B", classes.joinToString("\n") {
            "**•** `${it.path}`"
        }, false)
    }
}

@KtorExperimentalAPI
suspend fun ChannelClient.helpMessage(errorMsg: String = "") {
    sendMessage(errorMsg) {
        title = "CTBot Help"
        description = """
            CTBot is the friendly ChatTriggers bot designed to help you with all of your CT needs!
        """.trimIndent()

        field("MCP Mapping Lookup", """
            `!mcp <type> <name>`
            
            `<type>` can be `class`, `method`, or `field`.
            `<name>` can be any word. It does not have to be an exact match, and can optionally be obfuscated.
        """.trimIndent(), false)

        field("ChatTriggers Doc Lookup", """
            `!javadocs <search>`
            
            `<search>` can be the name of any public class, object, field, or method.
        """.trimIndent(), false)

        color = CTBot.MESSAGE_COLOR
    }
}