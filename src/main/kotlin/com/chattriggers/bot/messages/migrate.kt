package com.chattriggers.bot.messages

import com.chattriggers.bot.CTBot
import com.jessecorbett.diskord.api.rest.client.ChannelClient
import com.jessecorbett.diskord.dsl.footer
import com.jessecorbett.diskord.util.sendMessage
import io.ktor.util.KtorExperimentalAPI
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@KtorExperimentalAPI
suspend fun ChannelClient.migrateMessage(username: String) {
    sendMessage("") {
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