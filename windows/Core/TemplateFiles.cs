using Microsoft.VisualBasic.Logging;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace WinCFScan.Core
{
    public static class TemplateFiles
    {
        public static string trojan_ws_tls => @"{
  ""inbounds"": [{
    ""port"": ""PORTPORT"", 
    ""listen"": ""127.0.0.1"",
    ""tag"": ""socks-inbound"",
    ""protocol"": ""socks"",
    ""settings"": {
      ""auth"": ""noauth"",
      ""udp"": false,
      ""ip"": ""127.0.0.1""
    },
    ""sniffing"": {
      ""enabled"": true,
      ""destOverride"": [""http"", ""tls""]
    }
  }],
  ""outbounds"": [
    {
      ""tag"": ""proxy"",
      ""protocol"": ""trojan"",
      ""settings"": {
        ""servers"": [
          {
            ""address"": ""IP.IP.IP.IP"",
            ""method"": ""chacha20"",
            ""ota"": false,
            ""password"": ""{{PASSWORD}}"",
            ""port"": {{PORT}},
            ""level"": 1,
            ""flow"": """"
          }
        ]
      },
      ""streamSettings"": {
        ""network"": ""ws"",
        ""security"": ""tls"",
        ""tlsSettings"": {
          ""allowInsecure"": false,
          ""serverName"": ""{{SNI}}"",
          ""alpn"": [
            ""http/1.1""
          ],
          ""fingerprint"": ""chrome""
        },
        ""wsSettings"": {
          ""path"": ""{{PATH}}"",
          ""headers"": {
            ""Host"": ""{{HOST}}""
          }
        }
      },
      ""mux"": {
        ""enabled"": false,
        ""concurrency"": -1
      }
    }
  ],
  ""other"": {}
}";

        public static string vmess_ws_tls => @"{
  ""inbounds"": [{
    ""port"": ""PORTPORT"", 
    ""listen"": ""127.0.0.1"",
    ""tag"": ""socks-inbound"",
    ""protocol"": ""socks"",
    ""settings"": {
      ""auth"": ""noauth"",
      ""udp"": false,
      ""ip"": ""127.0.0.1""
    },
    ""sniffing"": {
      ""enabled"": true,
      ""destOverride"": [""http"", ""tls""]
    }
  }],
  ""outbounds"": [
    {
    ""protocol"": ""vmess"",
    ""settings"": {
      ""vnext"": [{
        ""address"": ""IP.IP.IP.IP"", 
        ""port"":  {{PORT}},
        ""users"": [{""id"": ""{{UUID}}"" }]
      }]
    },
		""streamSettings"": {
        ""network"": ""ws"",
        ""security"": ""tls"",
        ""wsSettings"": {
            ""headers"": {
                ""Host"": ""{{HOST}}""
            },
            ""path"": ""{{PATH}}""
        },
        ""tlsSettings"": {
            ""serverName"": ""{{SNI}}"",
            ""allowInsecure"": false,
			""fingerprint"": ""chrome"",
			""alpn"": [
			""http/1.1""
			]
        }
    }
	}],
  ""other"": {}
}";

        public static string vless_grpc_tls => @"{
  ""inbounds"": [{
    ""port"": ""PORTPORT"", 
    ""listen"": ""127.0.0.1"",
    ""tag"": ""socks-inbound"",
    ""protocol"": ""socks"",
    ""settings"": {
      ""auth"": ""noauth"",
      ""udp"": false,
      ""ip"": ""127.0.0.1""
    },
    ""sniffing"": {
      ""enabled"": true,
      ""destOverride"": [""http"", ""tls""]
    }
  }],
  ""outbounds"": [
    {
    ""protocol"": ""vless"",
    ""settings"": {
      ""vnext"": [{
        ""address"": ""IP.IP.IP.IP"", 
        ""port"": {{PORT}},
        ""users"": [{""id"": ""{{UUID}}"",
		""encryption"": ""none""
			}]
      }]
    },
		""streamSettings"": {
        ""network"": ""grpc"",
        ""security"": ""tls"",
        ""tlsSettings"": {
          ""allowInsecure"": false,
          ""serverName"": ""{{SNI}}"",
          ""alpn"": [
            ""http/1.1""
          ],
          ""fingerprint"": ""chrome""
        },
        ""grpcSettings"": {
          ""serviceName"": """",
          ""multiMode"": false
        }
      }
	}],
  ""other"": {}
}";
        public static string Shadowsocks => @"{
              ""name"": ""ss"",
              ""type"": ""shadowsocks"",
              ""enabled"": true
                                }";

        public static string vless_ws_tls => @"{
""inbounds"": [{
    ""port"": ""PORTPORT"", 
    ""listen"": ""127.0.0.1"",
    ""tag"": ""socks-inbound"",
    ""protocol"": ""socks"",
    ""settings"": {
      ""auth"": ""noauth"",
      ""udp"": false,
      ""ip"": ""127.0.0.1""
    },
    ""sniffing"": {
      ""enabled"": true,
      ""destOverride"": [""http"", ""tls""]
    }
  }],
  ""outbounds"": [
    {
      ""tag"": ""proxy"",
      ""protocol"": ""vless"",
      ""settings"": {
        ""vnext"": [{
        ""address"": ""IP.IP.IP.IP"", 
        ""port"": {{PORT}},
        ""users"": [{""id"": ""{{UUID}}"",
		""encryption"": ""none""
			}]
      }]
      },
      ""streamSettings"": {
        ""network"": ""ws"",
        ""security"": ""tls"",
        ""tlsSettings"": {
          ""allowInsecure"": false,
          ""serverName"": ""{{SNI}}"",
          ""alpn"": [
            ""http/1.1""
          ],
          ""fingerprint"": ""chrome""
        },
        ""wsSettings"": {
          ""path"": ""{{PATH}}"",
          ""headers"": {
            ""Host"": ""{{HOST}}""
          }
        }
      }
    }
  ],
	""other"": {}
}";
        public static void WriteTemplate(string fileName, string content,Action<string> log)
        {
            string basePath = AppDomain.CurrentDomain.BaseDirectory;
            string folder = Path.Combine(basePath, "Templates");

            if (!Directory.Exists(folder))
                Directory.CreateDirectory(folder);

            string path = Path.Combine(folder, fileName);

            if (!File.Exists(path))
            {
                File.WriteAllText(path, content, Encoding.UTF8);
                log?.Invoke($"{fileName} Created");
            }
            else
            {
                log?.Invoke($"{fileName} Already Exists");
            }
        }

    }
}
