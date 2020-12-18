package ai.platon.scent.examples.sites.zhefengle

import ai.platon.scent.ScentContext
import ai.platon.scent.context.withContext
import ai.platon.scent.examples.common.WebHarvester

class Harvest(context: ScentContext): WebHarvester(context) {

    val seeds = listOf(
            "https://www.eastbay.com/category/sport/baseball/mens/shoes.html",
            "https://www.calvinklein.us/en/womens-underwear-shop-all?ab=underwear_desktop_4#collection=bold-1981",
            "https://www.ralphlauren.com/men-clothing-polo-shirts?webcat=men%7Cclothing%7CPolo%20Shirts",
            "https://www.footlocker.com/category/mens/converse.html",
            "https://usa.tommy.com/en/jackets-outerwear-men",
            "https://www.moosejaw.com/navigation/clothing"
    ).filter { it.isNotBlank() }

    fun harvest() {
        val url = seeds[5]
        harvest(url)
    }
}

fun main() = withContext {
    Harvest(it).harvest()
}
