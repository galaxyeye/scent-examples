package ai.platon.scent.examples.common

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.crawl.parse.ParseFilters
import ai.platon.pulsar.crawl.parse.html.ParseContext
import ai.platon.pulsar.persist.WebPage
import ai.platon.scent.common.sql.SqlTemplate
import ai.platon.scent.context.ScentContexts
import ai.platon.scent.parse.html.JdbcSinkSqlExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.slf4j.LoggerFactory
import java.lang.Thread.sleep
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

open class AmazonStreamingSqlExtractor(val pages: Sequence<WebPage>) {
    val log = LoggerFactory.getLogger(Crawler::class.java)
    val session = ScentContexts.createSession()
    val closed = AtomicBoolean()
    val numRunning = AtomicInteger()
    val isAppActive get() = !closed.get() && session.isActive
    val sqlExtractor: JdbcSinkSqlExtractor get() = session.context.getBean()
    val parseFilters: ParseFilters get() = session.context.getBean()

    init {
        sqlExtractor.sqlTemplate = SqlTemplate.load("sites/amazon/sql/x-items-converted.sql")
        parseFilters.addFirst(sqlExtractor)
    }

    open suspend fun run() {
        supervisorScope {
            pages.forEach { page ->
                if (!isAppActive) return@supervisorScope

                numRunning.incrementAndGet()

                launch(Dispatchers.Default) {
                    val document = session.parse(page, noCache = true)
                    session.cache(page)
                    session.cache(document)
                    if (isAppActive) {
                        val parseContext = ParseContext(page)
                        sqlExtractor.filter(parseContext)
                    }
                    session.disableCache(page)
                    session.disableCache(document)
                    numRunning.decrementAndGet()
                }

                while (isAppActive && numRunning.get() >= AppContext.NCPU) {
                    sleep(200)
                }
            }
        }
    }
}
