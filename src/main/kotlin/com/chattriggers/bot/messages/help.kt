package com.chattriggers.bot.messages

import com.chattriggers.bot.CTBot
import com.jessecorbett.diskord.api.rest.client.ChannelClient
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.dsl.footer
import com.jessecorbett.diskord.util.sendMessage
import io.ktor.util.KtorExperimentalAPI
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@KtorExperimentalAPI
suspend fun ChannelClient.helpMessage(username: String, errorMsg: String = "") {
    sendMessage(errorMsg) {
        title = "CTBot Help"
        description = """
            CTBot is the friendly ChatTriggers bot designed to help you with all of your CT needs!
        """.trimIndent()

        field("Links", """
            Run `!links` for a list of useful links
        """.trimIndent(), false)

        field("MCP Mapping Lookup", """
            `!mcp <type> <name> [owner]`
            
            `<type>` can be `class`, `method`, or `field`.
            `<name>` can be any word. It does not have to be an exact match, and can optionally be obfuscated.
            `[owner]` can optionally be the (partial) name of the owning class.
        """.trimIndent(), false)

        field("ChatTriggers Doc Lookup", """
            `!javadocs <search>`
            
            `<search>` can be the name of any public class, object, field, or method.
        """.trimIndent(), false)

        footer("Query by $username")
        timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
        color = CTBot.MESSAGE_COLOR
    }
}