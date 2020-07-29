package ai.platon.scent.examples.tools

import ai.platon.scent.ScentEnvironment
import ai.platon.scent.component.RepeatP1D1Crawler
import ai.platon.scent.context.ScentContexts
import ai.platon.scent.context.support.AbstractScentContext
import ai.platon.scent.examples.component.ParserInitializer
import org.springframework.context.annotation.AnnotationConfigApplicationContext

fun main(args: Array<String>) {
    var profile = ""
    var clazz = ""
    var start = 0
    var limit = 0
    var test = false

    var i = 0
    while (i < args.size) {
        when {
            args[i] == "-profile" -> profile = args[++i]
            args[i] == "-clazz" -> clazz = args[++i]
            args[i] == "-cstart" -> start = args[++i].toInt()
            args[i] == "-climit" -> limit = args[++i].toInt()
            args[i] == "-test" -> test = true
        }
        ++i
    }

    if (clazz.isEmpty()) {
        // TODO: use spring profile
        clazz = when(profile) {
            "news" -> "ai.platon.scent.examples.sites.abroadnews.PND1CrawlerConfig"
            else -> "ai.platon.scent.examples.sites.amazon.config.AmazonCrawlerConfig"
        }
    }

    val context = AnnotationConfigApplicationContext(Class.forName(clazz))
            .apply { register(RepeatP1D1Crawler::class.java) }
            .apply { register(ParserInitializer::class.java) }
            .let { ScentContexts.activate(it) as AbstractScentContext }

    val initializer = context.getBean<ParserInitializer>()
    val crawler = context.getBean<RepeatP1D1Crawler>()

    ScentEnvironment.checkEnvironment()

    if (start > 0) crawler.categoryUrlStart = start
    if (limit > 0) crawler.categoryUrlLimit = limit

    if (test) crawler.test() else crawler.run()
}
