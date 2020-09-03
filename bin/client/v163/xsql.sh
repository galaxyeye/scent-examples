#!/bin/bash
username=gJn6fUBh
authToken=af1639a924d7232099a037e9544cf43f
# replace to your own callback url
callbackUrl="http://localhost:8182/api/hello/echo"

targetUrl="https://www.amazon.com/Etekcity-Multifunction-Stainless-Batteries-Included/dp/B0113UZJE2/ref=zg_bs_home-garden_21?_encoding=UTF8&psc=1&refRID=TS59NMS2K6A2PSXTTS4F"
sql="
select
    dom_base_uri(dom) as url,
    str_substring_between(dom_base_uri(dom), '/dp/', '/ref=') as asin,
    dom_element_sibling_index(dom) as ad_asin_postion,
    str_substring_between(dom_first_href(dom, 'div a[href~=/dp/]'), '/dp/', '/ref=') as ad_asin,
    str_substring_between(dom_first_href(dom, 'div a[href~=/dp/]'), '/dp/', '/ref=') as ad_asin_bsr,
    dom_first_href(dom, 'div a[href~=/dp/]') as ad_asin_url,
    dom_first_text(dom, 'div a div:expr(img=0 && char>30)') as ad_asin_title,
    dom_first_text(dom, 'div a span.a-color-price') as ad_asin_price,
    dom_first_attr(dom, 'div a img[data-a-dynamic-image]', 'src') as ad_asin_img,
    str_substring_after(dom_first_attr(dom, 'div > div > a i.a-icon-star', 'class'), ' a-star-') as ad_asin_score,
    dom_first_text(dom, 'div a:contains(out of 5 stars) ~ a[href~=reviews]') as ad_asin_reviews
from load_and_select(@url, '#sims-consolidated-2_feature_div ol.a-carousel li');
"

# remove control characters
sql=$(echo "$sql" | tr -s "[:cntrl:]" " ")
# replace @url by the actual target url
sql=${sql/@url/\'$targetUrl\'}
# build the json data to send
json="{\"username\": \"$username\", \"authToken\": \"$authToken\", \"sql\": \"$sql\", \"callbackUrl\": \"$callbackUrl\"}"

# echo $json

host=localhost
curl -H "Accept: application/json" -H "Content-type: application/json" -X POST -d "$json" "http://$host:8182/api/x/a/q"
