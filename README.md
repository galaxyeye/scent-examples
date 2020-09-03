# X-SQL server client API

The x-sql api is an async api, every call returns the id of the execution immediately.

* you can poll the result using this id
* you have to specify your own callbackUrl, once we have the execution done, we post the result to this url
* requirement for your callback handler: 
  * method: GET
  * media type: application/json

## Startup

    git clone https://github.com/galaxyeye/scent-examples.git
    cd scent-examples

## Configs:
We illustrate the configs in bash and sql files, but they have the similar forms in other languages.

    cat bin/client/v163/config/config.sh
    cat bin/client/v163/config/query.sql

## Bash

Execute a X-SQL and polling the result:

    ./bin/client/v163/xsql-status.sh
    
to try another x-sql, just modify [query.sql](bin/client/v163/config/query.sql)

## PHP

Execute a X-SQL:

    php bin/client/v163/curl.php

## Raw http
A raw http request to execute a X-SQL:

    POST http://localhost:8182/api/x/a/q
    Content-Type: application/json
    
    {
      "username": "gJn6fUBh",
      "authToken": "af1639a924d7232099a037e9544cf43f",
      "sql": "select dom_doc_title(dom) as title from load_and_select('https://www.jd.com', ':root');",
      "callbackUrl": "http://localhost:8182/api/hello/echo"
    }
    
A raw http request to poll the result of a X-SQL:

    GET http://localhost:8182/api/x/a/status?id=156ad198-b603-483a-b651-40ab43817304&username=gJn6fUBh&authToken=af1639a924d7232099a037e9544cf43f
