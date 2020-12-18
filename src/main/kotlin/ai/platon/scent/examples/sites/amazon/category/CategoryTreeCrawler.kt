package ai.platon.scent.examples.sites.amazon.category

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.url.PlainUrl
import ai.platon.pulsar.common.url.UrlAware
import ai.platon.pulsar.common.url.Urls
import ai.platon.pulsar.crawl.Crawler
import ai.platon.pulsar.crawl.StreamingCrawler
import ai.platon.pulsar.dom.nodes.Anchor
import ai.platon.pulsar.dom.nodes.node.ext.left
import ai.platon.pulsar.dom.nodes.node.ext.ownerDocument
import ai.platon.pulsar.dom.select.getAnchors
import ai.platon.pulsar.persist.WebPage
import ai.platon.scent.ql.h2.context.ScentSQLContext
import ai.platon.scent.ql.h2.context.withSQLContext
import kotlinx.coroutines.runBlocking
import org.jsoup.nodes.Element
import org.jsoup.select.NodeTraversor
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.InetAddress
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermissions
import java.time.Duration
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess

private class CategoryTask(
        val url: String,
        val categoryName: String,
        val anchor: Anchor
)

class CategoryTreeCrawler(
        val urlIdent: String,
        val topCategoryUrl: String,
        val depth0CategoriesRootSelector: String,
        val depth1CategoriesSelector: String,
        val loadArguments: String = "-i 1000d",
        val requiredTopCategories: List<String> = listOf(),
        val context: ScentSQLContext
): Crawler(context) {
    private val log = LoggerFactory.getLogger(CategoryTreeCrawler::class.java)

    private val loadOptions = LoadOptions.parse(loadArguments)
            .apply { retryFailed = true; nJitRetry = 3 }
    private lateinit var rootCategoryNode: GlobalCategoryNode
    private val allCategoryUrls = ConcurrentSkipListSet<String>()
    private val processedUrls = ConcurrentSkipListSet<String>()
    private val knownPaths = ConcurrentSkipListMap<String, String>()
    private val outputDirectory = AppPaths.getTmp("category").resolve(DateTimes.formatNow("HHmm"))
    private val categoryTreePath = outputDirectory.resolve("amazon-$urlIdent-categories.txt")
    private val redundantUrlParameters = arrayOf("qid", "ref", "_ref", "ref_")

    // private val redundantUrlParts = mapOf("ref=.+\\?".toRegex() to "?")
    private val redundantUrlParts = mapOf<String, String>()
    private val crawlRound = AtomicInteger()

    init {
        val headLine = String.format("%s | %s | %s | %s | %s | %s\n",
                "id", "parent id", "depth", "numSubcategories", "category path", "url")

        Files.createDirectories(outputDirectory)
        Files.writeString(categoryTreePath, headLine, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
    }

    fun crawl() {
        session.load(topCategoryUrl, "-i 1s -retry -njr 3")
        val page = session.load(topCategoryUrl, "-i 1s -retry -njr 3")
        rootCategoryNode = GlobalCategoryNode("Root", page.url)
        val document = session.parse(page)
        document.absoluteLinks()

        val path = session.export(document)
        log.info("Top category page is exported to file://$path")

        val depth0CategoriesRoot = document.selectFirstOrNull(depth0CategoriesRootSelector)
                ?: throw NoSuchElementException("No subcategory in root category")
        extractSubcategoryAnchorsTo(depth0CategoriesRoot, depth1CategoriesSelector, rootCategoryNode)

        val topSubcategoryAnchors = if (requiredTopCategories.isNotEmpty()) {
            rootCategoryNode.subcategoryAnchors.filter { it.text in requiredTopCategories }
        } else {
            rootCategoryNode.subcategoryAnchors
        }
        rootCategoryNode.subcategoryAnchors.clear()
        rootCategoryNode.subcategoryAnchors.addAll(topSubcategoryAnchors)

        log.info("There are {} subcategories to analyze under root [{}]",
                rootCategoryNode.subcategoryAnchors.size, rootCategoryNode.path)
        rootCategoryNode.subcategoryAnchors
                .mapIndexed { i, anchor -> "${1 + i}.\t${anchor.text}" }
                .joinToString("\n")
                .also { log.info("\n$it") }
        crawlCategoriesRecursively(1, rootCategoryNode)

        val text = allCategoryUrls.joinToString("\n")
        val allCategoryUrlsFile = outputDirectory.resolve("all-category-urls.txt")
        Files.writeString(allCategoryUrlsFile, text, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
    }

    fun loadKnownCategories(knownCategoryResource: String, urlIdent: String) {
        val urls = ResourceLoader.readAllLines(knownCategoryResource)
                .asSequence()
                .filter { line -> requiredTopCategories.isEmpty() || requiredTopCategories.any { it in line } }
                .map { it.substringAfterLast(" | ").trim() }
                .map { normalizeUrl(it) }
                .map { PlainUrl(it) }
                .toMutableList()
        log.info("Loading known categories from resource with {} links: ", urls.size)
        runBlocking {
            StreamingCrawler(urls.asSequence(), loadOptions, session).use { it.run() }
        }

        log.info("Collecting categories from known category pages")
        val urls2 = ConcurrentSkipListSet<PlainUrl>()
        urls.parallelStream().forEach {
            val document = session.loadDocument(it.url, loadOptions)
            document.select("ul a[href~=/$urlIdent/]")
                    .map { it.attr("href") }
                    .mapTo(urls2) { PlainUrl(normalizeUrl(it)) }
        }
        urls.clear()

        log.info("Loading categories from known category pages with {} links: ", urls2.size)
        runBlocking {
            StreamingCrawler(urls2.asSequence(), loadOptions, session).also {
                it.onLoadComplete = { url, page -> onLoadComplete(url, page) }
            }.use { it.run() }
        }
    }

    fun onLoadComplete(url: UrlAware, page: WebPage) {

    }

    fun report() {
        try {
            val target = "amazon-$urlIdent-categories.txt." + DateTimes.formatNow("HHmmss")
            Files.move(categoryTreePath, categoryTreePath.resolveSibling(target))
        } catch (e: IOException) {
            log.warn(e.message)
        }

        format(rootCategoryNode)
        generateScripts()
    }

    fun testUrlNormalizer() {
        val urls = arrayOf(
                "https://www.amazon.com/Best-Sellers-Industrial-Scientific/zgbs/industrial/ref=zg_bs_unv_indust_1_8297620011_3",
                "https://www.amazon.com/gp/most-wished-for/audible/18573370011/ref=zg_mw_nav_1_audible",
                "https://www.amazon.com/gp/movers-and-shakers/automotive/ref=zg_bsms_nav_0",
                "https://www.amazon.com/gp/movers-and-shakers/ref=zg_bsms_unv_auto_0_auto_1"
        )
        val normalizedUrls = arrayOf(
                "https://www.amazon.com/Best-Sellers-Industrial-Scientific/zgbs/industrial",
                "https://www.amazon.com/gp/most-wished-for/audible/18573370011",
                "https://www.amazon.com/gp/movers-and-shakers/automotive",
                "https://www.amazon.com/gp/movers-and-shakers"
        )

        var failures = 0
        urls.forEachIndexed { i, url ->
            val normalizedUrl = normalizeUrl(url)
            if (normalizedUrl != normalizedUrls[i]) {
                failures++
                log.warn("Failed to normalize url \n$normalizedUrl \n${normalizedUrls[i]}")
            }
        }

        if (failures == 0) {
            log.info("Url normalizer test is passed")
        } else {
            log.error("Url normalizer test is failed")
            exitProcess(0)
        }
    }

    fun testUrlNormalizerCategories(knownCategoryResource: String, urlIdent: String) {
        val urls = ResourceLoader.readAllLines(knownCategoryResource)
                .asSequence()
                .filter { line -> requiredTopCategories.isEmpty() || requiredTopCategories.any { it in line } }
                .map { it.substringAfterLast(" | ").trim() }
                .map { PlainUrl(normalizeUrl(it)) }
                .take(100)
                .toMutableList()
        log.info("Loading known categories from resource with {} links: ", urls.size)
        runBlocking {
            StreamingCrawler(urls.asSequence(), loadOptions, session).use { it.run() }
        }

        log.info("Collecting categories from known category pages with ident <$urlIdent>")
        val urls2 = ConcurrentSkipListSet<String>()
        urls.parallelStream().forEach {
            val document = session.loadDocument(it.url)
            document.select("ul a[href~=/$urlIdent/]")
                    .map { it.attr("href") }
                    .mapTo(urls2) { normalizeUrl(it) }
        }
        urls.clear()

        // urls2.forEach { println(it) }
        val allCategoryUrlsFile = outputDirectory.resolve("test-category-urls.txt")
        val text = urls2.joinToString("\n")
        val path = Files.writeString(allCategoryUrlsFile, text, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        log.info("Total {} links are written into {}", urls2.size, path)
    }

    private fun crawlCategoriesRecursively(depth: Int, category: GlobalCategoryNode) {
        crawlRound.incrementAndGet()
        log.info("Round $crawlRound.\tdepth [$depth] [${category.path}] {} subcategories | {}", category.subcategoryAnchors.size, category.url)

        val loadOptions2 = loadOptions.clone()
        if (depth <= 3) {
            // loadOptions2.expires = Duration.ZERO
        }

        loadAndCreateSubcategoriesFromSubcategoryAnchors(category, loadOptions2)
        if (category.children.isEmpty()) {
            log.info("No subcategory under [{}]", category.path)
            return
        }

        val urls = category.children.flatMap { it.subcategoryAnchors.map { PlainUrl(it.url) } }
        if (urls.isNotEmpty()) {
            log.info("Loading {} links in depth $depth using streaming crawler: ", urls.size)
            runBlocking {
                StreamingCrawler(urls.asSequence(), loadOptions2, session).use { it.run() }
            }
        }

        category.children.parallelStream().forEach { subcategory ->
            if (subcategory.url == category.url) {
                log.warn("Subcategory has the same url with the parent, ignore | {} <- {}", subcategory.url, category.url)
                return@forEach
            }

            if (subcategory.path == category.path) {
                log.warn("Subcategory has the same path with the parent, ignore | {} <- {}",
                        subcategory.path, category.path)
                return@forEach
            }

            val categoryDepth = subcategory.depth
            log.info("Crawling subcategory [{}] category depth:{} traversal depth {} under [{}]({})",
                    subcategory.path, categoryDepth, depth, category.path, category.depth)
            crawlCategoriesRecursively(1 + depth, subcategory)
        }

        if (crawlRound.get() % 1000 == 0) {
            format(rootCategoryNode)
        }
    }

    /**
     *
     * */
    private fun loadAndCreateSubcategoriesFromSubcategoryAnchors(parentCategory: GlobalCategoryNode, loadOptions: LoadOptions) {
        val counter = AtomicInteger()
        parentCategory.subcategoryAnchors.parallelStream().forEach { anchor ->
            try {
                val j = counter.incrementAndGet()
                loadPageAndCreateSubcategory(j, anchor, parentCategory, loadOptions)
            } catch (e: IllegalApplicationContextStateException) {
                log.warn("Illegal app state, exit")
                return@forEach
            } catch (e: Throwable) {
                log.warn("Unexpected throwable", e)
            }
        }
    }

    @Throws(IllegalApplicationContextStateException::class)
    private fun loadPageAndCreateSubcategory(j: Int, anchor: Anchor, parentCategory: GlobalCategoryNode, loadOptions: LoadOptions) {
        processedUrls.add(anchor.url)

        val url = anchor.url
        val categoryName = anchor.text.trim()
        val expectedPath = parentCategory.path + " > " + categoryName
        val knownPathUrl = knownPaths[expectedPath]
        if (knownPathUrl != null) {
            log.info("Path is already known | {} | {}", expectedPath, knownPathUrl)
            return
        }

        val task = CategoryTask(url, categoryName, anchor)

        log.info("$j.\t[{}](expected) Loading category page | {}", expectedPath, task.url)
        val categoryPage = session.load(task.url, loadOptions)
        if (categoryPage.crawlStatus.isFetched) {
            val subcategory = createSubcategoryNode(task, categoryPage, parentCategory)
            log.info("$j.\t[{}](actual) Category is created | {}", subcategory.path, url)
        } else {
            log.warn("$j.\tFailed to load category [{}](expected) | {}", expectedPath, url)
        }
    }

    private fun createSubcategoryNode(task: CategoryTask, categoryPage: WebPage, parentCategory: GlobalCategoryNode): GlobalCategoryNode {
        val subcategory = GlobalCategoryNode(task.categoryName, categoryPage.url, parentCategory)
        parentCategory.children.add(subcategory)
        knownPaths[subcategory.path] = subcategory.url

        val document = session.parse(categoryPage)
        // require(document.body.area > 1000 * 1000)
        document.absoluteLinks()

        document.select("ul.a-pagination li.a-normal a").forEach {
            subcategory.paginationLinks.add(Anchor(it))
        }

        val patterns = arrayOf("/zgbs/", "/new-releases/", "/movers-and-shakers/", "/most-wished-for/")
        val navRootSelector =  if (patterns.any { it in categoryPage.url }) {
            "#zg_browseRoot"
        } else return subcategory

        document.body.select(navRootSelector).forEach { navRoot ->
            when (navRootSelector) {
                "#zg_browseRoot" -> collectSubcategoryAnchors(subcategory, navRootSelector, navRoot)
            }
        }

        return subcategory
    }

    // https://www.amazon.com/Best-Sellers-Beauty/zgbs/beauty
    private fun collectSubcategoryAnchors(
            currentCategory: GlobalCategoryNode, navRootSelector: String, navRoot: Element) {
        val url = navRoot.ownerDocument.baseUri()

        require(url == currentCategory.url)
        require(navRoot.id() == "zg_browseRoot")

        // this category (selected category)
        val selectedCategory = navRoot.selectFirst("li > span.zg_selected")
        val subcategoryTreeRoot = selectedCategory.parent().nextElementSiblings()
                .firstOrNull { it.tagName() == "ul" }
        if (subcategoryTreeRoot == null) {
            log.warn("No subcategory tree root | {}", url)
            return
        }
        val name = selectedCategory.text()
        log.info("Subcategory tree | [{}] -> {}", name, subcategoryTreeRoot.text())

        val thisCategoryNode = LocalCategoryNode(name, url, selectedCategory.left, null)
        thisCategoryNode.selector = navRootSelector
        currentCategory.localCategoryNodeTree = thisCategoryNode

        log.info("Collecting subcategory anchors for category [{}] | [{}]({}) | {}",
                name, currentCategory.path, currentCategory.depth, currentCategory.url)

        extractSubcategoryAnchorsTo(subcategoryTreeRoot, "li a", currentCategory)
    }

    // https://www.amazon.com/Best-Sellers-Beauty/zgbs/beauty
    private fun buildBestSellersLocalCategoryTree(currentCategory: GlobalCategoryNode, selector: String, localCategoryRootElement: Element) {
        val url = localCategoryRootElement.ownerDocument.baseUri()

        require(localCategoryRootElement.ownerDocument.baseUri() == currentCategory.url)
        require(localCategoryRootElement.id() == "zg_browseRoot")

        // this category (selected category)
        val selectedCategory = localCategoryRootElement.selectFirst("li > span.zg_selected")
        val localCategoryTreeRootElement = selectedCategory.parent().nextElementSibling()
        if (localCategoryTreeRootElement == null || localCategoryTreeRootElement.nodeName() != "ul") {
            // The leaf category
            return
        }

        val name = selectedCategory.text()
        val thisCategoryNode = LocalCategoryNode(name, url, selectedCategory.left, null)
        thisCategoryNode.selector = selector
        currentCategory.localCategoryNodeTree = thisCategoryNode

        log.info("Building local category tree for global category [{}]|[{}]({}) | {}",
                name, currentCategory.path, currentCategory.depth, currentCategory.url)

        extractSubcategoryAnchorsTo(localCategoryTreeRootElement, "li a", currentCategory)
        if (alwaysTrue()) {
            return
        }

        NodeTraversor.traverse(LocalCategoryTreeBuilder(thisCategoryNode), localCategoryTreeRootElement)

        val size1 = currentCategory.subcategoryAnchors.size
        val size2 = currentCategory.localCategoryNodeTree!!.childNodeSize
        require(size1 == size2) {
            "Subcategory anchor size should be equal to local category node tree's direct children size ($size1 vs $size2)"
        }

        val nodeCount = thisCategoryNode.sumBy { 1 }
        var maxDepth = 0
        thisCategoryNode.forEach { maxDepth = maxDepth.coerceAtLeast(it.depth) }
        log.info("Local category tree is created for [{}]. Nodes: {} max depth: {} | {}",
                currentCategory.path, nodeCount, maxDepth, currentCategory.url)
    }

    private fun extractSubcategoryAnchorsTo(linkSection: Element, subcategoryCss: String, targetCategory: GlobalCategoryNode) {
        log.info("Extracting subcategory links in {}", linkSection.ownerDocument().baseUri())

        val anchors = linkSection.getAnchors(subcategoryCss)
                .filter { it.url.substringAfter(".com").length > 5 }
                .map { Anchor(normalizeUrl(it.url), it.text, it.path.removePrefix(linkSection.cssSelector()), it.rect) }
//                .filter { it.url !in processedUrls }
                .filter { it.text.isNotBlank() }
        addSubcategoryAnchorsTo(anchors, targetCategory)
        anchors.forEach { allCategoryUrls.add(it.url) }
    }

    private fun addSubcategoryAnchorsTo(anchors: List<Anchor>, targetCategory: GlobalCategoryNode) {
        anchors
                // .filter { it.url !in processedUrls }
                .filter { it.text.isNotBlank() }
                .mapTo(targetCategory.subcategoryAnchors) { it }
    }

    private fun normalizeUrl(url: String): String {
        var u = url

        var parts = u.split("/")
        if (parts.last().matches("(\\d{3})-(\\d+)-(\\d+)".toRegex())) {
            parts = parts.dropLast(1)
        }
        u = parts.filterNot { it.startsWith("ref=") }.joinToString("/")
        u = u.substringBefore("/ref=")

        redundantUrlParameters.forEach { q ->
            u = Urls.removeQueryParameters(u, q)
        }
        redundantUrlParts.forEach { (key, value) ->
            u = key.replace(url, value)
        }
        return u.removeSuffix("?")
    }

    private fun generateScripts() {
        val fileName = categoryTreePath.fileName
        val path = outputDirectory.resolve("gen.sh")
        val cmd = """
                cat $fileName | cut -d "|" -f 1-5 > $fileName.no.links.txt
                cat $fileName | grep "0 | Root" > $fileName.leaf.txt

            """.trimIndent()
        Files.writeString(path, cmd, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxrw-r--"))
    }

    private fun format(category: GlobalCategoryNode) {
        formatRecursively(category)
    }

    private fun formatRecursively(category: GlobalCategoryNode) {
        formatCategoryNode(category)
        category.children.forEach {
            formatRecursively(it)
        }
    }

    private fun formatCategoryNode(category: GlobalCategoryNode) {
        val line = category.toDataNode().toString()
        // val l = "-".repeat(category.depth) + category.path
        println(line)
        Files.writeString(categoryTreePath, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    }
}

class CategoryTreeCrawlerRunner(
        val urlIdent: String,
        val knownCategoryResource: String,
        val topCategoryUrl: String,
        val depth0CategoriesRootSelector: String = "ul#zg_browseRoot ul",
        val depth1CategoriesSelector: String = "li a",
        val loadArguments: String = "-i 1000d",
        val requiredTopCategories: List<String> = listOf()
) {
    fun run() {
        withSQLContext {
            val crawler = CategoryTreeCrawler(
                    urlIdent = urlIdent,
                    topCategoryUrl = topCategoryUrl,
                    depth0CategoriesRootSelector = depth0CategoriesRootSelector,
                    depth1CategoriesSelector = depth1CategoriesSelector,
                    loadArguments = loadArguments,
                    requiredTopCategories = requiredTopCategories,
                    context = it
            )

//            crawler.testUrlNormalizer()
//            crawler.testUrlNormalizerCategories(knownCategoryResource, urlIdent)

            crawler.loadKnownCategories(knownCategoryResource, urlIdent)

            crawler.crawl()
            crawler.report()
        }
    }
}

private fun generateUniqueLinks() {
    val file = "/home/vincent/workspace/scent/scent-resources/src/main/resources/sites/amazon/category/most-wished-for/most-wished-for.txt"
    val urls = Files.readAllLines(Paths.get(file))
            .filter { it.contains("0 | Root") }
            .mapTo(HashSet()) { "https://" + it.substringAfter("https://") }
    Files.newBufferedWriter(Paths.get("/tmp/1.txt")).use { writer ->
        urls.forEach {
            writer.write(it)
            writer.write("\n")
        }
    }
}

fun main(args: Array<String>) {
    Systems.loadAllProperties("config/application.properties")
    val hostName = InetAddress.getLocalHost().hostName
    var loadArguments = "-i 1000d"
    var urlIdent = ""
    val requiredCategoryResourceName = "sites/amazon/category/required-top-categories.txt"
    var requiredTopCategories = ResourceLoader.readAllLines(requiredCategoryResourceName)
            .filter { !it.startsWith("#") }

    var i = 0
    while (i < args.size - 1) {
        when {
            args[i] == "-loadArguments" -> {
                loadArguments = args[++i]
            }
            args[i] == "-urlIdent" -> {
                urlIdent = args[++i]
            }
            args[i] == "-requiredTopCategories" -> {
                requiredTopCategories = args[++i].split(" | ")
            }
        }
        ++i
    }

    val r1 = CategoryTreeCrawlerRunner(
            "zgbs",
            "sites/amazon/category/best-sellers/best-sellers.txt",
            "https://www.amazon.com/Best-Sellers/zgbs",
            loadArguments = loadArguments,
            requiredTopCategories = requiredTopCategories
    )
    val r2 = CategoryTreeCrawlerRunner(
            "most-wished-for",
            "sites/amazon/category/most-wished-for/most-wished-for.txt",
            "https://www.amazon.com/most-wished-for",
            loadArguments = loadArguments,
            requiredTopCategories = requiredTopCategories
    )
    val r3 = CategoryTreeCrawlerRunner(
            "new-releases",
            "sites/amazon/category/new-releases/new-releases.txt",
            "https://www.amazon.com/gp/new-releases",
            loadArguments = loadArguments,
            requiredTopCategories = requiredTopCategories
    )

    when(urlIdent) {
        "zgbs" -> r1.run()
        "most-wished-for" -> r2.run()
        "new-releases" -> r3.run()
        else -> {
            when(hostName) {
                "crawl0" -> r2.run()
                "crawl1" -> r2.run()
                "crawl2" -> r2.run()
                "crawl3" -> r3.run()
                else -> arrayOf(r1, r2, r3).forEach { it.run() }
            }
        }
    }
}
