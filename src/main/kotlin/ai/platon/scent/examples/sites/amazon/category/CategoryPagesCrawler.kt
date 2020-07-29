package ai.platon.scent.examples.sites.amazon.category

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.crawl.Crawler
import ai.platon.pulsar.dom.nodes.Anchor
import ai.platon.pulsar.dom.nodes.node.ext.left
import ai.platon.pulsar.dom.nodes.node.ext.ownerDocument
import ai.platon.pulsar.dom.select.getAnchors
import ai.platon.pulsar.persist.WebPage
import ai.platon.scent.ScentContext
import ai.platon.scent.examples.common.StreamingSqlCrawler
import ai.platon.scent.context.withContext
import kotlinx.coroutines.runBlocking
import org.jsoup.nodes.Element
import org.jsoup.select.NodeTraversor
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermissions
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger

private class CategoryTask(
        val url: String,
        val categoryName: String,
        val anchor: Anchor
)

class CategoryPagesCrawler(val context: ScentContext): Crawler(context.pulsarContext) {
    // private val siteDirectory = "https://www.amazon.com/gp/site-directory"
    private val categoryPortal = "https://www.amazon.com/Best-Sellers/zgbs"

    private val loadOptions = LoadOptions.parse("-i 70d")
    private lateinit var rootCategoryNode: GlobalCategoryNode
    private val allCategoryUrls = ConcurrentSkipListSet<String>()
    private val processedUrls = ConcurrentSkipListSet<String>()
    private val knownPaths = ConcurrentSkipListSet<String>()
    private val requiredTopCategories = ResourceLoader.readAllLines("sites/amazon/category/hot/required-top-categories.txt")
    private val dataDirectory = AppPaths.getTmp("category").resolve(DateTimes.formatNow("HHmm"))
    private val categoryTreePath = dataDirectory.resolve("amazon-categories.txt")
    private val redundantUrlParameters = arrayOf("qid", "ref", "_ref", "ref_")
    // private val redundantUrlParts = mapOf("ref=.+\\?".toRegex() to "?")
    private val redundantUrlParts = mapOf<String, String>()
    private val crawlRound = AtomicInteger()

