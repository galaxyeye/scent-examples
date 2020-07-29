package ai.platon.scent.examples.tools

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.crawl.StreamingCrawler
import ai.platon.pulsar.dom.select.collectNotNull
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.scent.ScentContext
import ai.platon.scent.context.ScentContexts
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue

fun main() {
    System.setProperty(CapabilityTypes.BROWSER_MAX_ACTIVE_TABS, "30")
    System.setProperty(CapabilityTypes.PROXY_ENABLE_DEFAULT_PROVIDERS, "true")

    // ProxyManager.enableDefaultProviders()
    // ProxyManager.disableProviders()
    val i = ScentContexts.createSession()
    val webDb = i.pulsarContext.getBean<WebDb>()
    val scanUrlPrefix = "https://www.amazon.com/"
    val scanFields = listOf(GWebPage.Field.PROTOCOL_STATUS, GWebPage.Field.FETCH_TIME)
    val maxRecords = 5000

    val args = "-i 1s"
    val options = LoadOptions.parse(args)

    val portalPages = ConcurrentLinkedQueue<WebPage>()
    runBlocking {
        LinkExtractors.fromResource("/amazon-categories.txt")
                .asSequence()
                .let { StreamingCrawler(it, options).apply { pageCollector = portalPages } }
                .run()
    }

    portalPages.iterator().asSequence().map {
        i.parse(it).document.collectNotNull {
            it.attr("abs:href").takeIf { it.matches("http.+/dp/.+".toRegex()) }
        }
    }.forEachIndexed { j, url -> println("$j.\t$url") }

    val days100 = Instant.now().minus(Duration.ofDays(100))
//    runBlocking {
//        webDb.scan(scanUrlPrefix, scanFields).asSequence()
//                .filter { it.fetchTime < days100 }
//                .map { it.url }
//                .filter { it.contains("/dp/") }
//                .take(maxRecords)
//                .let { StreamingCrawler(it, options) }.run()
//    }

    ScentContexts.shutdown()
}
