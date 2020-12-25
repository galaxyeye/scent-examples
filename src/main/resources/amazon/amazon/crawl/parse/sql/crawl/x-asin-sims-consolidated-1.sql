-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

-- asin 所在页面 广告位 追踪
-- set @url='https://www.amazon.com/Etekcity-Multifunction-Stainless-Batteries-Included/dp/B0113UZJE2/ref=zg_bs_home-garden_21?_encoding=UTF8&psc=1&refRID=TS59NMS2K6A2PSXTTS4F';
select
    dom_base_uri(dom) as url,
    str_substring_between(dom_base_uri(dom), '/dp/', '/ref=') as asin,
    dom_first_own_text(dom_owner_body(dom), '#sims-consolidated-1_feature_div h2.a-carousel-heading') as carousel_title,
    dom_first_text(dom_owner_body(dom), '#sims-consolidated-1_feature_div h2.a-carousel-heading div.sp_desktop_sponsored_label') as is_sponsored,
    dom_element_sibling_index(dom) as ad_asin_position,
    dom_first_href(dom, 'div[data-asin] a[href~=/dp/], div[data-asin] a[href~=/slredirect/]') as ad_asin_url,
    dom_first_text(dom, 'div[data-asin] a div:expr(img=0 && char>30)') as ad_asin_title,
    dom_first_text(dom, 'div[data-asin] a span.a-color-price') as ad_asin_price,
    dom_first_attr(dom, 'div[data-asin] a img[data-a-dynamic-image]', 'src') as ad_asin_img,
    dom_first_text(dom, 'div[data-asin] > div > a i.a-icon-star') as ad_asin_score,
    str_substring_after(dom_first_attr(dom, 'div[data-asin] > div > a i.a-icon-star', 'class'), ' a-star-') as ad_asin_score_2,
    dom_first_text(dom, 'div[data-asin] a:contains(out of 5 stars) ~ a[href~=reviews]') as ad_asin_starnum,
    format_timestamp(dom_attr(dom_select_first(dom, '#PulsarMetaInformation'), 'timestamp'), 'yyyy-MM-dd HH:mm:ss') as `task_time`,
    'sims-1' as `ad_type`
from load_and_select(@url, '#sims-consolidated-1_feature_div ol.a-carousel li');
