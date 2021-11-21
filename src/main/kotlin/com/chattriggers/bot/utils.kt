package com.chattriggers.bot

import com.jessecorbett.diskord.api.channel.*
import com.jessecorbett.diskord.util.sendEmbed
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

var formatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("'['MM/dd/yy']' '['hh:mm:ss:SSS a z']' ").withZone(ZoneId.of("UTC"))

fun logInfo(message: String) {
    print(LocalDateTime.now().format(formatter))
    print("[INFO] ")
    println(message)
}

fun logWarn(message: String) {
    print(LocalDateTime.now().format(formatter))
    print("\u001b[38;2;255;0;102m")
    print("[WARNING] ")
    print(message)
    println("\u001b[0m")
}

fun sanitizeInput(message: String) = message
    .replace("@", "\\@")
    .replace("~~", "\\~\\~")
    .replace("*", "\\*")
    .replace("`", "\\`")
    .replace("_", "\\_")

suspend fun ChannelClient.sendMessage(message: String = "", builder: Embed.() -> Unit) {
    createMessage(
        CreateMessage(
            message,
            allowedMentions = AllowedMentions.NONE,
            embed = Embed().apply(builder)
        )
    )
}

fun Embed.field(name: String, message: String, inline: Boolean) {
    fields.add(EmbedField(name, message, inline))
}

fun Embed.footer(text: String) {
    footer = EmbedFooter(text)
}

fun Embed.image(url: String) {
    image = EmbedImage(url)
}
