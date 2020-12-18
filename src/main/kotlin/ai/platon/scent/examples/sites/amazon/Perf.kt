package ai.platon.scent.examples.sites.amazon

import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.scent.ql.h2.context.withSQLContext
import kotlinx.coroutines.runBlocking

fun main() = withSQLContext { cx ->
    val resourcePrefix = "config/sites/amazon/crawl/parse/sql"
    val fields = listOf(GWebPage.Field.BASE_URL, GWebPage.Field.CONTENT)
    val sqls = cx.scan("https://www.amazon.com/", fields)
            .asSequence()
            .filter { it.url.contains("/dp/") }
            .filter { it.content?.array()?.size?:0 > 2000 }
            .take(1000)
            .map { it.url to "crawl/x-asin.sql" }
            .toList()
    val xsqlFilter = { xsql: String -> "x-asin.sql" in xsql }

    runBlocking {
        sqls.chunked(sqls.size / 10).toList().parallelStream().forEach {
            XSqlRunner(it, xsqlFilter, resourcePrefix, cx).run()
        }
    }
}
