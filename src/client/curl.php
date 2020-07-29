<?php

var $url = 'localhost:8182/sql/q';
var $sql=<<<EOF
select
    dom_base_uri(dom) as url,
    str_substring_between(dom_first_href(dom, 'span.zg-item a'), '/dp/', '/ref=') as `asin`,
    str_substring_between(dom_base_uri(dom), '.com/', '/ref=') as `category`,
    dom_first_integer(dom, 'span.zg-badge-text', 0) as `rank`,
    dom_first_text(dom, 'div > a > span.a-color-price') as `price`,
    dom_first_text(dom, 'span.zg-item a > div:expr(img=0 && char>30)') as title,
    dom_first_attr(dom, 'span.zg-item div img[src]', 'src') as `pic`,
    str_substring_between(dom_first_attr(dom, 'span.zg-item div a i.a-icon-star', 'class'), ' a-star-', ' ') as score,
    dom_first_text(dom, 'span.zg-item div a:has(i.a-icon-star) ~ a') as starnum
from load_and_select(@url, 'ol#zg-ordered-list > li.zg-item-immersion');
EOF;

var $curl = curl_init($url);
curl_setopt($curl, CURLOPT_RETURNTRANSFER, true);
curl_setopt($curl, CURLOPT_HTTPHEADER, array( 'Content-Type: application/json', 'Content-length:' . strlen($sql)));
curl_setopt($curl, CURLOPT_POST, true);
curl_setopt($curl, CURLOPT_POSTFIELDS, json_encode($sql));
curl_setopt($curl, CURLOPT_VERBOSE, true);

$json_response = curl_exec($curl);
