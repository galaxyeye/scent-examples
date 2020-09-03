# X-SQL server client API

    git clone https://github.com/galaxyeye/scent-examples.git
    cd scent-examples

## bash

Execute a X-SQL and polling the result:

    ./bin/client/v163/xsql-status.sh
    
to try another x-sql, just modify [query.sql](bin/client/v163/config/query.sql)

## php

Execute a X-SQL:

    php bin/client/v163/curl.php

## raw http
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
