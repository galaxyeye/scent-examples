package ai.platon.scent.examples.misc

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.CapabilityTypes.PULSAR_DOMAIN
import ai.platon.pulsar.common.config.CapabilityTypes.SCENT_CLASSIFIER_BLOCK_LABELS
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.dom.Documents
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.nodes.node.ext.isInitialized
import ai.platon.scent.context.ScentContexts
import ai.platon.scent.context.withContext
import ai.platon.scent.dom.HarvestOptions
import ai.platon.scent.segment.PatternLocator
import org.slf4j.LoggerFactory

object ScentExamples {

    private val log = LoggerFactory.getLogger(ScentExamples::class.java)

    private var i = ScentContexts.createSession()

    private val baseDir = "webpages"
    private val localPage = "$baseDir/mia.com/detail-b0fdd747d47d2315e223acf3b9e96a70.html"
    private var doc: FeaturedDocument
    private val labels = i.sessionConfig.getStrings(SCENT_CLASSIFIER_BLOCK_LABELS)

    val seeds = mapOf(
            0 to "http://category.dangdang.com/cid4002590.html",
            1 to "https://list.mogujie.com/book/magic/51894",
            2 to "https://list.jd.com/list.html?cat=6728,6742,13246",
            3 to "https://list.gome.com.cn/cat10000055-00-0-48-1-0-0-0-1-2h8q-0-0-10-0-0-0-0-0.html?intcmp=bx-1000078331-1",
            4 to "https://search.yhd.com/c0-0/k%25E7%2594%25B5%25E8%25A7%2586/",
            5 to "https://www.amazon.cn/b/ref=sa_menu_Accessories_l3_b888650051?ie=UTF8&node=888650051",
            6 to "https://hangzhou.anjuke.com/sale/?from=navigation", // anti crawl
            7 to "https://www.haozu.com/sh/fxxiezilou/", // anti crawl
            8 to "https://www.amazon.com/Best-Sellers/zgbs"
    )

    init {
        System.setProperty(CapabilityTypes.BROWSER_IMAGES_ENABLED, "true")
        doc = Documents.parse(ResourceLoader.getResourceAsStream(localPage)!!, "UTF-8", localPage)
    }

    fun validateConfig() {
        log.debug("config: " + i.sessionConfig.unbox().size())
        log.debug("config fallback: " + i.sessionConfig.fallbackConfig?.unbox()?.size())
        log.debug("conf wrapped: " + MutableConfig(i.pulsarContext.unmodifiedConfig).unbox().size())

        log.debug("domain: " + i.sessionConfig.get(PULSAR_DOMAIN))
        log.debug("labels: " + i.sessionConfig.get(SCENT_CLASSIFIER_BLOCK_LABELS))
        log.debug("resource dir: " + i.sessionConfig.getResource(baseDir).path)
        log.debug("document path: " + i.sessionConfig.getResource(localPage).path)
    }

    fun load() {
        val url = seeds[8]?:return
        val document = i.load(url, LoadOptions.parse("-i 1s")).let { i.parse(it) }
    }

    fun checkDocument() {
        val document = i.load(seeds[0]!!).let { i.parse(it) }

        println(document.document.isInitialized)
        document.document.isInitialized.set(true)
        println(document.document.isInitialized)

        val document2 = i.load(seeds[1]!!).let { i.parse(it) }
        println(document2.document.isInitialized)

        val document3 = i.load(seeds[2]!!).let { i.parse(it) }

        require(document2.document != document3.document)

        println(document3.document.isInitialized)
    }

    fun segment() {
        i.partition(doc)
    }

    fun segmentBy() {
        i.partitionBy(doc, PatternLocator(i.sessionConfig))
    }

    fun extract() {
        i.extract(doc, labels)
    }

    fun analysis() {
        val urlBase = "https://www.mia.com/item-"
        val options = HarvestOptions.create()

        i.scanHarvest(urlBase, 40, options).let { i.build(it) }

        // analyzer.draw()
    }

    fun truncate() {
        i.pulsarContext.webDb.truncate()
        i.pulsarContext.flush()
    }

    fun run() {
        load()
        // analysis()
        // extract()
        // extractByToPiped()
        // pipeline()
        // pipePipeline()
        // pipePipePipeline()
        // truncate()
    }
}

fun main() { withContext { ScentExamples.run() } }
