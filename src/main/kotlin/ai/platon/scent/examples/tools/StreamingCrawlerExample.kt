package ai.platon.scent.examples.tools

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.crawl.Crawler
import ai.platon.scent.ScentContext
import ai.platon.scent.examples.common.StreamingSqlCrawler
import ai.platon.scent.context.withContext
import ai.platon.scent.ql.h2.context.ScentSQLContext
import ai.platon.scent.ql.h2.context.withSQLContext
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths

class StreamingCrawlerExample(val context: ScentSQLContext): Crawler(context) {
    private val args = "-i 7d -ii 7d"
    private val options = LoadOptions.parse(args)
    private val links = LinkExtractors.fromDirectory(Paths.get("/tmp/pulsar-vincent/category/1625")).toList()

    fun run() {
        runBlocking {
            StreamingSqlCrawler(links.asSequence(), options, context).run()
        }
    }
}

fun main() {
    Systems.loadAllProperties("application.properties")

    withSQLContext {
        StreamingCrawlerExample(it).run()
    }
}
