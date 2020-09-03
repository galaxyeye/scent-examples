#!/bin/bash

this="${BASH_SOURCE-$0}"
basedir=$(dirname "$this")
source "$basedir/config/config.sh"

uuid=$(exec "$basedir/xsql.sh")

# change to your own pulsar server
url="http://$host:8182/api/x/a/status?id=$uuid&username=$username&authToken=$authToken"

while true; do
  result=$(curl -H "Accept: application/json" -X GET "$url")
  echo "$result"

  if [[ $result != *"Not Found"* ]]; then
    exit 0
  fi
  sleep 5s
done
