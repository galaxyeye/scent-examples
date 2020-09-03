#!/bin/bash

basedir=$(dirname "${BASH_SOURCE[0]}")
source "$basedir/config/config.sh"

sql=$(cat "$basedir/config/query.sql")

# remove control characters
sql=$(echo "$sql" | tr -s "[:cntrl:]" " ")
# replace @url by the actual target url
sql=${sql/@url/\'$xsqlTargetUrl\'}
# build the json data to send
json="{\"username\": \"$username\", \"authToken\": \"$authToken\", \"sql\": \"$sql\", \"callbackUrl\": \"$callbackUrl\"}"

# echo $json

curl -H "Accept: application/json" -H "Content-type: application/json" -X POST -d "$json" "http://$host:8182/api/x/a/q"
