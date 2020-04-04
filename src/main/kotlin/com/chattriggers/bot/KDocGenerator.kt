package com.chattriggers.bot

import com.copperleaf.kodiak.kotlin.KotlindocInvokerImpl
import com.copperleaf.kodiak.kotlin.models.KotlinModuleDoc
import org.eclipse.jgit.api.Git
import java.io.File
import java.nio.file.Files

object KDocGenerator {
    fun getDocs(): KotlinModuleDoc {
        val ctjsDir = File("./ctjs")

        if (!ctjsDir.exists()) {
            println("Cloning repo...")
            Git.cloneRepository()
                .setURI("https://github.com/ChatTriggers/ct.js.git")
                .setBranchesToClone(listOf("refs/heads/master"))
                .setBranch("refs/heads/master")
                .setDirectory(ctjsDir)
                .call()
            println("Repo cloned")
        }

        val cacheDir = Files.createTempDirectory("dokkaCache")
        val runner = KotlindocInvokerImpl(cacheDir)

        val outputDir = File("build/dokka").canonicalFile.apply {
            deleteRecursively()
            mkdirs()
        }

        return runner.getModuleDoc(
            listOf(File("./ctjs/src/main/kotlin").toPath()),
            outputDir.toPath()
        ) {
            Runnable { println(it.bufferedReader().readText()) }
        }!!
    }
}