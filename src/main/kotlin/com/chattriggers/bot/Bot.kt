package com.chattriggers.bot

import com.jessecorbett.diskord.dsl.bot
import com.jessecorbett.diskord.util.sendMessage
import java.io.File
import java.util.*

const val MY_CHANNEL = "..."

suspend fun main() {
    val envFile = File(".env.properties")
    val env = Properties().apply { load(envFile.reader()) }
    val botToken = env.getProperty("bot_token")

    val bot = bot(botToken) {
        started {
            val channel = clientStore.channels[MY_CHANNEL]
            channel.sendMessage("Heyo!")
        }
    }
}