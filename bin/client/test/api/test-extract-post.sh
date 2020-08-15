#!/bin/bash

targetUrl="https://www.amazon.com/Best-Sellers-Toys-Games/zgbs/toys-and-games/ref=zg_bs_toys-and-games_home_all"
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

sql=$(echo "$sql" | tr -s "[:cntrl:]" " ")
sql=${sql/@url/\'$targetUrl\'}
json="{\"sql\": \"$sql\"}"

echo $json
#exit

curl -H "Accept: application/json" -H "Content-type: application/json" -X POST -d "$json" "http://$host:8182/api/x/sql/json"
