package ai.platon.scent.examples.sites.amazon

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.scent.context.withContext

fun main() {
//    val productUrl = "https://www.amazon.com/gp/movers-and-shakers"
    val portalUrl = "https://www.amazon.co.jp/ -i 1s"

    val args = "-i 1s"

    withContext { cx ->
        System.clearProperty(CapabilityTypes.BROWSER_LAUNCH_SUPERVISOR_PROCESS)
        System.setProperty(CapabilityTypes.BROWSER_DRIVER_HEADLESS, "false")

        val session = cx.createSession()
        val page = session.load(portalUrl, args)
        val document = session.parse(page)
//        document.selectFirst("body").attributes().forEach { println(it.html()) }
//        val path = session.export(page)
//        println("Exported to file://$path")

        document.select("#zg_browseRoot ul li a").forEach { println(it.attr("href")) }
    }
}
