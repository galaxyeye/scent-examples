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

require_once "detail/api.php";

$fetchUrl='https://www.amazon.com/Disney-51394-Ariel-Necklace-Set/dp/B00BTX5926/ref=zg_bs_toys-and-games_1?_encoding=UTF8&psc=1&refRID=BX861MPVTN1E6SFC7C2K';

$sql = file_get_contents(dirname(__FILE__)."/../config/query.sql");
$sql = str_replace("@url", "'" . $fetchUrl . "'", $sql);

$submitUrl = 'http://119.45.149.30:8182/api/x/a/q';
$uuid = submit($submitUrl, $sql);
# $url = 'http://localhost:8182/api/x/a/q';

$queryUrl = "http://119.45.149.30:8182/api/x/a/status?id=$uuid&username=gJn6fUBh&authToken=af1639a924d7232099a037e9544cf43f";
$output = query($queryUrl);
while ($output['statusCode'] != 200) {
    print_r($output);
    sleep(3);
    $output = query($queryUrl);
}

print_r($output);
