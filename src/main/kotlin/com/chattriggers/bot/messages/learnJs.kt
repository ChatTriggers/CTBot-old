package com.chattriggers.bot.messages

import com.chattriggers.bot.CTBot
import com.jessecorbett.diskord.api.rest.client.ChannelClient
import com.jessecorbett.diskord.dsl.footer
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.sendMessage
import io.ktor.util.*
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@KtorExperimentalAPI
suspend fun ChannelClient.learnJsMessage(username: String) {
    sendMessage("") {
        title = "JavaScript Links"

        description = """
            You are expected to understand the basics of JavaScript before asking for help in the ChatTriggers discord. Here are some good places to start!
            [Official Documentation](https://developer.mozilla.org/en-US/docs/Web/JavaScript)
            [W3Schools Tutorial](https://www.w3schools.com/js/)
            [Tutorial with Interactive Editor](https://www.codecademy.com/learn/introduction-to-javascript)
        """.trimIndent()

        footer("Query by $username")
        timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
        color = CTBot.MESSAGE_COLOR
    }
}
