package ai.platon.scent.examples.common

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.scent.ScentContext
import ai.platon.scent.ScentSession
import ai.platon.scent.context.ScentContexts
import ai.platon.scent.dom.FullFeatureCalculator
import ai.platon.scent.dom.HarvestOptions
import ai.platon.scent.dom.nodes.annotateNodes
import com.google.common.collect.Lists
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

open class WebHarvester(val context: ScentContext): Crawler() {

    var i = context.createSession()

    private val counter = AtomicInteger(0)
    private val taskTimes = ConcurrentHashMap<String, Duration>()

    init {
        System.setProperty(CapabilityTypes.BROWSER_IMAGES_ENABLED, "true")
        // BrowserControl.pageLoadStrategy = "normal"
    }

    val pendingSeeds = listOf(
            // jobs
            "https://www.yingjiesheng.com/xianjob/", // low fvr, article like
            // love
            "http://www.taonanw.com/", // timeout
            // movie
            "http://v.hao123.baidu.com/video/x", // all links are the same, may be caused by anti-spider

            // pets
            "http://www.goumin.com/", // article, no table
            "http://lingyang.goumin.com/adopt/index", // gallery [pet-list > div] is not detected
            "http://www.boqii.com/", // low quantity anchor group
            // menu
            "http://www.chinacaipu.com/menu/rouleishipu/?hao123-life", // article, failed to find crumbs

            // other languages
            "https://www.tripadvisor.es/Restaurants-g187514-c36-Madrid.html", // just timeout

            "http://dc.pcpop.com/", // article
            "http://www.shouyoubus.com/game/", // No regional text node
            "http://www.shouyoubus.com/hangye/", // news, No regional text node
            "http://www.shouyoubus.com/gonglue/", // news, No regional text node
            "https://www.taoshouyou.com/game/wangzherongyao-2256-0-21",
            "https://www.taoshouyou.com/zuhao/G603", // banned
            // property
            "https://www.himawari-japan.com/index/lists/catid/3.html", // data in table
            "https://www.homes.co.jp/mansion/shinchiku/fukuoka/fukuoka_hakata-city/list/", // banned, determined as a bot
            "https://hotels.ctrip.com/international/", // seems too many tables: 24
            "https://xiangmu.trjcn.com/", // navigate with no images have low score
            "http://jrh.financeun.com/", // articles
            "https://issp.suning.com/", // few data
            ""
    ).filter { it.isNotBlank() }

    val testingSeeds = listOf(
            "https://p4psearch.1688.com/p4p114/p4psearch/offer.htm?spm=a2609.11209760.it2i6j8a.680.3c312de1W6LoPE&keywords=不锈钢螺丝" // very long time
    ).flatMap { seed -> Array(20) { seed }.toList() }

