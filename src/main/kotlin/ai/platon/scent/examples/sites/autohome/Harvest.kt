package ai.platon.scent.examples.sites.autohome

import ai.platon.scent.context.withContext
import ai.platon.scent.examples.common.WebHarvester

fun main() = withContext {
    it.scentEnvironment.checkEnvironment()
    WebHarvester(it).harvest("https://mall.autohome.com.cn/list/0-0-33-0-0-0-0-0-0-1.html", "-i 1s -ii 1s -ol a[href~=detail]")
}
