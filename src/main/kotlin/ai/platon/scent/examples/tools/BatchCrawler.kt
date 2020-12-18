package ai.platon.scent.examples.tools

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.crawl.Crawler
import ai.platon.scent.context.withContext
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths

class BatchFetcher: Crawler() {
    private val log = LoggerFactory.getLogger(BatchFetcher::class.java)

    private val args = "-i 7d -ii 7d"
    private val options = LoadOptions.parse(args)
    private val itemOptions = options.createItemOptions()
    private val links = LinkExtractors.fromDirectory(Paths.get("logs")).toList()
    private var round = 0

    fun run() {
        val path = AppPaths.getTmp("generated-urls.txt")
        Files.deleteIfExists(path)
        Files.write(path, links)

        links.withIndex().groupBy { it.index % 20 }.forEach { (i, group) ->
            try {
                if (!session.isActive) return@forEach

                log.info("\n\n\n--------------------------\nRound ${++round}")
                val pages = session.loadAll(group.map { it.value }, itemOptions)
                // report(pages)
                // privacyContextManager.reset()
            } catch (e: Throwable) {
                log.warn("Unexpected throwable", e)
            }
        }
    }
}

fun main() {
    withContext {
        BatchFetcher().run()
    }
}
