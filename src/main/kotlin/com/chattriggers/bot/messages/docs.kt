package com.chattriggers.bot.messages

import com.chattriggers.bot.CTBot
import com.chattriggers.bot.sanitizeInput
import com.chattriggers.bot.sendMessage
import com.jessecorbett.diskord.api.channel.ChannelClient
import com.jessecorbett.diskord.api.common.Message
import com.jessecorbett.diskord.util.words
import me.xdrop.fuzzywuzzy.FuzzySearch

suspend fun ChannelClient.docsMessage(message: Message) {
    val top = FuzzySearch.extractTop(message.words[1], CTBot.searchTerms, { it.name }, 5).map { it.referent }

    sendMessage {
        title = "Search results for \"${sanitizeInput(message.words[1])}\""

        description = top.joinToString("\n") {
            "[${it.descriptor}](${it.url})"
        }
    }
}
