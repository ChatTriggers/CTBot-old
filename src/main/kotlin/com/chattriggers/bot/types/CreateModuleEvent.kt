package com.chattriggers.bot.types

data class CreateModuleEvent(
    val type: String,
    val module: Module
)

data class Module(
    val id: Int,
    val owner: Owner,
    val name: String,
    val description: String,
    val image: String,
    val downloads: Int,
    val tags: Array<String>,
    val releases: Array<Release>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Module

        if (
            id != other.id ||
            owner != other.owner ||
            name != other.name ||
            description != other.description ||
            image != other.image ||
            downloads != other.downloads ||
            !tags.contentEquals(other.tags) ||
            !releases.contentEquals(other.releases)
        ) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + owner.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + image.hashCode()
        result = 31 * result + downloads
        result = 31 * result + tags.contentHashCode()
        result = 31 * result + releases.contentHashCode()
        return result
    }
}

data class Owner(
    val id: Int,
    val name: String,
    val rank: String
)
