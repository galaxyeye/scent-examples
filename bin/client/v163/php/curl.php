<?php

/**
 *The x-sql api is an async api, every call returns the id of the execution immediately.
 *
 * * you can poll the result using this id
 * * you have to specify your own callbackUrl, once we have the execution done, we post the result to this url
 *   requirement for your callback handler:
 *   * method: GET
 *   * media type: application/json
 */

$fetchUrl='https://www.amazon.com/Disney-51394-Ariel-Necklace-Set/dp/B00BTX5926/ref=zg_bs_toys-and-games_1?_encoding=UTF8&psc=1&refRID=BX861MPVTN1E6SFC7C2K';
$sql=<<<EOF
select
    dom_first_text(dom, '#productTitle') as `title`,
    dom_base_uri(dom) as `url`,
    str_substring_after(dom_first_href(dom, '#wayfinding-breadcrumbs_container ul li:last-child a'), '&node=') as `category`,
    cast(dom_all_hrefs(dom, '#wayfinding-breadcrumbs_container ul li a') as varchar) as `categorylevel`,
    cast(dom_all_hrefs(dom, '#wayfinding-breadcrumbs_container ul li a') as varchar) as `categorypath`,
    cast(dom_all_hrefs(dom, '#wayfinding-breadcrumbs_container ul li a') as varchar) as `categorypathlevel`,
    dom_first_text(dom, '#wayfinding-breadcrumbs_container ul li:last-child a') as `categoryname`,
    cast(dom_all_texts(dom, '#wayfinding-breadcrumbs_container ul li a') as varchar) as `categorynamelevel`,
    dom_first_slim_html(dom, '#bylineInfo') as `brand`,
    cast(dom_all_slim_htmls(dom, '#imageBlock img') as varchar) as `gallery`,
    dom_first_slim_html(dom, '#landingImage, #imgTagWrapperId img, #imageBlock img:expr(width>400)') as `img`,
    dom_first_text(dom, '#price tr td:contains(List Price) ~ td') as `listprice`,
    dom_first_text(dom, '#price tr td:matches(^Price) ~ td') as `price`,
    str_is_not_empty(dom_first_text(dom, '#acBadge_feature_div i:contains(Best Seller)')) as `isbs`,
    str_is_not_empty(dom_first_text(dom, '#acBadge_feature_div span:contains(Amazon)')) as `isac`,
    str_is_not_empty(dom_first_text(dom, '#couponBadgeRegularVpc span:contains(Coupon)')) as `iscoupon`,

    cast(dom_all_hrefs(dom, '#usedbuyBox div:contains(Sold by) a[href~=seller], #merchant-info a[href~=seller]') as varchar) as `soldby`,
    cast(dom_all_hrefs(dom, '#usedbuyBox div:contains(Sold by) a[href~=seller], #merchant-info a[href~=seller]') as varchar) as `sellerID`,
    cast(dom_all_hrefs(dom, '#usedbuyBox div:contains(Sold by) a[href~=seller], #merchant-info a[href~=seller]') as varchar) as `marketplaceID`,
    dom_first_text(dom, '#desktop_buybox #merchant-info') as `shipby`,
    dom_first_text(dom, '#availability, #outOfStock') as `instock`,
    cast(dom_all_hrefs(dom, '#availability a, #olp-upd-new-used a, #olp-upd-new a, #certified-refurbished-version a[href~=/dp/], #newer-version a[href~=/dp/]') as varchar) as `sellsameurl`,

    str_substring_between(dom_first_text(dom, '#olp-upd-new-used a, #olp-upd-new a'), '(', ')') as `othersellernum`,
    str_is_not_empty(dom_first_text(dom, '#addToCart_feature_div span:contains(Add to Cart), #submit.add-to-cart-ubb-announce')) as `isaddcart`,
    str_is_not_empty(dom_first_text(dom, '#buyNow span:contains(Buy now)')) as `isbuy`,
    dom_first_text(dom, '#productDescription, h2:contains(Product Description) + div') as `desc`,

    cast(dom_all_slim_htmls(dom, '#prodDetails h1:contains(Feedback) ~ div a') as varchar) as `feedbackurl`,
    dom_first_text(dom, '#prodDetails table tr > th:contains(ASIN) ~ td') as `asin`,
    dom_attr(dom_owner_body(dom), '_ps_pasin') as `pasin`,
    dom_first_text(dom, '#prodDetails table tr > th:contains(Product Dimensions) ~ td') as `volume`,
    dom_first_text(dom, '#prodDetails table tr > th:contains(Item Weight) ~ td') as `weight`,
    dom_outer_html(dom_select_first(dom, '#prodDetails table tr > th:contains(Best Sellers Rank) ~ td')) as `rank`,
    dom_outer_html(dom_parent(dom_select_nth(dom, '#prodDetails table tr > th:contains(Best Sellers Rank) ~ td a', 1))) as `bigrank`,
    dom_outer_html(dom_parent(dom_select_nth(dom, '#prodDetails table tr > th:contains(Best Sellers Rank) ~ td a', 2))) as `smallrank`,
    dom_first_text(dom, '#prodDetails table tr > th:contains(Date First) ~ td') as `onsaletime`,
    is_not_empty(dom_all_imgs(dom, '#prodDetails img[src]')) as `isa`,
    str_first_integer(dom_first_text(dom, '#askATFLink, .askTopQandALoadMoreQuestions a'), 0) as `qanum`,
    str_first_integer(dom_first_text(dom, '#reviewsMedley span:contains(customer ratings)'), 0) as `reviews`,

    cast(dom_all_slim_htmls(dom, '#reviewsMedley h3:contains(Read reviews that mention) ~ div a') as varchar) as `reviewsmention`,
    str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as `score`,
    str_first_integer(dom_first_text(dom, '#reviewsMedley div span:contains(customer ratings)'), 0) as `starnum`,
    dom_first_text(dom, 'table#histogramTable:expr(width > 100) td:contains(5 star) ~ td:contains(%)') as `score5percent`,
    dom_first_text(dom, 'table#histogramTable:expr(width > 100) td:contains(4 star) ~ td:contains(%)') as `score4percent`,
    dom_first_text(dom, 'table#histogramTable:expr(width > 100) td:contains(3 star) ~ td:contains(%)') as `score3percent`,
    dom_first_text(dom, 'table#histogramTable:expr(width > 100) td:contains(2 star) ~ td:contains(%)') as `score2percent`,
    dom_first_text(dom, 'table#histogramTable:expr(width > 100) td:contains(1 star) ~ td:contains(%)') as `score1percent`,
    dom_first_href(dom, '#reviews-medley-footer a') as `reviewsurl`
from load_and_select('$fetchUrl', ':root body');
EOF;

$sql=array(
    "username" => "gJn6fUBh",
    "authToken" => "af1639a924d7232099a037e9544cf43f",
    "sql" => $sql,
    "callbackUrl" => "http://localhost:8182/api/hello/echo"
);
$json = json_encode($sql);

$url = 'http://119.45.149.30:8182/api/x/a/q';
# $url = 'http://localhost:8182/api/x/a/q';
$curl = curl_init($url);
curl_setopt($curl, CURLOPT_RETURNTRANSFER, true);
curl_setopt($curl, CURLOPT_POST, true);
curl_setopt($curl, CURLOPT_POSTFIELDS, $json);
curl_setopt($curl, CURLOPT_VERBOSE, true);

curl_setopt($curl, CURLOPT_HTTPHEADER, array( 'Content-Type: application/json', 'Content-length:' . strlen($json))); // check if the length be correct
$json_response = curl_exec($curl);
print $json_response;
