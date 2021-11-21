package com.chattriggers.bot.messages

import com.chattriggers.bot.CTBot
import com.chattriggers.bot.footer
import com.chattriggers.bot.sendMessage
import com.jessecorbett.diskord.api.channel.ChannelClient
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

suspend fun ChannelClient.notWorkingMessage(username: String) {
    sendMessage {
        title = "Not Working?"

        description = """
            Hello! If your code isn't functioning in the way you think it should, you've come to the right place!
            The members of the CT discord are happy to help you, but we aren't mind readers.
            In order for us to support you, we'll need some information.
            Most importantly, we need to see your code and an error message if one exists.
            To check for error messages type `/ct console js` into Minecraft.
        """.trimIndent()

        footer("Query by $username")
        timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
        color = CTBot.MESSAGE_COLOR
    }
}
