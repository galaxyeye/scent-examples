package ai.platon.scent.examples.sites.amazon.category

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.ResourceLoader
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class LeafCategory {
    val categoryTreePath = "sites/amazon/category/best-sellers/best-sellers.txt"
    private val dataDirectory = AppPaths.getTmp("category").resolve(DateTimes.formatNow("HHmm"))
    private val leafCategoriesPath = dataDirectory.resolve("leafCategories.txt")

    fun run() {
        Files.createDirectories(leafCategoriesPath.parent)
        Files.deleteIfExists(leafCategoriesPath)

        var i = 0
        ResourceLoader.readAllLines(categoryTreePath)
                .map { GlobalCategoryDataNode.parse(it) }
                .filter { it.numChildren == 0 }
                .forEach {
                    ++i
                    Files.writeString(leafCategoriesPath, it.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND)
                }

        println("Total $i categories")
    }
}

fun main(args: Array<String>) {
    LeafCategory().run()
}
