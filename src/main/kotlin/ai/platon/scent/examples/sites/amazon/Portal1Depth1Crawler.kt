package ai.platon.scent.examples.sites.amazon

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.crawl.parse.ParseFilters
import ai.platon.pulsar.crawl.parse.html.ParseContext
import ai.platon.scent.context.support.AbstractScentContext
import ai.platon.scent.context.withContext
import ai.platon.scent.examples.common.Crawler
import ai.platon.scent.parse.html.JdbcSinkSqlExtractor

class Portal1Depth1Crawler(
        val url: String, val args: String, val context: AbstractScentContext
): Crawler() {

    val sqlExtractor: JdbcSinkSqlExtractor get() = context.getBean()
    val parseFilters: ParseFilters get() = context.getBean()

    init {
        sqlExtractor.session = session.pulsarSession
        sqlExtractor.sqlTemplateResource = "sites/amazon/sql/x-items-converted.sql"
        parseFilters.addFirst(sqlExtractor)
    }

    fun run() {
        var i = 0
        loadOutPages(url, args).forEach {
            val startTime = System.currentTimeMillis()
            println("${++i}.\t-----------------------------------")
            sqlExtractor.filter(ParseContext(it))
            val elapsedTime = DateTimes.elapsedTime(startTime)
            println("Time cost: $elapsedTime")
        }
    }
}

fun main() {
    Systems.loadAllProperties("config/sites/amazon/application-p1d1.properties")

    val url = "https://www.amazon.com/gp/browse.html?node=16713337011&ref_=nav_em_0_2_8_5_sbdshd_cameras"
    val args = "-i 7d -ii 30d -ol a[href~=/dp/]"
    withContext {
        Portal1Depth1Crawler(url, args, it as AbstractScentContext).run()
    }
}
