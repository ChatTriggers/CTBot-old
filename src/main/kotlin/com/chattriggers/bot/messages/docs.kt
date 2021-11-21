package com.chattriggers.bot.messages

import com.chattriggers.bot.CTBot
import com.chattriggers.bot.sanitizeInput
import com.jessecorbett.diskord.api.common.Message
import com.jessecorbett.diskord.bot.BotContext
import com.jessecorbett.diskord.util.words
import me.xdrop.fuzzywuzzy.FuzzySearch

suspend fun BotContext.docsMessage(message: Message) {
    val top = FuzzySearch.extractTop(message.words[1], CTBot.searchTerms, { it.name }, 5).map { it.referent }

    message.reply("") {
        title = "Search results for \"${sanitizeInput(message.words[1])}\""

        description = top.joinToString("\n") {
            "[${it.descriptor}](${it.url})"
        }
    }
}
