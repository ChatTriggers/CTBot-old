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
            You need some basic JavaScript knowledge to use ChatTriggers, and the best place to do that is a dedicated JavaScript tutorial. Here are some good places to start!
            [Official Documentation](https://developer.mozilla.org/en-US/docs/Web/JavaScript)
            [Tutorial with Interactive Editor](https://www.codecademy.com/learn/introduction-to-javascript)
        """.trimIndent()

        footer("Query by $username")
        timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
        color = CTBot.MESSAGE_COLOR
    }
}
