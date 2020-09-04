# X-SQL client API

The x-sql query api is asynchronous, every query returns the id of the query immediately.

* you CAN poll the result using this id, but it's optional
* you MUST implement your callback url to receive the result, once we have the query done, we post the result back
* your callback url have to:
  * accept http method: GET
  * accept http media type: application/json
  * see [Raw http](#Raw http) section for detail

## Startup

    git clone https://github.com/galaxyeye/scent-examples.git
    cd scent-examples

## Configs
We show the config properties in bash and sql files, but they have the similar forms in other languages

    cat bin/client/v163/bash/config/config.sh
    cat bin/client/v163/bash/config/query.sql

## Bash

Execute an X-SQL and poll the result:

    ./bin/client/v163/bash/xsql-status.sh
    
to try another x-sql, just modify [query.sql](bin/client/v163/bash/config/query.sql)

## PHP

Execute an X-SQL:

    php bin/client/v163/php/curl.php

## Raw http
A raw http request to execute an X-SQL:

    POST http://localhost:8182/api/x/a/q
    Content-Type: application/json
    
    {
      "username": "gJn6fUBh",
      "authToken": "af1639a924d7232099a037e9544cf43f",
      "sql": "select dom_doc_title(dom) as title from load_and_select('https://www.jd.com', ':root');",
      "callbackUrl": "http://localhost:8182/api/hello/echo"
    }
    
A raw http request to poll the result of an X-SQL:

    GET http://localhost:8182/api/x/a/status?id=156ad198-b603-483a-b651-40ab43817304&username=gJn6fUBh&authToken=af1639a924d7232099a037e9544cf43f

A raw http request to post to your callback api:

    POST http://{{host-of-your-callback-api}}/{{path-of-your-callback-api}}
    Content-Type: application/json
    
    [
        {
            "title": "京东(JD.COM)-正品低价、品质保障、配送及时、轻松购物！",
            "uri":"https://www.jd.com/"}
        }
    ]
