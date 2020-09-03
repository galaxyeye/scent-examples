<?php

$fetchUrl='https://www.amazon.com/Best-Sellers-Toys-Games/zgbs/toys-and-games/ref=zg_bs_toys-and-games_home_all?pf_rd_p=089b8285-7691-4849-a7f5-b2fca56bf24a&pf_rd_s=center-1&pf_rd_t=2101&pf_rd_i=home&pf_rd_m=ATVPDKIKX0DER&pf_rd_r=7Z41QZGQ4X56MJK1CJV0&pf_rd_r=7Z41QZGQ4X56MJK1CJV0&pf_rd_p=089b8285-7691-4849-a7f5-b2fca56bf24a';
$sql=<<<EOF
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
from load_and_select('$fetchUrl', 'ol#zg-ordered-list > li.zg-item-immersion');
EOF;

$sql=array(
    "username" => "gJn6fUBh",
    "authToken" => "af1639a924d7232099a037e9544cf43f",
    "sql" => $sql,
    "callbackUrl" => "http://localhost:8182/api/hello/echo"
);
$json = json_encode($sql);

# $url = 'http://119.45.149.30:8182/api/x/a/q';
$url = 'http://localhost:8182/api/x/a/q';
$curl = curl_init($url);
curl_setopt($curl, CURLOPT_RETURNTRANSFER, true);
curl_setopt($curl, CURLOPT_HTTPHEADER, array( 'Content-Type: application/json', 'Content-length:' . strlen($json))); // check if the length be correct
curl_setopt($curl, CURLOPT_POST, true);
curl_setopt($curl, CURLOPT_POSTFIELDS, $json);
curl_setopt($curl, CURLOPT_VERBOSE, true);

$json_response = curl_exec($curl);
print $json_response;