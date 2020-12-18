package ai.platon.scent.examples.tools

import ai.platon.scent.examples.common.CommonSqlExtractor
import ai.platon.scent.ql.h2.context.withSQLContext

fun main() = withSQLContext { cx ->
    val sql = """
select
    dom_base_uri(dom) as `url`,
    str_substring_after(dom_base_uri(dom), '&rh=') as `nodeID`,
    str_substring_between(dom_base_uri(dom), 'k=', '&') as `keyword`,
    dom_first_text(dom, 'a span.a-price:first-child span.a-offscreen') as `price`,
    dom_first_text(dom, 'a:has(span.a-price) span:containsOwn(/Item)') as `priceperitem`,
    dom_first_text(dom, 'a span.a-price[data-a-strike] span.a-offscreen') as `listprice`,
    dom_first_text(dom, 'h2 a') as `title`,
    dom_first_href(dom, 'h2 a') as `asin_url`,
    str_substring_between(dom_first_href(dom, 'h2 a'), '/dp/', '/ref=') as `asin`,
    dom_first_text(dom, 'div span[class]:containsOwn(by) ~ span[class~=a-color-secondary]') as `brand`,
    dom_first_text(dom, 'div a:containsOwn(new offers), div a span:containsOwn(new offers)') as `follow_seller_num`,

    dom_first_attr(dom, 'span[data-component-type=s-product-image] a img', 'src') as `pic`,
    dom_first_text(dom, 'div.a-section:expr(a>0) span[aria-label~=stars] i span') as score,
    dom_first_text(dom, 'div.a-section:expr(a>0) span[aria-label~=stars] ~ span a') as `reviews`,
    dom_first_text(dom, 'div span[aria-label~=Amazon], div span:containsOwn(Amazon)') as `isac`,
    dom_first_text(dom, 'a[id~=BESTSELLER], div a[href~=bestsellers], div span:containsOwn(Best Seller)') as `isbs`,
    dom_first_text(dom, 'span[data-component-type=s-coupon-component]') as `iscoupon`,
    dom_first_text(dom, 'div.a-section span:contains(in stock), div.a-section span:contains(More Buying Choices)') as `instock`,
    dom_first_text(dom, 'div span:containsOwn(Sponsored)') as `isad`,
    dom_first_text(dom_owner_body(dom), 'ul.a-pagination li.a-selected') as `adposition_page`,
    (dom_top(dom) - dom_top(dom_select_first(dom_owner_body(dom), 'div.s-main-slot.s-result-list.s-search-results'))) / dom_height(dom) as `adposition_page_row`,
    dom_element_sibling_index(dom) as `position_in_list`,
    dom_width(dom_select_first(dom, 'a img[srcset]')) as `pic_width`,
    dom_height(dom_select_first(dom, 'a img[srcset]')) as `pic_height`
from load_and_select('https://www.amazon.com/s?k=dresser&rh=n:1055398,n:1063306,n:1063308', 'div.s-main-slot.s-result-list.s-search-results > div:expr(img>0)');
    """.trimIndent()

    CommonSqlExtractor(cx).query(sql)
}
