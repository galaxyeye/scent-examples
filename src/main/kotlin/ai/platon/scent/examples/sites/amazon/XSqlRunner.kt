package ai.platon.scent.examples.sites.amazon

import ai.platon.pulsar.common.sql.ResultSetFormatter
import ai.platon.pulsar.ql.h2.SqlUtils
import ai.platon.scent.common.sql.SqlConverter
import ai.platon.scent.common.sql.SqlTemplate
import ai.platon.scent.examples.common.CommonSqlExtractor
import ai.platon.scent.ql.h2.context.ScentSQLContext
import ai.platon.scent.ql.h2.context.ScentSQLContexts
import ai.platon.scent.ql.h2.context.withSQLContext
import org.slf4j.LoggerFactory

class XSqlRunner(
        val xsqls: List<Pair<String, String>>,
        val xsqlFilter: (String) -> Boolean,
        val resourcePrefix: String,
        val cx: ScentSQLContext = ScentSQLContexts.activate()
) {
    private val log = LoggerFactory.getLogger(XSqlRunner::class.java)

    val extractor = CommonSqlExtractor(cx)
    val session = extractor.session

    fun run() {
        var i = 0
        xsqls.filter { xsqlFilter(it.second) }
                .map {
                    val name = it.second.substringAfterLast("/").substringBeforeLast(".sql")
                    it.first to SqlTemplate.load("$resourcePrefix/${it.second}", name = name)
                }
                .forEach { (url, sqlTemplate) ->
                    log.info("${++i}.\t${sqlTemplate.resource}")
                    if (sqlTemplate.template.contains("create table", ignoreCase = true)) {
                        log.info(SqlConverter.createSql2extractSql(sqlTemplate.template))
                    } else {
                        val doc = session.loadDocument(url, "-i 1s")
                        val sql = sqlTemplate.createInstance(url)

                        var rs = extractor.query(sql, printResult = true)

                        if (sqlTemplate.resource?.contains("x-similar-items.sql") == true) {
                            rs = SqlUtils.transpose(rs)
                            println("Transposed: ")
                            rs.beforeFirst()
                            println(ResultSetFormatter(rs, withHeader = true).toString())
                        }

                        val count = SqlUtils.count(rs)
                        val path = session.export(doc)
                        log.info("Extracted $count records, document is exported to file://$path")
                    }
                }
    }
}

fun main() = withSQLContext { cx ->
    val resourcePrefix = "config/sites/amazon/crawl/parse/sql"

    val productUrl = "https://www.amazon.com/Assistant-2700-6500K-Tunable-Changing-Dimmable/dp/B07L4RR1N2/ref=amzdv_cabsh_dp_3/144-0825628-8630255?_encoding=UTF8&pd_rd_i=B07L4RR1N2&pd_rd_r=7cde081c-a7c0-4b56-85da-89b5b800bd80&pd_rd_w=jTOsh&pd_rd_wg=ydYvv&pf_rd_p=10835d28-3e4a-4f93-bb3c-443ad482b1c9&pf_rd_r=MK4N7F3G6ADHH2TS3VK4&psc=1&refRID=MK4N7F3G6ADHH2TS3VK4"
    val productAlsoReview = "https://www.amazon.com/Seagate-Portable-External-Hard-Drive/dp/B07CRG94G3/ref=lp_16225007011_1_11?s=computers-intl-ship&ie=UTF8&qid=1596590947&sr=1-11"
    val offerListingUrl = "https://www.amazon.com/gp/offer-listing/B076H3SRXG/ref=dp_olp_NEW_mbc"
    val asinBestUrl = "https://www.amazon.com/Best-Sellers-Automotive/zgbs/automotive/ref=zg_bs_nav_0"
    val sellerUrl = "https://www.amazon.com/sp?_encoding=UTF8&asin=&isAmazonFulfilled=1&isCBA=&marketplaceID=ATVPDKIKX0DER&orderID=&protocol=current&seller=A2QJQR8DPHL921&sshmPath="
    val sellerAsinListUrl = "https://www.amazon.com/s?me=A2QJQR8DPHL921&marketplaceID=ATVPDKIKX0DER"
    val sellerAsinUrl = "https://www.amazon.com/Wireless-Bluetooth-Ear-TWS-Headphones-Waterproof/dp/B07ZQCST89/ref=sr_1_1?dchild=1&m=A2QJQR8DPHL921&marketplaceID=ATVPDKIKX0DER&qid=1595908896&s=merchant-items&sr=1-1"
    val sellerBrandListUrl = "https://www.amazon.com/s?me=A2QJQR8DPHL921&marketplaceID=ATVPDKIKX0DER"
    val keywordAsin = "https://www.amazon.com/s?k=dresser&rh=n:1055398,n:1063306,n:1063308"
    val categoryListUrl = "https://www.amazon.com/b?node=16225007011&pf_rd_r=345GN7JFE6VHWVT896VY&pf_rd_p=e5b0c85f-569c-4c90-a58f-0c0a260e45a0"
    val productReviewUrl = "https://www.amazon.com/Dash-Mini-Maker-Individual-Breakfast/product-reviews/B01M9I779L/ref=cm_cr_dp_d_show_all_btm?ie=UTF8&reviewerType=all_reviews"
    val mostWishedFor = "https://www.amazon.com/gp/most-wished-for/boost/ref=zg_mw_nav_0/141-8986881-4437304"

    val sqls = listOf(
            productUrl to "crawl/x-asin.sql",
            productUrl to "crawl/x-asin-sims-consolidated-1.sql",
            productUrl to "crawl/x-asin-sims-consolidated-2.sql",
            productUrl to "crawl/x-asin-sims-consolidated-3.sql",
            productUrl to "crawl/x-similar-items.sql",
            productReviewUrl to "crawl/x-asin-reviews.sql",
            mostWishedFor to "crawl/x-asin-most-wished-for.sql",

            productAlsoReview to "asin-ad-also-view-extract.sql",
            productUrl to "asin-ad-similiar-extract.sql",
            productUrl to "asin-ad-sponsored-extract.sql",
            asinBestUrl to "asin-best-extract.sql",
            sellerAsinListUrl to "seller-asin-extract.sql",
            sellerBrandListUrl to "seller-brand-extract.sql",
            offerListingUrl to "asin-follow-extract.sql",
            categoryListUrl to "category-asin-extract.sql",
            keywordAsin to "keyword-asin-extract.sql"
    )

    val xsqlFilter = { xsql: String -> "x-asin.sql" in xsql }
    XSqlRunner(sqls, xsqlFilter, resourcePrefix, cx).run()
}
