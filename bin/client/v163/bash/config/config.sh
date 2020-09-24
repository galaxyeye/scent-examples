#!/bin/bash

# The host of the API server
export host=119.45.149.30

# Ask the administrator for your username and authToken
export username=gJn6fUBh
export authToken=af1639a924d7232099a037e9544cf43f

# Important!
# Change to your own callback url, we will post the result of x-sql to this url
export callbackUrl="http://localhost:8182/api/hello/echo"

# The example target url for our xsql
export fetchUrl='https://www.amazon.com/Disney-51394-Ariel-Necklace-Set/dp/B00BTX5926/ref=zg_bs_toys-and-games_1?_encoding=UTF8&psc=1&refRID=BX861MPVTN1E6SFC7C2K -i 1s';
