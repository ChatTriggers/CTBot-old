package com.chattriggers.bot.messages

import com.chattriggers.bot.CTBot
import com.jessecorbett.diskord.api.model.ChannelType
import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.util.EnhancedEventListener
import com.jessecorbett.diskord.util.words
import io.ktor.util.KtorExperimentalAPI
import me.xdrop.fuzzywuzzy.FuzzySearch

@KtorExperimentalAPI
suspend fun EnhancedEventListener.docsMessage(message: Message) {
    val top = FuzzySearch.extractTop(message.words[1], CTBot.searchTerms, { it.name }, 5).map { it.referent }

    message.reply("") {
        title = "Search results for \"${message.words[1]}\""

        description = top.joinToString("\n") {
            "[${it.descriptor}](${it.url})"
        }
    }
}