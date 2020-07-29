package ai.platon.scent.examples.tools

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebDb
import ai.platon.scent.context.withContext
import org.apache.commons.codec.digest.DigestUtils
import java.nio.file.Files
import kotlin.streams.toList

fun main() {
    val exportBaseDir = AppPaths.WEB_CACHE_DIR
    val screenshotBaseDir = AppPaths.WEB_CACHE_DIR.resolve("cache")
    val screenshotPaths = Files.walk(screenshotBaseDir)
            .filter { Files.isRegularFile(it) }
            .filter { it.toString().endsWith("png") }
            .toList()

    withContext { cx ->
        val indexDocument = FeaturedDocument.createShell(AppConstants.EXAMPLE_URL)
        val body = indexDocument.document.body()
        body.appendElement("h2").text("Abroad news")
        body.appendElement("div").attr("class", "stage")
                .appendElement("ol")
        val ol = indexDocument.selectFirst(".stage ol")

        val session = cx.createSession()
        val webDb = session.pulsarContext.getBean<WebDb>()
        var i = 0
        webDb.scan("").asSequence()
                .filterNot { it.url.contains("amazon.com") }
                .filterNot { it.url.contains("platon.ai") }
                .forEach { page ->
                    val content = page.contentAsString
                    if (content.length > 10_000) {
                        val url = page.url
                        val document = session.parse(page)
                        document.absoluteLinks()

                        document.stripScripts()
                        val readableBytes = Strings.readableBytes(content.length.toLong())
                        val path = session.export(document, "style", true)
                        val relativePath = exportBaseDir.relativize(path)

                        document.stripStyles()
                        val path2 = session.export(document, "nostyle", true)
                        val relativePath2 = exportBaseDir.relativize(path2)

                        val fileId = DigestUtils.md5Hex(url)
                        val screenshotPath = screenshotPaths
                                .firstOrNull { it.toString().contains(fileId) }
                                ?:return@forEach
                        val relativeScreenshotPath = exportBaseDir.relativize(screenshotPath)

                        val li = ol.appendElement("li")
                        li.appendElement("a")
                                .attr("href", "$relativePath")
                                .attr("target", "_blank")
                                .text(document.title.takeUnless { it.isBlank() }?:"(no title)")

                        li.appendElement("span").text(" | ")
                        li.appendElement("a")
                                .attr("href", "$relativePath2")
                                .attr("target", "_blank")
                                .text("no style")

                        li.appendElement("span").text(" | ")
                        li.appendElement("a")
                                .attr("href", "$relativeScreenshotPath")
                                .attr("target", "_blank")
                                .text("screenshot")

                        println("${++i}.\t$readableBytes - file://$path | $screenshotPath | $url")
                    }
        }

        val path = indexDocument.exportTo(exportBaseDir.resolve("index.htm"))
        println("Index document is exported to file://$path")
    }
}
