package ai.platon.scent.examples.sites.amazon

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.crawl.parse.ParseFilters
import ai.platon.pulsar.crawl.parse.html.ParseContext
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.scent.common.sql.SqlTemplate
import ai.platon.scent.context.support.AbstractScentContext
import ai.platon.scent.context.withContext
import ai.platon.scent.examples.common.Crawler
import ai.platon.scent.parse.html.JdbcSinkSqlExtractor
import org.apache.commons.lang3.StringUtils
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.select.NodeFilter
import org.jsoup.select.NodeTraversor

class Portal1Depth1Crawler(
        val url: String, val args: String, val context: AbstractScentContext
): Crawler() {

    val sqlExtractor: JdbcSinkSqlExtractor get() = context.getBean()
    val parseFilters: ParseFilters get() = context.getBean()

    init {
        sqlExtractor.sqlTemplate = SqlTemplate.load("sites/amazon/sql/extract/crawl/x-asin.sql")
        parseFilters.addFirst(sqlExtractor)
    }

    fun run() {
        var i = 0
        loadOutPages(url, args).forEach {
            val startTime = System.currentTimeMillis()
            println("${++i}.\t-----------------------------------")
            val document = session.parse(it)
            findParentAsin(document)

            sqlExtractor.filter(ParseContext(it))
            val elapsedTime = DateTimes.elapsedTime(startTime)
            println("Time cost: $elapsedTime")
        }
    }

    private fun findParentAsin(document: FeaturedDocument) {
        var pasin: String? = null
        NodeTraversor.filter(object: NodeFilter {
            override fun head(node: Node, depth: Int): NodeFilter.FilterResult {
                if (node is Element && node.tagName() == "script") {
                    pasin = StringUtils.substringBetween(node.data(), "parentAsin=", "&")
                }
                return if (pasin == null) NodeFilter.FilterResult.CONTINUE else NodeFilter.FilterResult.STOP
            }
        }, document.body)

        if (pasin != null) {
            println(pasin)
            document.body.attr("_ps_pasin", pasin)
        }
    }
}

fun main() {
    Systems.loadAllProperties("config/sites/amazon/other/application-p1d1.properties")

    val url = "https://www.amazon.com/gp/browse.html?node=16713337011&ref_=nav_em_0_2_8_5_sbdshd_cameras"
    val args = "-i 7d -ii 30d -ol a[href~=/dp/]"
    withContext {
        Portal1Depth1Crawler(url, args, it as AbstractScentContext).run()
    }
}
