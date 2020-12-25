select
    dom_all_attrs(dom, 'tr.comparison_table_image_row center > img[alt]', 'data-src') as `ad_asin_img`,
    dom_all_hrefs(dom, '#comparison_title, tr.comparison_table_image_row th a[href~=/dp/]') as `ad_asin`,
    dom_all_hrefs(dom, '#comparison_title, tr.comparison_table_image_row th a[href~=/dp/]') as `ad_asin_url`,
    dom_all_texts(dom, 'tr.comparison_table_image_row th i span:contains(Best Seller)') as `ad_asin_bsr`,
    dom_all_texts(dom, '#comparison_title, tr.comparison_table_image_row th a[href~=/dp/]') as `ad_asin_title`,
    dom_all_texts(dom, 'tr#comparison_price_row > td') as `ad_asin_price`,
    dom_all_texts(dom, 'tr#comparison_custormer_rating_row > td a[href~=product-reviews]') as `ad_asin_starnum`,
    dom_all_texts(dom, 'tr#comparison_custormer_rating_row > td i.a-icon-star') as `ad_asin_score`,
    dom_all_texts(dom, 'tr#comparison_sold_by_row > td') as `ad_asin_soldby`,
    dom_all_hrefs(dom, 'tr#comparison_sold_by_row > td a') as `ad_asin_soldby_url`,
    dom_all_slim_htmls(dom, 'tr#comparison_shipping_info_row > td span') as `ad_asin_shipby`,
    make_array(1, 2, 3, 4, 5, 6, 7, 8, 9, 10) as `ad_asin_position`,
    make_array(dom_base_uri(dom), 10) as `url`,
    make_array(str_substring_between(dom_base_uri(dom), '/dp/', '/ref='), 10) as `asin`,
    make_array(format_timestamp(dom_attr(dom_select_first(dom, '#PulsarMetaInformation'), 'timestamp'), 'yyyy-MM-dd HH:mm:ss'), 10) as `task_time`,
    make_array('Compare with similar items', 10) as `carousel_title`,
    make_array('similar-items', 10) as `ad_type`
from load_and_select(@url, '#HLCXComparisonTable');
