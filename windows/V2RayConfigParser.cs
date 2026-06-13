using System;
using System.Collections.Generic;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;
using System.Text.RegularExpressions;

namespace WinCFScan.Classes.Config
{
    /// <summary>
    /// Parsed info extracted from a v2ray share link (vmess/vless/trojan)
    /// </summary>
    public class V2RayLinkInfo
    {
        public string Protocol { get; set; } = "";   // vmess / vless / trojan
        public string Address { get; set; } = "";    // server address (SNI/domain)
        public int Port { get; set; } = 443;
        public string UUID { get; set; } = "";       // uuid / password for trojan
        public string Path { get; set; } = "/";
        public string Host { get; set; } = "";       // ws Host header
        public string SNI { get; set; } = "";        // TLS serverName
        public string Network { get; set; } = "ws";  // ws / grpc / tcp
        public string Security { get; set; } = "tls";
        public string ServiceName { get; set; } = ""; // grpc service name
        public string Encryption { get; set; } = "none"; // vless encryption
        public bool AllowInsecure { get; set; } = false;

        /// <summary>
        /// The domain to use for TLS ping — SNI first, then Host, then Address
        /// </summary>
        public string TlsPingDomain => !string.IsNullOrWhiteSpace(SNI) ? SNI
                                     : !string.IsNullOrWhiteSpace(Host) ? Host
                                     : Address;

        public bool IsValid =>
            !string.IsNullOrWhiteSpace(Protocol) &&
            !string.IsNullOrWhiteSpace(Address) &&
            (!string.IsNullOrWhiteSpace(UUID) || Protocol == "trojan");
    }

    public static class V2RayConfigParser
    {
        // ────────────────────────────────────────────────────────────────────
        // Public entry point
        // ────────────────────────────────────────────────────────────────────

        /// <summary>
        /// Parse any supported share link.  Returns null + sets errorMessage on failure.
        /// Supported: vmess://, vless://, trojan://
        /// </summary>
        public static V2RayLinkInfo? Parse(string link, out string errorMessage)
        {
            errorMessage = "";
            if (string.IsNullOrWhiteSpace(link))
            {
                errorMessage = "لینک خالی است.";
                return null;
            }

            link = link.Trim();

            if (link.StartsWith("vmess://", StringComparison.OrdinalIgnoreCase))
                return ParseVmess(link, out errorMessage);

            if (link.StartsWith("vless://", StringComparison.OrdinalIgnoreCase))
                return ParseVless(link, out errorMessage);

            if (link.StartsWith("trojan://", StringComparison.OrdinalIgnoreCase))
                return ParseTrojan(link, out errorMessage);

            errorMessage = "پروتکل پشتیبانی نمی‌شود. لینک باید با vmess:// یا vless:// یا trojan:// شروع شود.";
            return null;
        }

        // ────────────────────────────────────────────────────────────────────
        // vmess://  (base64-encoded JSON)
        // ────────────────────────────────────────────────────────────────────

        private static V2RayLinkInfo? ParseVmess(string link, out string errorMessage)
        {
            errorMessage = "";
            try
            {
                string b64 = link.Substring("vmess://".Length).Trim();
                // add padding if needed
                b64 = b64.PadRight(b64.Length + (4 - b64.Length % 4) % 4, '=');
                string json = Encoding.UTF8.GetString(Convert.FromBase64String(b64));

                var doc = JsonNode.Parse(json)!;

                var info = new V2RayLinkInfo { Protocol = "vmess" };
                info.Address  = doc["add"]?.ToString()  ?? doc["address"]?.ToString() ?? "";
                info.Port     = int.TryParse(doc["port"]?.ToString(), out int p) ? p : 443;
                info.UUID     = doc["id"]?.ToString()   ?? "";
                info.Path     = doc["path"]?.ToString() ?? "/";
                info.Host     = doc["host"]?.ToString() ?? "";
                info.SNI      = doc["sni"]?.ToString()  ?? doc["host"]?.ToString() ?? "";
                info.Network  = doc["net"]?.ToString()  ?? doc["network"]?.ToString() ?? "ws";
                info.Security = doc["tls"]?.ToString()  ?? "tls";
                info.ServiceName = doc["type"]?.ToString() ?? ""; // grpc serviceName sometimes here

                if (string.IsNullOrWhiteSpace(info.Address))
                {
                    errorMessage = "آدرس سرور در لینک vmess پیدا نشد.";
                    return null;
                }
                return info;
            }
            catch (Exception ex)
            {
                errorMessage = $"خطا در parse کردن لینک vmess: {ex.Message}";
                return null;
            }
        }

