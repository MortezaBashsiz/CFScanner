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
            bool success = false;
            Tools.logStep("\n ------- Start IP Check ------- ");
            
            // first of all quick test on fronting domain through cloudflare
            if(checkFronting())
            {
                // then test quality of connection by downloading small file through v2ray vpn
                success = checkV2ray();

            }

            Tools.logStep("\n------- End IP Check -------\n");
            return success;

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

        public bool checkFronting(bool withCustumDNSResolver = true, int timeout = 1)
        {
            DnsHandler dnsHandler;
            HttpClient client;
            if (withCustumDNSResolver)
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

            Tools.logStep($"Start fronting check, timeout: {timeout}, Resolver IP: {ip}, withCustumDNSResolver: {withCustumDNSResolver.ToString()}");

            try
            {
                Stopwatch sw = new Stopwatch();
                string frUrl = "https://" + ConfigManager.Instance.getAppConfig()?.frontDomain;
                Tools.logStep($"Starting fronting check with url: {frUrl}");
                var html = client.GetStringAsync(frUrl).Result;
                Tools.logStep($"Fronting check done in {sw.ElapsedMilliseconds:n0} ms, content: '{html.Substring(0, 50)}'");
                return true;
            }
            catch (Exception ex)
            {
                Tools.logStep($"Fronting check had exception: {ex.Message}");
                return false;
            }
            finally
            {
                client.Dispose();
            }

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
            Tools.logStep($"Start check dl speed, proxy port: {port}, timeout: {client.Timeout.TotalSeconds} sec");
            Stopwatch sw =  new Stopwatch();
            try
            {
                sw.Start();
                string dlUrl = "https://" + ConfigManager.Instance.getAppConfig().scanDomain + "100000";
                Tools.logStep($"Starting dl url: {dlUrl}");
                var data = client.GetStringAsync(dlUrl).Result;
                Tools.logStep($"*** Download success in {sw.ElapsedMilliseconds:n0} ms, dl size: {data.Length:n0} bytes for IP {ip}");

                return data.Length > 90_000;
            }
            catch (Exception ex)
            {
                Tools.logStep($"dl had exception: {ex.Message}");
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
                ClientConfig clientConfig = ConfigManager.Instance.getClientConfig();

                configTemplate = configTemplate
                    .Replace("IDID", clientConfig.id)
                    .Replace("PORTPORT", port)
                    .Replace("HOSTHOST", clientConfig.host)
                    .Replace("CFPORTCFPORT", clientConfig.port)
                    .Replace("RANDOMHOST", clientConfig.serverName)
                    .Replace("IP.IP.IP.IP", this.ip)
                    .Replace("ENDPOINTENDPOINT", clientConfig.path);

                File.WriteAllText(v2rayConfigPath, configTemplate);

                return true;
            }
            catch (Exception ex)
            {
                Tools.logStep($"createV2rayConfigFile has exception: {ex.Message}");
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
            Tools.logStep($"Starting v2ray.exe with arg: {startInfo.Arguments}");
            process = Process.Start(startInfo);
            Thread.Sleep(1500);
            bool wasSuccess = process.Responding && !process.HasExited;
            Tools.logStep($"v2ray.exe executed success:  {wasSuccess}");

            // log error
            if (!wasSuccess)
            {
                Tools.logStep($"v2ray.exe Error: {process.StandardError.ReadToEnd()}");
            }

            return wasSuccess;
        }

        
    }
}
