using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Web;
using WinCFScan.Models;

namespace WinCFScan.Core
{
    internal class ConfigParser
    {
        private readonly VmessParser vmessParser =
         new VmessParser();
        private readonly TrojanParser trojanParser =
    new TrojanParser();
        public ConfigInfo ParseVless(string link)
        {
            var uri = new Uri(link);

            var query = HttpUtility.ParseQueryString(
                uri.Query
            );

            string host =
                query["host"] ?? "";

            string sni =
                query["sni"] ?? "";

            host = host
                .Replace("https://", "")
                .Replace("http://", "")
                .Trim('/');

            sni = sni
                .Replace("https://", "")
                .Replace("http://", "")
                .Trim('/');

            return new ConfigInfo
            {
                UUID =
                    uri.UserInfo,

                Host =
                    host,

                SNI =
                    sni,

                Path =
                    query["path"] ?? "",

                Port =
                    uri.Port,

                Protocol =
                    "vless"
            };

        }

        public ConfigInfo Parse(string link)
        {
            if (link.StartsWith("vless://"))
                return ParseVless(link);

            if (link.StartsWith("vmess://"))
                return vmessParser.Parse(link);
            if (link.StartsWith("trojan://"))
            {
                return trojanParser.Parse(link);
            }

            throw new Exception(
                "Unsupported protocol."
            );
        }

    }
}
