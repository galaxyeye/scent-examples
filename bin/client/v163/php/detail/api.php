<?php

/**
 *The x-sql api is an async api, every call returns the id of the execution immediately.
 *
 * * you can poll the result using this id
 * * you have to specify your own callbackUrl, once we have the execution done, we post the result to this url
 *   requirement for your callback handler:
 *   * method: GET
 *   * media type: application/json
 *
 * @param $url string
 * @param $sql string
 * @return bool|string
 */
function submit($url, $sql) {
    $sql = array(
        "authToken" => "gJn6fUBh-1-af1639a924d7232099a037e9544cf43f",
        "sql" => $sql,
        "callbackUrl" => "http://localhost:8182/api/hello/echo"
    );
    $json = json_encode($sql);

    $curl = curl_init($url);
    curl_setopt($curl, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($curl, CURLOPT_POST, true);
    curl_setopt($curl, CURLOPT_POSTFIELDS, $json);
    curl_setopt($curl, CURLOPT_VERBOSE, true);

    curl_setopt($curl, CURLOPT_HTTPHEADER, array( 'Content-Type: application/json', 'Content-length:' . strlen($json))); // check if the length be correct
    return curl_exec($curl);
}

/**
 * @param $url
 * @return mixed
 */
function query($url) {
    $headerArray =array("Content-type: application/json");
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, FALSE);
    curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, FALSE);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
    curl_setopt($ch,CURLOPT_HTTPHEADER, $headerArray);
    $output = curl_exec($ch);
    curl_close($ch);

    return json_decode($output, true);
}