        // ────────────────────────────────────────────────────────────────────
        // vless://  (URI format: vless://uuid@host:port?params#name)
        // ────────────────────────────────────────────────────────────────────

        private static V2RayLinkInfo? ParseVless(string link, out string errorMessage)
            => ParseUriFormat(link, "vless", out errorMessage);

        // ────────────────────────────────────────────────────────────────────
        // trojan:// (URI format: trojan://password@host:port?params#name)
        // ────────────────────────────────────────────────────────────────────

        private static V2RayLinkInfo? ParseTrojan(string link, out string errorMessage)
            => ParseUriFormat(link, "trojan", out errorMessage);

        // ────────────────────────────────────────────────────────────────────
        // Shared URI-format parser (vless & trojan share the same structure)
        // ────────────────────────────────────────────────────────────────────

        private static V2RayLinkInfo? ParseUriFormat(string link, string proto, out string errorMessage)
        {
            errorMessage = "";
            try
            {
                // strip fragment (#name)
                int hashIdx = link.IndexOf('#');
                if (hashIdx >= 0) link = link.Substring(0, hashIdx);

                // split query string
                int qIdx = link.IndexOf('?');
                string queryString = qIdx >= 0 ? link.Substring(qIdx + 1) : "";
                string core = qIdx >= 0 ? link.Substring(0, qIdx) : link;

                // core: vless://uuid@host:port
                string body = core.Substring((proto + "://").Length);
                int atIdx = body.LastIndexOf('@');
                if (atIdx < 0) { errorMessage = "فرمت لینک نادرست است (@ یافت نشد)."; return null; }

                string uuid = Uri.UnescapeDataString(body.Substring(0, atIdx));
                string hostPort = body.Substring(atIdx + 1);

                // handle IPv6  [::1]:443
                string host; int port = 443;
                if (hostPort.StartsWith("["))
                {
                    int bracketEnd = hostPort.IndexOf(']');
                    host = hostPort.Substring(1, bracketEnd - 1);
                    var afterBracket = hostPort.Substring(bracketEnd + 1);
                    if (afterBracket.StartsWith(":")) int.TryParse(afterBracket.Substring(1), out port);
                }
                else
                {
                    int colonIdx = hostPort.LastIndexOf(':');
                    if (colonIdx >= 0)
                    {
                        host = hostPort.Substring(0, colonIdx);
                        int.TryParse(hostPort.Substring(colonIdx + 1), out port);
                    }
                    else host = hostPort;
                }

                var info = new V2RayLinkInfo { Protocol = proto };
                info.UUID    = uuid;
                info.Address = host;
                info.Port    = port;

                // parse query parameters
                var qp = ParseQueryString(queryString);
                info.Network    = qp.GetValueOrDefault("type", "ws");
                info.Security   = qp.GetValueOrDefault("security", "tls");
                info.SNI        = Uri.UnescapeDataString(qp.GetValueOrDefault("sni", host));
                info.Host       = Uri.UnescapeDataString(qp.GetValueOrDefault("host", ""));
                info.Path       = Uri.UnescapeDataString(qp.GetValueOrDefault("path", "/"));
                info.ServiceName= Uri.UnescapeDataString(qp.GetValueOrDefault("serviceName", ""));
                info.Encryption = qp.GetValueOrDefault("encryption", "none");
                info.AllowInsecure = qp.GetValueOrDefault("allowInsecure", "0") == "1";

                if (string.IsNullOrWhiteSpace(info.Address))
                {
                    errorMessage = "آدرس سرور در لینک پیدا نشد.";
                    return null;
                }
                return info;
            }
            catch (Exception ex)
            {
                errorMessage = $"خطا در parse کردن لینک {proto}: {ex.Message}";
                return null;
            }
        }

        private static Dictionary<string, string> ParseQueryString(string query)
        {
            var dict = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
            if (string.IsNullOrWhiteSpace(query)) return dict;
            foreach (var part in query.Split('&'))
            {
                int eq = part.IndexOf('=');
                if (eq > 0)
                    dict[part.Substring(0, eq)] = part.Substring(eq + 1);
            }
            return dict;
        }

