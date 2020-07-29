package ai.platon.scent.examples.tools

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.Frequency
import ai.platon.pulsar.common.LinkExtractors
import org.apache.commons.lang3.StringUtils
import java.nio.file.Files
import java.nio.file.Paths

fun collectFromLogs() {
    val links = LinkExtractors.fromDirectory(Paths.get("logs")).toList()

    links.forEachIndexed { i, link ->
        println("$i.\t$link")
    }

    val path = AppPaths.get("generated_links.txt")
    Files.deleteIfExists(path)
    Files.write(path, links)

    val frequency: Frequency<String> = links.flatMapTo(Frequency()) {
        it.substringAfter("?").split("&").map { it.substringBefore("=") }
    }
    var report = frequency.entrySet()
            .sortedByDescending { it.count }
            .filter { it.count > 10 }
            .mapIndexed { i, e -> "$i.\t" + e.element + ": " + e.count }
            .joinToString("\n") { it }
    println(report)

    println()
    println("Unique links report: ")
    frequency.clear()
    links.mapNotNullTo(frequency) {
        StringUtils.substringBetween(it, ".com/", "/dp/")
    }
    // println(frequency.toReport())
    println(frequency.elementSet().size)
}

fun main() {
    collectFromLogs()
}
