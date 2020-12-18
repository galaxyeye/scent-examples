package ai.platon.scent.examples.misc

import ai.platon.pulsar.common.url.Hyperlink
import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.crawl.StreamingCrawler
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.scent.context.ScentContexts
import kotlinx.coroutines.runBlocking

/**
 * Scan the database and fetch each page immediately
 * */
fun main() {
    Systems.setProperty(CapabilityTypes.FETCH_CONCURRENCY, 2 * AppConstants.FETCH_THREADS)

    val i = ScentContexts.createSession()
    val webDb = i.context.getBean<WebDb>()
    val scanUrlPrefix = ""
    val scanFields = listOf(GWebPage.Field.PROTOCOL_STATUS)
    val maxRecords = 50
    val options = LoadOptions.parse("-i 1s")

    runBlocking {
        webDb.scan(scanUrlPrefix, scanFields).asSequence()
                .map { Hyperlink(it.url) }
//                .dropWhile { Instant.now() > stopTime }
                .take(maxRecords)
                .let { StreamingCrawler(it, options) }.run()
    }
}
