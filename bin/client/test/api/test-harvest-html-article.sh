#!/bin/bash

targetUrl="https://n.eastday.com/pnews/1597294803022532"
htmlUri=/home/vincent/workspace/platon-all/platon-dev/scent/bin/tools/test/api/resources/eastday-com-1597294803022532.html
htmlContent=$(jq -aRs . <<< $(cat $htmlUri))

json=$(jq -n \
  --arg targetUrl "$targetUrl" \
  --arg htmlContent "$htmlContent" \
  --arg args "-i 1d" \
  '{u: $targetUrl, htmlContent: $htmlContent, args: $args}')

host=platonic.fun
curl -H "Accept: application/json" -H "Content-type: application/json" -X POST -d "$json" "http://$host:8182/api/xx/h/article"
