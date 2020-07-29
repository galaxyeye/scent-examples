-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

-- asin 所在页面 广告位 追踪
-- set @url='https://www.amazon.com/Etekcity-Multifunction-Stainless-Batteries-Included/dp/B0113UZJE2/ref=zg_bs_home-garden_21?_encoding=UTF8&psc=1&refRID=TS59NMS2K6A2PSXTTS4F';
select
    dom_base_uri(dom) as url,
    str_substring_between(dom_base_uri(dom), '/dp/', '/ref=') as asin,
    dom_element_sibling_index(dom) as ad_asin_postion,
    dom_first_attr(dom, 'div[data-asin]', 'data-asin') as ad_asin,
    dom_first_attr(dom, 'div[data-asin]', 'data-asin') as ad_asin_bsr,
    dom_first_href(dom, 'div.a-section > a') as ad_asin_url,
    dom_first_text(dom, 'div.a-section > a > div:expr(img=0 && char>30)') as ad_asin_title,
    dom_first_text(dom, 'div.a-section > div a span.a-color-price') as ad_asin_price,
    dom_first_attr(dom, 'div.a-section > a img[data-a-dynamic-image]', 'src') as ad_asin_img,
    str_substring_after(dom_first_attr(dom, 'div.a-section > div > a i.a-icon-star', 'class'), ' a-star-') as ad_asin_score,
    dom_first_text(dom, 'div.a-section > div > a i.a-icon-star ~ span') as ad_asin_starnum
from load_and_select(@url, '#sp_detail ol li');
