#!/usr/bin/env bash

echo "Do not run this script directly, run the specified commands alone in this script"

curl -H "Accept: application/json" -H "Content-type: application/json" -X POST -d "{\"sql\":\"select dom_base_uri(dom) as url from load_and_select('https://www.jd.com', ':root')\"}" http://119.45.149.30:8182/api/x/sql/json
