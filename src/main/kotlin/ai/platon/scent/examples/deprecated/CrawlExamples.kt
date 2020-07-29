package ai.platon.scent.examples.deprecated

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.select.selectFirstOrNull
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.persist.metadata.FetchMode
import ai.platon.scent.context.ScentContexts
import org.jsoup.nodes.Element
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

object CrawlExamples {

    private var i = ScentContexts.createSession()
    private val baseUri = "http://www.ccgp-hubei.gov.cn/contract"
    private val contractBaseUri = "http://www.ccgp-hubei.gov.cn:8040/fcontractAction!download.action?path="
    private var banned = false

    fun crawl() {
        val optionalSeeds = IntRange(5, 200).shuffled().take(20).toMutableSet()
        listOf(1, 2, 3, 4).union(optionalSeeds)
                .map { "$baseUri//index_$it.html" }.map { crawlDetailPages(it) }
    }

    fun export() {
        i.pulsarContext.scan(contractBaseUri).forEachRemaining {
            val size = it.content?.array()?.size?:0

            if (size < 2000) {
                return@forEachRemaining
            }

            val flag = "Last-export-time"

            val now = LocalDateTime.now()
            val exportTime = it.metadata[flag]?.let { LocalDateTime.parse(it) }?:now

            if (exportTime.isAfter(now.minusHours(12))) {
                val ident = DateTimes.format(it.createTime, "yyyyMMdd")
                i.export(it, ident)

                it.metadata[flag] = now.toString()
                i.persist(it)
            }
        }
    }

    private fun crawlDetailPages(indexUrl: String) {
        i.load(indexUrl)
                .let { i.parse(it) }
                .select(".news-list-content li a") { it.attr("abs:href") }
                .asSequence()
                .filter { it.startsWith("http") }
                .map { i.load(it) }
                .map { i.parse(it) }
                .map { extract(it) }
                .mapNotNull { it["attachmentUrl"] }
                .filter { i.get(it)?.content?.array()?.size?:0 < 4000 }
                .mapNotNull { loadDelay(it, Duration.ofSeconds(30)) }
                .toList()
                // .onEach { i.export(it) }

        val delay = 60L + Random().nextInt(90)
        Thread.sleep(Duration.ofSeconds(delay).toMillis())
    }

    private fun extract(doc: FeaturedDocument): Map<String, String?> {
        return mapOf(
                "title" to doc.first(".con_info h2") { it.text() },
                "url" to doc.location,
                "art_info" to doc.first(".con_info .art_info") { it.text() },
                "table" to doc.first(".con_info table") { format(it) },
                "attachmentUrl" to doc.first(".con_info table tr:last-child a") { contractUrl(it) },
                "attachmentName" to doc.first(".con_info table tr:last-child a") { it.text() },
                "files" to doc.select(".con_info a") { it.attr("onclick") }.joinToString()
        )
    }

    private fun loadDelay(url: String, delay: Duration): WebPage? {
        if (banned) {
            return null
        }

        val loadOption = LoadOptions.parse("--expires=360d")
        loadOption.browser = BrowserType.NATIVE
        loadOption.fetchMode = FetchMode.NATIVE

        val bannedString = "overLimitIP"
        val page = i.loadDelay(url, delay, loadOption)
        if (bannedString in page.url || bannedString in page.reprUrl) {
            banned = true
        }
        return page
    }

    private fun format(ele: Element): String {
        return when (ele.tagName()) {
            "table" -> ele.select("tr").map {
                it.selectFirstOrNull("td:nth-child(1)")?.text() + ": " +
                        it.selectFirstOrNull("td:nth-child(2)")?.text()
            }.joinToString(" | ")
            else -> ele.outerHtml()
        }
    }

    private fun contractUrl(ele: Element): String {
        val s = ele.attr("onclick").substringAfter('\'').substringBefore('\'')
        return "$contractBaseUri$s"
    }
}

fun main() {
    // CrawlExamples.crawl()
    Files.list(Paths.get("/tmp/pulsar-vincent")).forEach {
        println(it)
    }
}
