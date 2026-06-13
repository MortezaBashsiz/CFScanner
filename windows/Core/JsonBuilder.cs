using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using WinCFScan.Models;

namespace WinCFScan.Core
{
    internal class JsonBuilder
    {
        public void Generate(ConfigInfo info)
        {
            string templateName = GetTemplate(info);

            string templatePath =
                Path.Combine(
                    AppDomain.CurrentDomain.BaseDirectory,
                    "Templates",
                    templateName
                );

            if (!File.Exists(templatePath))
            {
                throw new Exception(
                    $"Template not found: {templateName}"
                );
            }

            string json =
                File.ReadAllText(templatePath);

            json = ReplaceTokens(json, info);

            string generatedDir =
                Path.Combine(
                    AppDomain.CurrentDomain.BaseDirectory,
                    "generated"
                );

            Directory.CreateDirectory(
                generatedDir
            );

            File.WriteAllText(
                Path.Combine(
                    generatedDir,
                    "generated.json"
                ),
                json
            );

            GenerateClientConfig(
                info,
                generatedDir
            );
        }
        public string GetTemplateName(
    ConfigInfo info)
        {
            if (info.Protocol == "trojan")
                return "trojan_ws_tls.json";

            if (info.Protocol == "vmess")
                return "vmess_ws_tls.json";

            if (info.Protocol == "vless" &&
                info.Network == "grpc")
                return "vless_grpc_tls.json";

            return "vless_ws_tls.json";
        }

        private string GetTemplate(
            ConfigInfo info)
        {
            if (info.Protocol == "trojan")
                return "trojan_ws_tls.json";

            if (info.Protocol == "vmess")
                return "vmess_ws_tls.json";

            if (info.Protocol == "vless" &&
                info.Network == "grpc")
                return "vless_grpc_tls.json";

            return "vless_ws_tls.json";
        }

        private string ReplaceTokens(
            string json,
            ConfigInfo info)
        {
            return json
                .Replace(
                    "{{UUID}}",
                    info.UUID
                )
                .Replace(
                    "{{PASSWORD}}",
                    info.Password
                )
                .Replace(
                    "{{HOST}}",
                    info.Host
                )
                .Replace(
                    "{{SNI}}",
                    info.SNI
                )
                .Replace(
                    "{{PATH}}",
                    info.Path
                )
                .Replace(
                    "{{PORT}}",
                    info.Port.ToString()
                );
        }

        private void GenerateClientConfig(
            ConfigInfo info,
            string outputDir)
        {
            string clientJson =
$@"{{
    ""id"": ""{info.UUID}"",
    ""host"": ""{info.Host}"",
    ""port"": ""{info.Port}"",
    ""path"": ""{info.Path.TrimStart('/')}"",
    ""serverName"": ""{info.SNI}"",
    ""subnetsList"": ""https://raw.githubusercontent.com/MortezaBashsiz/CFScanner/main/config/cf.local.iplist""
}}";

            File.WriteAllText(
                Path.Combine(
                    outputDir,
                    "ClientConfig.json"
                ),
                clientJson
            );
        }

    }
}
