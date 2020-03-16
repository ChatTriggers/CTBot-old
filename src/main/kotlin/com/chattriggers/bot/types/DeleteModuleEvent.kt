package com.chattriggers.bot.types

data class DeleteModuleEvent(
    val type: String,
    val module: Module
)