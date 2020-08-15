#!/bin/bash

targetUrl="https://www.amazon.com/gp/most-wished-for/photo/ref=zg_mw_nav_0"
jsonObject=(
  '{"u": "' "$targetUrl" '"'
  ','
  '"args:": "-i 1s"}'
)

json="${jsonObject[*]}"

echo $json
# exit

host=localhost
curl -H "Accept: application/json" -H "Content-type: application/json" -X POST -d "$json" "http://$host:8182/api/xx/h"
