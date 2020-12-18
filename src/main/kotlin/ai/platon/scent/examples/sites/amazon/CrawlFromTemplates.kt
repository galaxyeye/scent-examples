package ai.platon.scent.examples.sites.amazon

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.url.Hyperlink
import ai.platon.pulsar.common.Systems
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.crawl.StreamingCrawler
import ai.platon.pulsar.persist.model.WebPageFormatter
import ai.platon.scent.protocol.browser.emulator.context.MultiPrivacyContextManager
import ai.platon.scent.examples.common.Crawler
import kotlinx.coroutines.runBlocking

class CrawlFromTemplates: Crawler() {
    private var round = 0
    private val privacyManager = session.context.getBean(MultiPrivacyContextManager::class)

    val seeds = listOf(
            "https://www.amazon.com/gp/browse.html?node=16713337011&ref_=nav_em_0_2_8_5_sbdshd_cameras"
    ).filter { it.isNotBlank() }

    val portalUrlTemplates = arrayOf(
            "https://www.amazon.com/s?i=specialty-aps&srs=13575748011&page={{page}}&qid=1575032004&ref=lp_13575748011_pg_{{page}}",
            "https://www.amazon.com/s?i=fashion-girls-intl-ship&bbn=16225020011&rh=n%3A7141123011%2Cn%3A16225020011%2Cn%3A3880961&page={{page}}&qid=1578841587&ref=sr_pg_{{page}}",
            "https://www.amazon.com/s?i=fashion-boys-intl-ship&bbn=16225021011&rh=n%3A7141123011%2Cn%3A16225021011%2Cn%3A6358551011&page={{page}}&qid=1578842855&ref=sr_pg_{{page}}",
            "https://www.amazon.com/s?i=pets-intl-ship&bbn=16225013011&rh=n%3A16225013011%2Cn%3A2975312011&page={{page}}&qid=1578842918&ref=sr_pg_{{page}}"
    )

    fun run() {
        val portalUrls = portalUrlTemplates.flatMap { template ->
            IntRange(1, 10).map { template.replace("{{page}}", it.toString()) }
        }.shuffled()

        portalUrls.forEach {
            // println(it)
        }

        portalUrls.forEach {
            crawlOutPages(it)
        }
    }

    fun loadOutPages() {
        val url = seeds[0]
        loadOutPages(url, "-i 1s -ii 1s -ol a[href~=/dp/]")
    }

    private fun crawlOutPages(portalUrl: String) {
        if (!session.isActive) {
            return
        }

        log.info("\n\n\n--------------------------\nRound ${++round} $portalUrl")

        val args = "-i 1d -ii 1s -ol \"a[href~=/dp/]\""
        val options = LoadOptions.parse(args)
        val portalPage = session.load(portalUrl, options)

        val portalDocument = session.parse(portalPage)
        val links = portalDocument.select("a[href~=/dp/]") {
            it.attr("abs:href").substringBeforeLast("#")
        }.mapTo(mutableSetOf()) { Hyperlink(it) }

        val sb = StringBuilder("\n")
        links.forEachIndexed { j, l ->
            sb.appendLine(String.format("%-10s%s", "$j.", l))
        }
        log.info(sb.toString())
        sb.setLength(0)

        if (links.isEmpty()) {
            log.info("Warning: No links")
            val link = AppPaths.uniqueSymbolicLinkForUri(portalPage.url)
            log.info("file://$link")
            log.info("Page details: \n" + WebPageFormatter(portalPage))

            return
        }

        val itemOptions = options.createItemOptions()
        runBlocking {
            StreamingCrawler(links.shuffled().asSequence(), itemOptions).run()
        }
    }
}

fun main() {
    System.setProperty(CapabilityTypes.BROWSER_DRIVER_HEADLESS, "false")
    System.setProperty(CapabilityTypes.FETCH_CONCURRENCY, "10")
    Systems.setProperty(CapabilityTypes.BROWSER_JS_INVADING_ENABLED, "false")
    Systems.setPropertyIfAbsent(CapabilityTypes.BROWSER_CHROME_PATH, "/usr/bin/google-chrome-stable")

    CrawlFromTemplates().use { it.loadOutPages() }
}
