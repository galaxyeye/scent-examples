-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

-- asin  跟卖列表
-- 每天的跟卖列表不同
select
    dom_base_uri(dom) as url,
    str_substring_between(dom_base_uri(dom), '/offer-listing/', '/ref=') as asin,
    dom_first_text(dom, 'div.olpSellerColumn h3.olpSellerName') as soldby,
    dom_first_href(dom, 'div.olpSellerColumn h3.olpSellerName a') as sellerID,
    dom_first_href(dom, 'div.olpSellerColumn h3.olpSellerName a') as seller_url,
    dom_first_text(dom, 'div.olpDeliveryColumn a:contains(Shipping)') as shipby,
    dom_first_text(dom, 'div.olpPriceColumn span') as price,
    str_substring_after(dom_first_attr(dom, 'div.olpSellerColumn i.a-icon-star', 'class'), ' a-star-') as score,
    dom_own_text(dom_parent(dom_select_first(dom, 'div.olpSellerColumn i.a-icon-star'))) as reviews,
    dom_first_text(dom, 'div.olpConditionColumn') as is_out,
    ' - ' as is_log
from load_and_select(@url, '#olpOfferList div.olpOffer[role=row]');
