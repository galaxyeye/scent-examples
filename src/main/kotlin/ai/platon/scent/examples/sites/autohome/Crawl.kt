package ai.platon.scent.examples.sites.autohome

import ai.platon.pulsar.common.Systems
import ai.platon.scent.context.withContext
import ai.platon.scent.examples.common.Crawler

fun main() {
    Systems.loadAllProperties("config/sites/amazon/other/application-p1d1.properties")

    val seed = "https://mall.autohome.com.cn/list/0-0-33-0-0-0-0-0-0-1.html"
    val args = "-i 1s -ii 100d -ol a[href~=item] -tl 100"

    withContext {
        val crawler = Crawler(it)
        repeat(10) {
            crawler.loadOutPages(seed, args)
        }
    }
}
