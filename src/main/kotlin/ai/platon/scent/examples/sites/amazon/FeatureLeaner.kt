package ai.platon.scent.examples.sites.amazon

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.nodes.node.ext.textRepresentation
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import ai.platon.scent.analysis.data
import ai.platon.scent.common.message.ScentMiscMessageWriter
import ai.platon.scent.context.withContext
import ai.platon.scent.dom.HarvestOptions
import ai.platon.scent.dom.nodes.HyperPath
import ai.platon.scent.dom.select.selectFirstOrNull
import ai.platon.scent.examples.common.AmazonStreamingSqlExtractor
import ai.platon.scent.examples.common.Crawler
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.system.measureTimeMillis

class FeatureLeaner(
        private val maxRecords: Int = 50
): Crawler() {
    private val scanUrlPrefix = "https://www.amazon.com/"
    private val scanFields = listOf(GWebPage.Field.PROTOCOL_STATUS, GWebPage.Field.CONTENT)
    private val scanMinimumContentSize = 10_000

    private val args = "-ic -i 10d -ii 70d -tl 40 -ol \"h2 a[href~=/dp/]\""
    private val options = HarvestOptions.parse(args).apply { diagnose = true; trustSamples = true }
    private val portalUrls = LinkExtractors.fromResource("/amazon-categories.txt")
    private val hyperPaths = ConcurrentLinkedQueue<HyperPath>()
    private val webDb = session.pulsarContext.getBean<WebDb>()
    private val driverManager = session.pulsarContext.getBean<WebDriverPoolManager>()
    private val messageWriter get() = session.pulsarContext.getBean<ScentMiscMessageWriter>()
    private var round = 0

    fun run() {
        val elapsed = measureTimeMillis {
            runBlocking { AmazonStreamingSqlExtractor(scanSequence()).run() }
        }

        println("Elapsed: " + Duration.ofMillis(elapsed))
    }

    fun leanAndScan() {
        loadPaths()

//        convertHyperPaths2Sql()
//        if (alwaysTrue()) return

        val urls = portalUrls.take(1)
        val portalPages = session.loadAll(urls, options)

        // Learn all selectors
        portalPages.parallelStream().forEach { learn(it.url) }

        // Release resources
        driverManager.close()
        // scan()

//        officialAttributes.joinToString { it }.also { println(it) }
//        idSelectors.joinToString { it }.also { println(it) }
    }

    fun loadPaths() {
        // NodePaths.load(hyperPaths)
        val lines = ResourceLoader.readAllLines("/trusted-hyper-paths.txt")
        lines.mapNotNull { HyperPath.parse(it) }.forEach { hyperPaths.add(it) }
    }

    suspend fun scanAndExtract() {
        val header = hyperPaths.iterator().asSequence().toList()
                .joinToString("\t") { it.label ?: it.firstName ?: "" }
        messageWriter.reportExtractCsvResult(header)

        val elapsed = measureTimeMillis {
            supervisorScope {
                scanSequence().map { session.parse(it) }.forEach { launch { extractByHyperPath(it) } }
            }
        }

        log.info("Elapsed {}", Duration.ofMillis(elapsed).readable())
    }

    fun learn(portalUrl: String) {
        val group = runBlocking { session.harvest(portalUrl, options) }
        group.tables.forEach { it.columns.forEach { hyperPaths.add(it.data.hyperPath) } }
        session.buildAll(group, options)
    }

    private fun scanSequence(): Sequence<WebPage> {
        return webDb.scan(scanUrlPrefix, scanFields).asSequence()
                .filter { it.key.contains("/dp/") }
                .filter { it.protocolStatus.isSuccess && it.content?.array()?.size ?: 0 > scanMinimumContentSize }
                .drop(2000)
                .take(maxRecords)
    }

    private fun loadAndExtractOutPages(portalUrl: String, paths: Collection<HyperPath>) {
        println("\n\nRound ${++round}.=====================")
        val pages = session.loadOutPages(portalUrl, options)
        println("Loaded ${pages.size} pages and total ${paths.size} fields are expected | $portalUrl")

        val sb = StringBuilder()
        pages.forEachIndexed { k, page ->
            extractByHyperPath(page, sb)
        }
    }

    private fun extractByHyperPath(page: WebPage, sb: StringBuilder = StringBuilder()) {
        val doc = session.parse(page)
        extractByHyperPath(doc, sb)
    }

    private fun extractByHyperPath(doc: FeaturedDocument, sb: StringBuilder = StringBuilder()) {
        // log.debug("Extract document | {} | {}", doc.title, doc.location)

        sb.setLength(0)

        var fields = 0
        val missFields = mutableListOf<String>()
        hyperPaths.iterator().asSequence().forEachIndexed { j, path ->
            val node = doc.selectFirstOrNull(path)
            val text = (node?.textRepresentation ?: "").replace("\t", "")
            if (text.isNotBlank()) {
                ++fields
            } else {
                missFields.add(path.display)
            }
            sb.append(text).append("\t")
        }

        if (fields > 10) {
            sb.append(doc.location)
            val tid = Thread.currentThread().id
//            val j = numRecords.incrementAndGet()
            // println("$j.\t[w$tid] $fields <" + missFields.joinToString { it } + "> | " + doc.location)
            messageWriter.reportExtractCsvResult(sb.toString())
        }
    }

    private fun convertHyperPaths2Sql() {
        val sb = StringBuilder()
        sb.appendln("select")
        hyperPaths.iterator().asSequence().toList().joinTo(sb, ",\n") { "    dom_first_text(dom, '$it') as `${it.label}`" }
        sb.appendln()
        sb.appendln("from dom_select({{url}})")
        println(sb.toString())
    }
}

fun main(args: Array<String>) {
    var maxRecords = 1000
    var i = 0
    while (i++ < args.size) {
        if (args[i] == "-maxRecords") {
            maxRecords = args[i++].toInt()
        }
    }

    withContext {
        FeatureLeaner(maxRecords).use { it.run() }
    }
}