    init {
        val line = String.format("%s | %s | %s | %s | %s | %s\n",
                "id", "parent id", "depth", "numSubcategories", "category path", "url")

        Files.createDirectories(dataDirectory)
        Files.writeString(categoryTreePath, line, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
    }

    fun crawl() {
        val page = session.load(categoryPortal, loadOptions)
        rootCategoryNode = GlobalCategoryNode("Root", page.url)
        val document = session.parse(page)
        document.absoluteLinks()
        document.select("ul#zg_browseRoot ul").forEach {
            extractSubcategoryAnchorsTo(it, "li a", rootCategoryNode)
        }

        val topSubcategoryAnchors = rootCategoryNode.subcategoryAnchors.filter { it.text in requiredTopCategories }
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
        val allCategoryUrlsFile = dataDirectory.resolve("all-category-urls.txt")
        Files.writeString(allCategoryUrlsFile, text, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
    }

    fun report() {
        format(rootCategoryNode)
        generateScripts()
    }

    private fun crawlCategoriesRecursively(depth: Int, category: GlobalCategoryNode) {
        crawlRound.incrementAndGet()
        log.info("$crawlRound.\t[$depth].............................................")
        log.info(category.path)
        log.info(category.url)

        createSubcategoriesFromSubcategoryAnchors(category)

        val urls = category.children.flatMap { it.subcategoryAnchors.map { it.url } }
        if (urls.isNotEmpty()) {
            log.info("Loading {} links: ", urls.size)
            runBlocking {
                StreamingSqlCrawler(urls.asSequence(), loadOptions, context).use { it.run() }
            }
        }

        category.children.parallelStream().forEach { subcategory ->
            if (subcategory.url == category.url) {
                log.warn("Subcategory has the same url as the parent, ignore | {} <- {}", subcategory.url, category.url)
                return@forEach
            }

            if (subcategory.path == category.path) {
                log.warn("Subcategory has the same path as the parent, ignore | {} <- {}",
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
    private fun createSubcategoriesFromSubcategoryAnchors(parentCategory: GlobalCategoryNode) {
        log.info("There {} subcategories to crawl under [{}]({})",
                parentCategory.subcategoryAnchors.size, parentCategory.path, parentCategory.depth)

        val counter = AtomicInteger()
        parentCategory.subcategoryAnchors.parallelStream().forEach { anchor ->
            try {
                val j = counter.incrementAndGet()
                loadPageAndCreateSubcategory(j, anchor, parentCategory)
            } catch (e: Throwable) {
                log.warn("Unexpected throwable", e)
            }
        }
    }

    private fun loadPageAndCreateSubcategory(j: Int, anchor: Anchor, parentCategory: GlobalCategoryNode) {
        processedUrls.add(anchor.url)

        val url = anchor.url
        val categoryName = anchor.text.trim()
        val expectedPath = parentCategory.path + " > " + categoryName
        if (expectedPath !in knownPaths && categoryName !in parentCategory.path) {
            val task = CategoryTask(url, categoryName, anchor)

            log.info("$j.\t[{}](expected) Loading category page | {}", expectedPath, task.url)
            // val categoryPage = session.load(task.url, loadOptions)
            val categoryPage = session.getOrNil(task.url)
            if (categoryPage.crawlStatus.isFetched) {
                val subcategory = createSubcategoryNode(task, categoryPage, parentCategory)
                log.info("$j.\t[{}](actual) Category is created | {}", subcategory.path, url)
            }
        }
    }

    private fun createSubcategoryNode(task: CategoryTask, categoryPage: WebPage, parentCategory: GlobalCategoryNode): GlobalCategoryNode {
        val subcategory = GlobalCategoryNode(task.categoryName, categoryPage.url, parentCategory)
        parentCategory.children.add(subcategory)
        knownPaths.add(subcategory.path)

        val document = session.parse(categoryPage)
        // require(document.body.area > 1000 * 1000)
        document.absoluteLinks()

        document.select("ul.a-pagination li.a-normal a").forEach {
            subcategory.paginationLinks.add(Anchor(it))
        }

        arrayOf(
//                "#nav-xshop",
//                "#mars-fs-subnav",
//                "#departments",
//                "#nav-subnav",
//                ".left_nav.browseBox",
//                "#leftNav",
                "#zg_browseRoot" // best seller category, https://www.amazon.com/Best-Sellers-Beauty/zgbs/beauty/ref=zg_bs_nav_0
        ).forEach { selector ->
            document.body.select(selector).forEach { linkSection ->
                when (selector) {
//                    "#nav-xshop" -> parseCategorySectionLinks(category, linkSection, "a")
                    "#mars-fs-subnav" -> parseTopNav(subcategory, selector, linkSection)
                    "#departments", ".left_nav.browseBox", "#leftNav" -> parseLeftNav(subcategory, selector, linkSection)
                    "#zg_browseRoot" -> collectBestSellersSubcategoryAnchors(subcategory, selector, linkSection)
//                    "#nav-subnav" -> parseCategorySectionLinks(category, linkSection, "a[href~=/b/]")
//                    "#leftNav" -> parseCategoryNavBox(linkSection, "ul:nth-child(1) li a[href~=/s/]")
                }
            }
        }

        return subcategory
    }

    private fun parseTopNav(category: GlobalCategoryNode, selector: String, linkSection: Element) {
        val caption = arrayOf("#n-title", "h3", "ul > li > a > b")
                .mapNotNull { linkSection.selectFirst(it)?.text() }.firstOrNull()
        val url = linkSection.ownerDocument.baseUri()
        require(url == category.url)

        // here is a bug
        val root = LocalCategoryNode(caption
                ?: "[Unknown Caption]", url, 0, null)
//        root.page = category.page
        root.selector = selector
        category.localCategoryNodeTree = root

        NodeTraversor.traverse(LocalCategoryTreeBuilder(root), linkSection)

        TODO("NOT IMPLEMENTED")
    }

    private fun parseLeftNav(category: GlobalCategoryNode, selector: String, linkSection: Element) {
//        if (selector == "#leftNav") {
//            return
//        }

        val caption = arrayOf("#n-title", "h3", "ul > li > a > b")
                .mapNotNull { linkSection.selectFirst(it)?.text() }.firstOrNull()
        val url = linkSection.ownerDocument.baseUri()
        require(url == category.url)
        val root = LocalCategoryNode(caption
                ?: "[Unknown Caption]", url, category.depth, category.localCategoryNodeTree)
//        root.page = category.page
        root.selector = selector
        category.localCategoryNodeTree = root

        NodeTraversor.traverse(LocalCategoryTreeBuilder(root), linkSection)

        TODO("NOT IMPLEMENTED")
    }


    // https://www.amazon.com/Best-Sellers-Beauty/zgbs/beauty/ref=zg_bs_nav_0
    private fun collectBestSellersSubcategoryAnchors(currentCategory: GlobalCategoryNode, selector: String, localCategoryRootElement: Element) {
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

        log.info("Collecting subcategory anchors for category [{}]|[{}]({}) | {}",
                name, currentCategory.path, currentCategory.depth, currentCategory.url)

        extractSubcategoryAnchorsTo(localCategoryTreeRootElement, "li a", currentCategory)
    }

    // https://www.amazon.com/Best-Sellers-Beauty/zgbs/beauty/ref=zg_bs_nav_0
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
        log.warn("Extracting subcategory links in {}", linkSection.ownerDocument().baseUri())

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

        if (url.contains("Best-Sellers", ignoreCase = true)) {
            var parts = u.split("/")
            if (parts.last().matches("(\\d{3})-(\\d+)-(\\d+)".toRegex())) {
                parts = parts.dropLast(1)
            }
            u = parts.filterNot { it.startsWith("ref=") }.joinToString("/")
            return u.substringBefore("/ref=")
        }

        redundantUrlParameters.forEach { q ->
            u = Urls.removeQueryParameters(u, q)
        }
        redundantUrlParts.forEach { (key, value) ->
            u = key.replace(url, value)
        }
        return u
    }

    private fun generateScripts() {
        val path = dataDirectory.resolve("gen.sh")
        val cmd = """
                cat amazon-categories.txt | cut -d "|" -f 1-5 > amazon-categories-no-links.txt

            """.trimIndent()
        Files.writeString(path, cmd, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxrw-r--"))
    }

    private fun format(category: GlobalCategoryNode) {
        try {
            val target = "amazon-categories." + DateTimes.formatNow("HHmmss") + ".txt"
            Files.move(categoryTreePath, categoryTreePath.resolveSibling(target))
        } catch (e: IOException) {
            log.warn(e.message)
        }

        formatRecursively(category)
    }

    private fun formatRecursively(category: GlobalCategoryNode) {
        print(category)
        category.children.forEach {
            formatRecursively(it)
        }
    }

    private fun print(category: GlobalCategoryNode) {
        val line = category.toDataNode().toString()
        // val l = "-".repeat(category.depth) + category.path
        println(line)
        Files.writeString(categoryTreePath, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    }
}

fun main() {
    Systems.loadAllProperties("application.properties")

    withContext {
        val crawler = CategoryPagesCrawler(it)
        crawler.crawl()
        crawler.report()
    }
}
