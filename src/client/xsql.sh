#!/bin/bash

targetUrl="https://www.amazon.com/Best-Sellers-Toys-Games/zgbs/toys-and-games/ref=zg_bs_toys-and-games_home_all?pf_rd_p=089b8285-7691-4849-a7f5-b2fca56bf24a&pf_rd_s=center-1&pf_rd_t=2101&pf_rd_i=home&pf_rd_m=ATVPDKIKX0DER&pf_rd_r=7Z41QZGQ4X56MJK1CJV0&pf_rd_r=7Z41QZGQ4X56MJK1CJV0&pf_rd_p=089b8285-7691-4849-a7f5-b2fca56bf24a"
sql="
select
    dom_base_uri(dom) as url,
    str_substring_between(dom_first_href(dom, 'span.zg-item a'), '/dp/', '/ref=') as asin,
    str_substring_between(dom_base_uri(dom), '.com/', '/ref=') as category,
    dom_first_integer(dom, 'span.zg-badge-text', 0) as rank,
    dom_first_text(dom, 'div > a > span.a-color-price') as price,
    dom_first_text(dom, 'span.zg-item a > div:expr(img=0 && char>30)') as title,
    dom_first_attr(dom, 'span.zg-item div img[src]', 'src') as pic,
    str_substring_between(dom_first_attr(dom, 'span.zg-item div a i.a-icon-star', 'class'), ' a-star-', ' ') as score,
    dom_first_text(dom, 'span.zg-item div a:has(i.a-icon-star) ~ a') as starnum
from load_and_select(@url, 'ol#zg-ordered-list > li.zg-item-immersion');
"

# remove control characters
sql=$(echo "$sql" | tr -s "[:cntrl:]" " ")
# replace @url by the actual target url
sql=${sql/@url/\'$targetUrl\'}
# build the json data to send
json="{\"sql\": \"$sql\"}"

# echo $json

host=119.45.149.30
curl -H "Accept: application/json" -H "Content-type: application/json" -X POST -d "$json" "http://$host:8182/api/x/sql/json"
