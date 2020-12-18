package ai.platon.scent.examples.sites.amazon

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.Systems
import ai.platon.scent.ScentContext
import ai.platon.scent.common.sql.SqlUtils
import ai.platon.scent.context.withContext
import ai.platon.scent.examples.common.Crawler
import org.apache.commons.lang3.StringUtils

class Portal1Depth1CssPathCrawler(context: ScentContext): Crawler(context) {

    fun run() {
        val url = "https://www.amazon.com/gp/browse.html?node=16713337011&ref_=nav_em_0_2_8_5_sbdshd_cameras"
        val args = "-i 7d -ii 30d -ol a[href~=/dp/]"
        val sql = SqlUtils.loadSql("/sql/x-items-load-out-pages.sql")
        val cssQueries = sql.split("\n").filter { " as " in it }.mapNotNull {
            StringUtils.substringBetween(it, "'", "'")?.takeIf { it.length > 8 }
        }

        var i = 0
        loadOutPages(url, args).forEach { page ->
            val startTime = System.currentTimeMillis()
            println("${++i}.\t-----------------------------------")

            cssQueries.forEach { cssQuery ->
                val value = session.parse(page).selectFirstOrNull(cssQuery)?.text()
                String.format("%3d.%100s  ->  %s", i, cssQuery, value).also { println(it) }
            }

            val elapsedTime = DateTimes.elapsedTime(startTime)
            println("Time cost: $elapsedTime")
        }
    }
}

fun main() {
    Systems.loadAllProperties("portal1Depth1Crawler.properties")
    withContext {
        Portal1Depth1CssPathCrawler(it).run()
    }
}
