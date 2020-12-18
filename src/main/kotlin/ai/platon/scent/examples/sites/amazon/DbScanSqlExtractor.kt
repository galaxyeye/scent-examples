package ai.platon.scent.examples.sites.amazon

import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.scent.context.ScentContexts
import ai.platon.scent.context.withContext
import ai.platon.scent.examples.common.AmazonStreamingSqlExtractor
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.system.measureTimeMillis

class DbSanSqlExtractor(
        private val start: Int = 0,
        private val limit: Int = Int.MAX_VALUE
) {
    private val log = LoggerFactory.getLogger(DbSanSqlExtractor::class.java)
    private val scanUrlPrefix = "https://www.amazon.com/"
    private val scanFields = listOf(GWebPage.Field.PROTOCOL_STATUS, GWebPage.Field.CONTENT)
    private val scanMinimumContentSize = 80_000
    private val session = ScentContexts.createSession()

    fun run() {
        val millis = measureTimeMillis {
            val streamingSqlExtractor = AmazonStreamingSqlExtractor(scanSequence())
            runBlocking { streamingSqlExtractor.run() }
        }

//        val speed = Duration.ofMillis(millis / numRecords.coerceAtLeast(1))
//        log.info("Extracted {} records in {}, speed: {} per page",
//                numRecords, Duration.ofMillis(millis).readable(), speed.readable())
    }

    private fun scanSequence(): Sequence<WebPage> {

        val webDb = session.context.getBean<WebDb>()
        return webDb.scan(scanUrlPrefix, scanFields).asSequence()
                .filter { it.key.contains("/dp/") }
                .filter { it.protocolStatus.isSuccess && it.content?.array()?.size ?: 0 > scanMinimumContentSize }
                .drop(start)
                .take(limit)
    }
}

fun main(args: Array<String>) {
    var start = 0
    var limit = 500
    var i = 0
    while (i < args.size) {
        if (args[i] == "-start") {
            start = args[++i].toIntOrNull()?:start
        } else if (args[i] == "-limit") {
            limit = args[++i].toIntOrNull()?:limit
        }
        ++i
    }

    withContext {
        DbSanSqlExtractor(start, limit).run()
    }
}
