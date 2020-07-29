package ai.platon.scent.examples.sites.amazon

import ai.platon.pulsar.common.Frequency
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.scent.context.ScentContexts
import ai.platon.scent.context.withContext
import org.slf4j.LoggerFactory

class DbScanner(
        private val start: Int = 0,
        private val limit: Int = Int.MAX_VALUE
) {
    private val log = LoggerFactory.getLogger(DbSanSqlExtractor::class.java)
    private val scanUrlPrefix = "https://www.amazon.com/"
    private val scanFields = listOf(GWebPage.Field.PROTOCOL_STATUS, GWebPage.Field.CONTENT)
    private val scanMinimumContentSize = 80_000
    private val session = ScentContexts.createSession()
    private val itemFrequency = Frequency<String>()

    fun run() {
        var i = 0
        scanSequence().forEach {
            it.url.substringAfter("amazon.com")
                    .split("/")
                    .filter { it.startsWith("ref=") }
                    .toCollection(itemFrequency)
            println("${i++}.\t" + it.url)
        }

        println(itemFrequency.toReport())
    }

    private fun scanSequence(): Sequence<WebPage> {
        val webDb = session.pulsarContext.getBean<WebDb>()
        return webDb.scan(scanUrlPrefix, scanFields).asSequence()
                .filter { it.key.contains("Best-Sellers") }
                .filter { it.protocolStatus.isSuccess && it.content?.array()?.size ?: 0 > scanMinimumContentSize }
                .drop(start)
                .take(limit)
    }
}

fun main(args: Array<String>) {
    var start = 0
    var limit = 2000
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
        DbScanner(start, limit).run()
    }
}
