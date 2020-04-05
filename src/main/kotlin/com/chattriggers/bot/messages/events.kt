package com.chattriggers.bot

import com.chattriggers.bot.types.Module
import com.chattriggers.bot.types.Release
import com.jessecorbett.diskord.api.rest.client.ChannelClient
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.dsl.image
import com.jessecorbett.diskord.util.sendMessage
import io.ktor.util.KtorExperimentalAPI
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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
        timestamp = ZonedDateTime.now(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_INSTANT)
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
        timestamp = ZonedDateTime.now(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_INSTANT)
    }
}

@KtorExperimentalAPI
suspend fun ChannelClient.onDeleteModule(module: Module) {
    sendMessage("") {
        title = "Module deleted: ${module.name}"
        color = CTBot.MESSAGE_COLOR
        timestamp = ZonedDateTime.now(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_INSTANT)
    }
}