        // ────────────────────────────────────────────────────────────────────
        // JSON template builders
        // ────────────────────────────────────────────────────────────────────

        /// <summary>
        /// Build a WinCFScan-compatible JSON config string from parsed link info.
        /// Placeholders PORTPORT and IP.IP.IP.IP are inserted as required by the scanner.
        /// </summary>
        public static string BuildJsonConfig(V2RayLinkInfo info, out string errorMessage)
        {
            errorMessage = "";

            // resolve effective host / sni
            string sni  = !string.IsNullOrWhiteSpace(info.SNI)  ? info.SNI  : info.Address;
            string host = !string.IsNullOrWhiteSpace(info.Host) ? info.Host : sni;
            string path = string.IsNullOrWhiteSpace(info.Path)  ? "/"        : info.Path;
            bool isTls  = info.Security.Equals("tls", StringComparison.OrdinalIgnoreCase) ||
                          info.Security.Equals("xtls", StringComparison.OrdinalIgnoreCase);

            string securityBlock = isTls
                ? $@"""security"": ""{info.Security}"",
        ""tlsSettings"": {{
            ""allowInsecure"": {(info.AllowInsecure ? "true" : "false")},
            ""serverName"": ""{EscJson(sni)}"",
            ""alpn"": [""http/1.1""],
            ""fingerprint"": ""chrome""
        }}"
                : @"""security"": ""none""";

            string streamBlock;
            switch (info.Network.ToLower())
            {
                case "grpc":
                    streamBlock = $@"""streamSettings"": {{
        ""network"": ""grpc"",
        {securityBlock},
        ""grpcSettings"": {{
            ""serviceName"": ""{EscJson(info.ServiceName)}"",
            ""multiMode"": false
        }}
    }}";
                    break;

                case "tcp":
                    streamBlock = $@"""streamSettings"": {{
        ""network"": ""tcp"",
        {securityBlock}
    }}";
                    break;

                default: // ws
                    streamBlock = $@"""streamSettings"": {{
        ""network"": ""ws"",
        {securityBlock},
        ""wsSettings"": {{
            ""path"": ""{EscJson(path)}"",
            ""headers"": {{
                ""Host"": ""{EscJson(host)}""
            }}
        }}
    }}";
                    break;
            }

            string outboundSettings;
            switch (info.Protocol)
            {
                case "vmess":
                    outboundSettings = $@"""protocol"": ""vmess"",
    ""settings"": {{
        ""vnext"": [{{
            ""address"": ""IP.IP.IP.IP"",
            ""port"": {info.Port},
            ""users"": [{{""id"": ""{EscJson(info.UUID)}""}}]
        }}]
    }}";
                    break;

                case "vless":
                    outboundSettings = $@"""protocol"": ""vless"",
    ""settings"": {{
        ""vnext"": [{{
            ""address"": ""IP.IP.IP.IP"",
            ""port"": {info.Port},
            ""users"": [{{""id"": ""{EscJson(info.UUID)}"", ""encryption"": ""{EscJson(info.Encryption)}""}}]
        }}]
    }}";
                    break;

                case "trojan":
                    outboundSettings = $@"""protocol"": ""trojan"",
    ""settings"": {{
        ""servers"": [{{
            ""address"": ""IP.IP.IP.IP"",
            ""port"": {info.Port},
            ""password"": ""{EscJson(info.UUID)}"",
            ""ota"": false,
            ""level"": 1,
            ""flow"": """"
        }}]
    }}";
                    break;

                default:
                    errorMessage = $"پروتکل '{info.Protocol}' پشتیبانی نمی‌شود.";
                    return "";
            }

            return $@"{{
    ""inbounds"": [{{
        ""port"": ""PORTPORT"",
        ""listen"": ""127.0.0.1"",
        ""tag"": ""socks-inbound"",
        ""protocol"": ""socks"",
        ""settings"": {{
            ""auth"": ""noauth"",
            ""udp"": false,
            ""ip"": ""127.0.0.1""
        }},
        ""sniffing"": {{
            ""enabled"": true,
            ""destOverride"": [""http"", ""tls""]
        }}
    }}],
    ""outbounds"": [{{
        {outboundSettings},
        {streamBlock}
    }}],
    ""other"": {{}}
}}";
        }

        private static string EscJson(string s) => s.Replace("\\", "\\\\").Replace("\"", "\\\"");
    }
}
