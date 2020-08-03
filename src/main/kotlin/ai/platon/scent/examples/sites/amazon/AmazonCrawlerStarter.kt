package ai.platon.scent.examples.sites.amazon

import ai.platon.scent.component.RepeatP1D1Crawler
import ai.platon.scent.context.ScentContexts
import ai.platon.scent.context.support.AbstractScentContext
import ai.platon.scent.examples.component.ParserInitializer
import ai.platon.scent.examples.sites.amazon.config.AmazonCrawlerConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.stereotype.Component

@Component
class AmazonParserInitializer {
    @Autowired
    lateinit var parserInitializer: ParserInitializer

    fun initialize() {
        parserInitializer.report()
        parserInitializer.addSqlExtractor(".+/dp/.+",  500_000, 20,
                "sites/amazon/sql/x-items-final.sql", "asin_sync_utf8mb4")
        parserInitializer.addSqlExtractor(".+/seller/.+",  100_000, 8,
                "sites/amazon/sql/x-sellers-v20200717.sql", "seller_sync")
        parserInitializer.addSqlExtractor(".+/product-reviews/.+",  100_000, 10,
                "sites/amazon/sql/x-reviews-v20200717.sql", "asin_review_sync")
    }
}

fun main(args: Array<String>) {
    var start = 0
    var limit = 0
    var test = false

    var i = 0
    while (i < args.size) {
        when {
            args[i] == "-cstart" -> start = args[++i].toInt()
            args[i] == "-climit" -> limit = args[++i].toInt()
            args[i] == "-test" -> test = true
        }
        ++i
    }

    val context = AnnotationConfigApplicationContext(AmazonCrawlerConfig::class.java)
            .apply { register(RepeatP1D1Crawler::class.java) }
            .apply { register(ParserInitializer::class.java, AmazonParserInitializer::class.java) }
            .let { ScentContexts.activate(it) as AbstractScentContext }

    context.getBean<AmazonParserInitializer>().initialize()

    val crawler = context.getBean<RepeatP1D1Crawler>()

    if (test) crawler.test() else crawler.run()
}
