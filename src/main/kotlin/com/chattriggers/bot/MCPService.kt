package com.chattriggers.bot

import me.xdrop.fuzzywuzzy.FuzzySearch

data class Field(
    val name: String,
    val obfName: String,
    val owner: String
)

data class Method(
    val name: String,
    val obfName: String,
    val signature: String,
    val owner: String,
    val static: Boolean
)

data class Class(
    val name: String,
    val path: String
)

object MCPService {
    private lateinit var classes: List<Class>
    private lateinit var fields: List<Field>
    private lateinit var methods: List<Method>
    private lateinit var staticMethods: List<String>

    fun init() {
        staticMethods = MCPService::class.java.getResource("/static_methods.txt").readText().split('\n')

        val mcpLines = MCPService::class.java.getResource("/mcp-srg.srg").readText().split('\n')

        val mcpFields = mcpLines
            .filter { it.startsWith("FD: ") }
            .map { it.substring("FD: ".length) }

        val mcpMethods = mcpLines
            .filter { it.startsWith("MD: ") }
            .map { it.substring("MD: ".length) }

        classes = mcpLines
            .filter { it.startsWith("CL: ") }
            .map { it.substring("CL: ".length) }
            .map { it.split(' ')[0] }
            .map { Class(it.split('/').last(), it) }

        fields = mcpFields.map {
            val (path, obfPath) = it.split(' ')
            val name = path.split('/').last()
            val obfName = obfPath.split('/').last()
            val owner = path.split('/').dropLast(1).joinToString("/")

            Field(name, obfName, owner)
        }

        methods = mcpMethods.map {
            val (path, signature, obfPath) = it.split(' ')
            val name = path.split('/').last()
            val obfName = obfPath.split('/').last()
            val owner = path.split('/').dropLast(1).joinToString("/")

            Method(
                name,
                obfName,
                signature.replace(")", ")\u200B"),
                owner,
                staticMethods.contains(obfName)
            )
        }
    }

    fun fieldsFromName(name: String, obf: Boolean) = FuzzySearch.extractTop(name, fields, {
        if (obf) it.obfName else it.name
    }, 5).map { it.referent }

    fun methodsFromName(name: String, obf: Boolean) = FuzzySearch.extractTop(name, methods, {
        if (obf) it.obfName else it.name
    }, 5).map { it.referent }

    fun classesFromName(name: String) = FuzzySearch.extractTop(name, classes, {
        it.name
    }, 5).map { it.referent }
}