package com.chattriggers.bot.messages

import com.chattriggers.bot.CTBot
import com.chattriggers.bot.Class
import com.chattriggers.bot.Field
import com.chattriggers.bot.Method
import com.jessecorbett.diskord.api.rest.client.ChannelClient
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.dsl.footer
import com.jessecorbett.diskord.util.sendMessage
import io.ktor.util.KtorExperimentalAPI
import me.xdrop.fuzzywuzzy.FuzzySearch
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@KtorExperimentalAPI
suspend fun ChannelClient.mcpClassMessage(name: String, classes: List<Class>, username: String) {
    sendMessage("") {
        title = "MCP class search results for \"$name\""

        description = classes.joinToString("\n") {
            "**•** `${it.path}`"
        }

        footer("Query by $username")
        timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
        color = CTBot.MESSAGE_COLOR
    }
}

@KtorExperimentalAPI
suspend fun ChannelClient.mcpMethodMessage(name: String, obf: Boolean, methods: List<Method>, username: String, ownerClass: String? = null) {
    sendMessage("") {
        title = "MCP method search results for \"$name\""

        val sorted = if (ownerClass != null) {
            methods.sortedBy {
                FuzzySearch.ratio(it.owner, ownerClass)
            }.reversed()
        } else methods

        description = sorted.joinToString("\n\n") {
            val firstName = if (obf) it.obfName else it.name
            val secondName = if (obf) it.name else it.obfName

            "**•** `$firstName` → `$secondName`" +
            "\n\u2002\u2002 Owner: \u2002\u2002`${it.owner}`" +
            "\n\u2002\u2002 Signature: `${it.signature}`"
        }

        footer("Query by $username")
        timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
        color = CTBot.MESSAGE_COLOR
    }
}

@KtorExperimentalAPI
suspend fun ChannelClient.mcpFieldMessage(
    name: String, obf: Boolean, fields: List<Field>, username: String, ownerClass: String? = null
) {
    sendMessage("") {
        title = "MCP field search results for \"$name\""

        val sorted = if (ownerClass != null) {
            fields.sortedBy {
                FuzzySearch.ratio(it.owner, ownerClass)
            }.reversed()
        } else fields

        description = sorted.joinToString("\n\n") {
            val firstName = if (obf) it.obfName else it.name
            val secondName = if (obf) it.name else it.obfName

            "**•** `$firstName` → `$secondName`" +
            "\n\u2002\u2002 Owner: `${it.owner}`"
        }

        footer("Query by $username")
        timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
        color = CTBot.MESSAGE_COLOR
    }
}