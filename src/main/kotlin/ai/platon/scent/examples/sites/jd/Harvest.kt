package ai.platon.scent.examples.sites.jd

import ai.platon.scent.context.withContext
import ai.platon.scent.examples.common.WebHarvester

fun main() = withContext {
    WebHarvester(it).harvest("https://list.jd.com/list.html?cat=652,12345,12349", "-i 1s -ii 1s -ol a[href~=item]")
}
