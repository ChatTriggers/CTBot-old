package com.chattriggers.bot.types

data class CreateReleaseEvent(
    val type: String,
    val module: Module,
    val release: Release
)

data class Release(
    val id: String,
    val releaseVersion: String,
    val modVersion: String,
    val changelog: String,
    val downloads: Int
)