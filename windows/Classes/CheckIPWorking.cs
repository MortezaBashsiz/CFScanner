using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Net;
using System.Text;
using System.Threading.Tasks;
using WinCFScan.Classes.Config;
using WinCFScan.Classes.HTTPRequest;

namespace WinCFScan.Classes
{
    internal class CheckIPWorking
    {
        private readonly string ip;
        private Process? process ;
        private string port;
        private string v2rayConfigPath;
        public long downloadDuration { get; private set; }

        public CheckIPWorking(string ip)
        {
            this.ip = ip;
            this.port = getPortByIP();
            v2rayConfigPath = $"v2ray-config/generated/config.{ip}.json";
        }

        public CheckIPWorking()
        {
        }

        public bool check()
        {
            // first of all quick test on fronting domain through cloudflare
            if(! checkFronting())
                return false;

            // then test quality of connection by downloading small file through v2ray vpn
            return checkV2ray();    
        }

        public bool checkV2ray() {
            bool success = false;
            // create config
            if (createV2rayConfigFile())
            {
                // start v2ray.exe process
                if (runV2rayProcess())
                {
                    // send download request
                    if (checkDownloadSpeed())
                    {
                        // speed was enough
                        success = true;
                    };
                    process.Kill();
                }
            }
            return success;
        }

        private bool checkDownloadSpeed()
        {
            var proxy = new WebProxy();
            proxy.Address = new Uri($"socks5://127.0.0.1:{port}");
            var handler = new HttpClientHandler
            {
                Proxy = proxy
            };

            var client = new HttpClient(handler);
            client.Timeout = TimeSpan.FromSeconds(2); // 2 seconds
            Stopwatch sw =  new Stopwatch();
            try
            {
                sw.Start();
                string dlUrl = "https://" + ConfigManager.Instance.getAppConfig().scanDomain + "/data.100k";
                var data = client.GetStringAsync(dlUrl).Result;
                
                return data.Length > 90_000;
            }
            catch (Exception ex)
            {
                return false;
            }
            finally
            {
                downloadDuration = sw.ElapsedMilliseconds;
                handler.Dispose();
                client.Dispose();
            }
        }

        private bool createV2rayConfigFile()
        {
            try
            {
                var configTemplate = ConfigManager.Instance.v2rayConfigTemplate;
                RealConfig realConfig = ConfigManager.Instance.getRealConfig();

                configTemplate = configTemplate
                    .Replace("IDID", realConfig.id)
                    .Replace("PORTPORT", port)
                    .Replace("HOSTHOST", realConfig.host)
                    .Replace("CFPORTCFPORT", realConfig.port)
                    .Replace("RANDOMHOST", realConfig.serverName)
                    .Replace("IP.IP.IP.IP", this.ip)
                    .Replace("ENDPOINTENDPOINT", realConfig.path);

                File.WriteAllText(v2rayConfigPath, configTemplate);

                return true;
            }
            catch (Exception)
            {
                return false;
            }

        }

        // sum of ip segments plus 3000
        private string getPortByIP()
        {            
            int sum = Int32.Parse(
                this.ip.Split(".").Aggregate((current, next) =>
                                      (Int32.Parse(current) + Int32.Parse(next)).ToString())
                );

            return (3000 + sum).ToString();
        }

        private bool runV2rayProcess()
        {
            ProcessStartInfo startInfo = new ProcessStartInfo();
            startInfo.FileName = "v2ray.exe";
            startInfo.WindowStyle = ProcessWindowStyle.Hidden;
            startInfo.RedirectStandardOutput = true;
            startInfo.RedirectStandardError = true;
            startInfo.UseShellExecute = false;
            startInfo.CreateNoWindow = true;
            startInfo.Arguments = $"run -config=\"{v2rayConfigPath}\"";
            process = Process.Start(startInfo);
            Thread.Sleep(1500);
            return process.Responding && ! process.HasExited;
        }

        public bool checkFronting(bool withCstumDNSResolver = true, int timeout = 1)
        {
            DnsHandler dnsHandler;
            HttpClient client;
            if (withCstumDNSResolver)
            {
                dnsHandler = new DnsHandler(new CustomDnsResolver(ip));
                client = new HttpClient(dnsHandler);
            }
            else
            {
                client = new HttpClient();
            }

            //ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls12;
            client.Timeout = TimeSpan.FromSeconds(timeout);

            try
            {
                string frUrl = "https://" + ConfigManager.Instance.getAppConfig()?.frontDomain;
                var html = client.GetStringAsync(frUrl).Result;

                return true;
            } 
            catch (Exception ex)
            {
                return false;
            }
            finally { 
                client.Dispose();
            }

        }
    }
}
