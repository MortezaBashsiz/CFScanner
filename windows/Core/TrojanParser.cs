using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Web;
using WinCFScan.Models;

namespace WinCFScan.Core
{
    internal class TrojanParser
    {


        public ConfigInfo Parse(string link)
        {
            Uri uri = new Uri(link);

            var query =
                HttpUtility.ParseQueryString(
                    uri.Query
                );

            string host =
                query["host"] ?? "";

            string sni =
                query["sni"] ?? "";

            return new ConfigInfo
            {
                Protocol = "trojan",

                Password =
                    uri.UserInfo,

                Host =
                    host,

                SNI =
                    sni,

                Path =
                    query["path"] ?? "",

                Port =
                    uri.Port,

                Network =
                    query["type"] ?? "ws",

                Security =
                    query["security"] ?? "tls"
            };
        }
    }
}
