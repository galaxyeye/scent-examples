package ai.platon.scent.examples.sites.amazon

import ai.platon.scent.common.SqlTemplate
import ai.platon.scent.context.withContext
import ai.platon.scent.examples.common.CommonSqlExtractor
import ai.platon.scent.examples.tools.SqlConverter

fun main() = withContext { cx ->
    val productUrl = "https://www.amazon.com/LAGSHIAN-Elegant-Ruffles-Shoulder-Evening/dp/B07DY9HLZ8/ref=zg_bs_11006702011_1?_encoding=UTF8&psc=1&refRID=HV43R6PJNHN3JYK1D2JA"
    val offerListingUrl = "https://www.amazon.com/gp/offer-listing/B076H3SRXG/ref=dp_olp_NEW_mbc?ie=UTF8&condition=NEW"
    val asinBestUrl = "https://www.amazon.com/Best-Sellers-Automotive/zgbs/automotive/ref=zg_bs_nav_0"
    val sellerUrl = "https://www.amazon.com/sp?_encoding=UTF8&asin=&isAmazonFulfilled=1&isCBA=&marketplaceID=ATVPDKIKX0DER&orderID=&protocol=current&seller=A2QJQR8DPHL921&sshmPath="
    val sellerAsinListUrl = "https://www.amazon.com/s?me=A2QJQR8DPHL921&marketplaceID=ATVPDKIKX0DER"
    val sellerAsinUrl = "https://www.amazon.com/Wireless-Bluetooth-Ear-TWS-Headphones-Waterproof/dp/B07ZQCST89/ref=sr_1_1?dchild=1&m=A2QJQR8DPHL921&marketplaceID=ATVPDKIKX0DER&qid=1595908896&s=merchant-items&sr=1-1"
    val sellerBrandListUrl = "https://www.amazon.com/s?me=A2QJQR8DPHL921&marketplaceID=ATVPDKIKX0DER"
    val keywordAsin = "https://www.amazon.com/s?k=dresser&rh=n:1055398,n:1063306,n:1063308"
    val categoryListUrl = "https://www.amazon.com/b?node=16225007011&pf_rd_r=345GN7JFE6VHWVT896VY&pf_rd_p=e5b0c85f-569c-4c90-a58f-0c0a260e45a0"
    val extractor = CommonSqlExtractor(cx)

    var i = 0
    listOf(productUrl to "asin-ad-also-view-extract.sql",
            productUrl to "asin-ad-similiar-extract.sql",
            productUrl to "asin-ad-sponsored-extract.sql",
            asinBestUrl to "asin-best-extract.sql",
            sellerAsinListUrl to "seller-asin-extract.sql",
            sellerBrandListUrl to "seller-brand-extract.sql",
            offerListingUrl to "asin-follow-extract.sql",
            categoryListUrl to "category-asin-extract.sql",
            keywordAsin to "keyword-asin-extract.sql"
    ).filter { "asin" in it.second }
            .map { it.first to SqlTemplate.load("sites/amazon/sql/extract/${it.second}") }
            .forEach { (url, sqlTemplate) ->
                println()
                println("${++i}.\t${sqlTemplate.resource}")
                if (sqlTemplate.template.contains("create table", ignoreCase = true)) {
                    println(SqlConverter.createSql2extractSql(sqlTemplate.template))
                } else {
                    extractor.query(sqlTemplate.injectUrl(url))
                }
            }
}
