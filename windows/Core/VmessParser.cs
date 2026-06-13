using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using WinCFScan.Models;

namespace WinCFScan.Core
{
    internal class VmessParser
    {

        public ConfigInfo Parse(string link)
        {
            link = link.Replace("vmess://", "");

            string json =
                Encoding.UTF8.GetString(
                    Convert.FromBase64String(link)
                );

            using JsonDocument doc =
                JsonDocument.Parse(json);

            JsonElement root =
                doc.RootElement;

            return new ConfigInfo
            {
                Protocol = "vmess",

                UUID =
                    root.GetProperty("id").GetString() ?? "",

                Host =
                    root.TryGetProperty("host", out var host)
                    ? host.GetString() ?? ""
                    : "",

                SNI =
                    root.TryGetProperty("sni", out var sni)
                    ? sni.GetString() ?? ""
                    : "",

                Path =
                    root.TryGetProperty("path", out var path)
                    ? path.GetString() ?? ""
                    : "",

                Port =
                    int.Parse(
                        root.GetProperty("port")
                            .GetString() ?? "443"
                    ),

                Network =
                    root.TryGetProperty("net", out var net)
                    ? net.GetString() ?? "ws"
                    : "ws",

                Security =
                    root.TryGetProperty("tls", out var tls)
                    ? tls.GetString() ?? "tls"
                    : "tls"
            };
        }

    }
}
