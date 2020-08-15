#!/bin/bash

targetUrl="https://n.eastday.com/pnews/1597294803022532"
json=$(jq -n \
  --arg targetUrl "$targetUrl" \
  --arg args "-i 1d" \
  '{u: $targetUrl, args: $args}')

host=localhost
curl -H "Accept: application/json" -H "Content-type: application/json" -X POST -d "$json" "http://$host:8182/api/xx/h/article"