    val testedSeeds = listOf(
            /////////////////////////////////////////////////////////
            // The sites below are well tested

            "http://mall.goumin.com/mall/list/219",
            "https://www.hua.com/gifts/chocolates/",
            "http://category.dangdang.com/cid4002590.html",
            "https://list.mogujie.com/book/magic/51894",
            "https://list.jd.com/list.html?cat=6728,6742,13246",
            "https://list.gome.com.cn/cat10000055-00-0-48-1-0-0-0-1-2h8q-0-0-10-0-0-0-0-0.html?intcmp=bx-1000078331-1",
            "https://search.yhd.com/c0-0/k电视/",
            "https://www.amazon.cn/b/ref=sa_menu_Accessories_l3_b888650051?ie=UTF8&node=888650051",
            "https://category.vip.com/search-1-0-1.html?q=3|49738||&rp=26600|48483&ff=|0|2|1&adidx=2&f=ad&adp=130610&adid=632686",

            "https://www.lagou.com/zhaopin/chanpinzongjian/?labelWords=label",
            "https://mall.ccmn.cn/mallstocks/",
            "https://sh.julive.com/project/s/i1",
            "https://www.meiju.net/Mlist//Mju13.html",
            "http://mall.molbase.cn/p/612",
            "https://www.haier.com/xjd/all.shtml",
            "https://bj.nuomi.com/540",
            "https://www.haozu.com/sh/fxxiezilou/",
            "http://www.dianping.com/",
            "http://www.dianping.com/wuhan/ch55/g163",

            "https://p4psearch.1688.com/p4p114/p4psearch/offer.htm?spm=a2609.11209760.it2i6j8a.680.3c312de1W6LoPE&keywords=不锈钢螺丝", // very long time
            // company
            "https://www.cyzone.cn/company/list-0-0-1/",
            "https://www.cyzone.cn/capital/list-0-1-4/",
            // flower
            "https://www.hua.com/flower/",
            "https://www.hua.com/gifts/chocolates/",
            "https://www.hua.com/yongshenghua/yongshenghua_large.html",
            "http://www.cityflower.net/goodslist/5/",
            "http://www.cityflower.net/goodslist/2/",
            "http://www.cityflower.net/goodslist/1/0/0-0-4-0.html",
            "http://www.cityflower.net/",
            "http://www.zgxhcs.com/",
            // laobao
            "https://www.zhaolaobao.com/productlist.html?classifyId=77",
            "https://www.zhaolaobao.com/productlist.html?classifyId=82",
            "https://www.zhaolaobao.com/",
            // snacks
            "http://www.lingshi.com/",
            "http://www.lingshi.com/list/f64_o1.htm",
            "http://www.lingshi.com/list/f39_o1.htm",
            // jobs
            "https://www.lagou.com/gongsi/",
            "https://www.lagou.com/zhaopin/chanpinzongjian/",
            // love
            "http://yuehui.163.com/",
            // movie
            "http://v.hao123.baidu.com/v/search?channel=movie&category=科幻",
            "https://youku.com/",
            "https://movie.youku.com/?spm=a2ha1.12675304.m_6913_c_14318.d_3&scm=20140719.manual.6913.url_in_blank_http%3A%2F%2Fmovie.youku.com",
            "https://auto.youku.com/?spm=a2ha1.12675304.m_6913_c_14318.d_16&scm=20140719.manual.6913.url_in_blank_http%3A%2F%2Fauto.youku.com",
            "http://list.youku.com/category/video?spm=a2h1n.8251847.0.0",

            // pets
            "http://shop.boqii.com/cat/list-576-0-0-0.html",
            "http://shop.boqii.com/small/",
            "http://shop.boqii.com/brand/",
            "http://longyu.cc/shop.php?mod=exchange",
            "http://longdian.com/",
            "http://longdian.com/et_special.php?id=75",
            // menu
            "http://life.hao123.com/menu",
            "http://www.chinacaipu.com/shicai/sjy/junzao/xianggu/",

            "http://sj.zol.com.cn/",
            "http://sj.zol.com.cn/series/list80_1_1.html",
            "http://sj.zol.com.cn/pad/android/",
            "https://www.taoshouyou.com/game/wangzherongyao-2256-0-21",
            // property
            "https://www.ausproperty.cn/building/melbourne/",
            "http://jp.loupan.com/xinfang/",
            "https://malaysia.fang.com/house/",
            ""
    ).filter { it.isNotBlank() }

    val seedGroup1 = listOf(
            "https://list.suning.com/0-20006-0-0-0-0-0-0-0-0-11635.html -expires 1s -ol \".product-box a[href~=product]\"",
            "http://dzhcg.sinopr.org/channel/301",
            "https://list.gome.com.cn/cat10000070-00-0-48-1-0-0-0-1-0-0-1-0-0-0-0-0-0.html?intcmp=phone-163",
            "http://category.dangdang.com/cid4002590.html -tp 140 -i 1h -scrollCount 20 -ii 1d -ol a[href~=product]",
            "https://www.proya.com/product_query-xId-583.html -i 1d -tl 40 -ol \"a[href~=product_detail]\" -ii 7d -c \".productInfo .conn\"",
            "https://www.esteelauder.com.cn/products/14731/product-catalog -i 1s -ii 7d -ol a[href~=product]",
            "https://www.darphin.com/collections/essential-oil-elixir",
            "https://search.jd.com/Search?keyword=basketball&enc=utf-8&wq=basketball&pvid=27d8a05385cd49298b5caff778e14b97"
    ).filter { it.isNotBlank() }

    val failedSeeds = listOf(
            "https://stackoverflow.com/questions/220547/printable-char-in-java", // timeout
            "https://car.autohome.com.cn/price/brand-33.html#pvareaid=2042362", // few fields, might be many tables
            "http://cpu.pcpop.com/", // article, no table
            "https://www.huxiu.com/channel/104.html", // article, no table
            "http://you.163.com/item/list?categoryId=1043000&_stat_area=nav_5", // timeout
            "https://www.ebay.com/b/Apple-Tablets-eReaders/171485/bn_319675", // anti-spider, login required
            "https://music.163.com/ -expires 1s", // multiple-frames is not handled
            "https://hangzhou.anjuke.com/sale/?from=navigation", // anti-spider
            "http://www.pizzahut.com.cn/",
            "http://1.hsh172.cn/pro/7",
            "http://wuhan.baixing.com/",
            "http://wuhan.baixing.com/fangpd/?src=topbar",
            "http://www.finndy.com/robot.php " // phone view
    )

