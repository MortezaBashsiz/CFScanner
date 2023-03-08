put your custom v2ray configs here but before we can use this configs you must put two variable in your config.
PORTPORT in inbound section like this:

  "inbounds": [{
    "port": "PORTPORT", 
    "listen": "127.0.0.1",
    "tag": "socks-inbound",
    ...

and IP.IP.IP.IP in outbound section like this:
  
   "outbounds": [
    {
    "protocol": "vmess",
    "settings": {
      "vnext": [{
        "address": "IP.IP.IP.IP", 
        "port": 443,
        ...

see sample file in this directory.

find more info here:
https://github.com/MortezaBashsiz/CFScanner/discussions/210