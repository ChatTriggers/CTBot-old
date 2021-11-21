package com.chattriggers.bot.messages

import com.chattriggers.bot.CTBot
import com.chattriggers.bot.footer
import com.chattriggers.bot.sendMessage
import com.jessecorbett.diskord.api.channel.ChannelClient
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

suspend fun ChannelClient.migrateMessage(username: String) {
    sendMessage {
        title = "Module Migration"

        description =
            "Modules made in CT 0.18.4 are not compatible with " +
            "CT 1.0.0+. In order to migrate your modules, see " +
            "[our migration guide](https://github.com/ChatTriggers/ChatTriggers/blob/master/MIGRATION.md)."

        footer("Query by $username")
        timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
        color = CTBot.MESSAGE_COLOR
    }
}