    val defaultArgs = "" +
            " -expires 1d" +
            " -itemExpires 1d" +
//                " -scrollCount 6" +
//                " -itemScrollCount 4" +
            " -nScreens 5" +
//                " -polysemous" +
            " -diagnose" +
            " -nVerbose 1" +
            " -preferParallel false" +
//                " -pageLoadTimeout 60s" +
            " -showTip" +
            " -showImage" +
//                " -cellType PLAIN_TEXT" +
            ""

    fun arrangeDocument(portalUrl: String) {
        val taskName = AppPaths.fromUri(portalUrl)

        val normUrl = i.normalize("$portalUrl $defaultArgs")
        val options = normUrl.hOptions
        val portalPage = i.load(normUrl)
        val portalDocument = i.parse(portalPage)
        val anchorGroups = i.arrangeLinks(normUrl, portalDocument)
        log.info("------------------------------")
        anchorGroups.take(1).forEach {
            it.urlStrings.shuffled().take(10).forEachIndexed { i, url -> println("${1 + i}.\t$url") }
            it.urlStrings.take(options.topLinks)
                    .map { i.load(it, options) }
                    .map { i.parse(it, options) }
                    .let { i.arrangeDocuments(normUrl, portalPage, it) }
        }

        portalDocument.also { it.annotateNodes(options) }.also { i.export(it, type = "portal") }
    }

    fun harvest(url: String) {
        harvest(url, defaultArgs)
    }

    fun harvest(url: String, args: String) {
        harvest(url, HarvestOptions.parse(args))
    }

    fun harvest(url: String, options: HarvestOptions) {
        harvest(i, url, options)
    }

    fun harvest(session: ScentSession, url: String, options: HarvestOptions) {
        val start = Instant.now()

        val group = runBlocking { session.harvest(url, options) }
        session.buildAll(group, options)

//        val json = session.buildJson(group, options)
//        println(json)

        taskTimes[url] = Duration.between(start, Instant.now())
    }

    fun harvestAll() {
        counter.set(0)

        val concurrencyLevel = 20
        val portalUrls = listOf(testedSeeds).flatten().filter { it.isNotBlank() }.shuffled()
        portalUrls.let { Lists.partition(it, concurrencyLevel) }.forEachIndexed { i, seeds ->
            val start = Instant.now()
            seeds.stream()
                    .parallel()
                    .forEach { seed ->
                        ScentContexts.createSession().use { harvest(it, seed, HarvestOptions.create()) }
            }
            val elapsed = Duration.between(start, Instant.now())
            log.info("Takes {} at concurrency level {}", elapsed, concurrencyLevel)
        }

        val numDocuments = FeaturedDocument.globalNumDocuments
        val numNodes = FullFeatureCalculator.globalNumNodes
        log.info("Allocated total $numDocuments documents and $numNodes DOM nodes")
        val sortedTaskTimes = taskTimes.entries.toList().sortedByDescending { it.value }
        Lists.partition(sortedTaskTimes, 20)
                .joinToString("\n") { it.joinToString { it.value.toString().removePrefix("PT") } }
                .also { log.info("Times spent for each harvest tasks: \n$it") }
        sortedTaskTimes.take(10).mapIndexed { i, e ->
            "${1 + i}.\t${e.value}\t${e.key}"
        }.joinToString("\n") { it }.also { log.info("Slowest 10 tasks: \n$it") }
    }

    fun concurrencyLevelTest() {
        concurrencyLevelTest(5)
        concurrencyLevelTest(10)
        concurrencyLevelTest(15)
    }

    fun concurrencyLevelTest(concurrencyLevel: Int) {
        val start = Instant.now()
        val portalUrls = listOf(seedGroup1, seedGroup1, seedGroup1, seedGroup1, seedGroup1).flatten()
                .filter { it.isNotBlank() }
                .shuffled()
        portalUrls.let { Lists.partition(it, concurrencyLevel) }.forEachIndexed { i, seeds ->
            parallelHarvest(seeds, concurrencyLevel)
        }
        val elapsed = Duration.between(start, Instant.now())
        log.info(">>> Takes total {} to analyze {} tasks at concurrency level {}", elapsed, portalUrls.size, concurrencyLevel)
    }

    fun parallelHarvest(seeds: List<String>, concurrencyLevel: Int) {
        val start = Instant.now()
        seeds.stream().parallel().forEach { seed -> ScentContexts.createSession().use { harvest(it, seed, HarvestOptions.create()) } }
        val elapsed = Duration.between(start, Instant.now())
        log.info("Takes {} at concurrency level {}", elapsed, concurrencyLevel)
    }

    fun harvestPerformanceTest() {
        counter.set(0)
        repeat(20) {
            harvestAll()
        }
    }
}
