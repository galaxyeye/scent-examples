package ai.platon.scent.examples.sites.amazon

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.crawl.parse.ParseFilters
import ai.platon.pulsar.crawl.parse.html.ParseContext
import ai.platon.scent.component.RepeatP1D1Crawler
import ai.platon.scent.context.ScentContexts
import ai.platon.scent.context.support.AbstractScentContext
import ai.platon.scent.context.support.GenericScentContext
import ai.platon.scent.examples.common.Crawler
import ai.platon.scent.examples.component.ParserInitializer
import ai.platon.scent.examples.sites.amazon.config.AmazonCrawlerConfig
import ai.platon.scent.context.withContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext

class MainResourceSimpleCrawler(val urlResource: String, val args: String): Crawler() {
    val parseFilters: ParseFilters = session.pulsarContext.getBean()
    val loadOptions = LoadOptions.parse(args)

    fun run(n: Int) {
        val urls = LinkExtractors.fromResource(urlResource).take(n)
        urls.forEach { url ->
            val page = session.load(url, loadOptions)
            val document = session.parse(page)
            val path = session.export(page, "crawler", ".htm")
            parseFilters.filter(ParseContext(page))
        }
    }
}

fun main() {
    Systems.setProperty(CapabilityTypes.PROXY_USE_PROXY, false)

    val context = AnnotationConfigApplicationContext(AmazonCrawlerConfig::class.java)
            .apply { register(RepeatP1D1Crawler::class.java) }
            .apply { register(ParserInitializer::class.java) }
            .let { ScentContexts.activate(GenericScentContext(it)) as AbstractScentContext }

    val initializer = context.getBean<ParserInitializer>()
    initializer.minSyncBatchSize = 2
    initializer.addSqlExtractor(".+/dp/.+",  500_000, 20,
            "sites/amazon/sql/x-items-final.sql", "asin_sync_utf8mb4")
    initializer.addSqlExtractor(".+/seller/.+",  100_000, 8,
            "sites/amazon/sql/x-sellers-v20200717.sql", "seller_sync")
    initializer.addSqlExtractor(".+/product-reviews/.+",  100_000, 8,
            "sites/amazon/sql/x-reviews-v20200717.sql", "asin_review_sync")

    withContext {
        val crawler1 = MainResourceSimpleCrawler("sites/amazon/seller/sellers.txt", "-i 1h")
        val crawler2 = MainResourceSimpleCrawler("sites/amazon/review/reviews.txt", "-i 1d")

        crawler2.run(20)
    }
}
