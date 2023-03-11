using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Net;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
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
        public long frontingDuration { get; private set; }
        private ScanSpeed targetSpeed;
        private readonly CustomConfigInfo scanConfig;
        private readonly int downloadTimeout;
        public string downloadException = "";
        public string frontingException = "";

        public CheckIPWorking(string ip, ScanSpeed targetSpeed, CustomConfigInfo scanConfig, int downloadTimeout)
        {
            this.ip = ip;
            this.port = getPortByIP();
            v2rayConfigPath = $"v2ray-config/generated/config.{ip}.json";
            this.targetSpeed = targetSpeed;
            this.scanConfig = scanConfig;
            this.downloadTimeout = downloadTimeout;
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

                    process?.Kill();
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
            Stopwatch sw = new Stopwatch();
            try
            {
                
                string frUrl = "https://" + ConfigManager.Instance.getAppConfig()?.frontDomain;
                Tools.logStep($"Starting fronting check with url: {frUrl}");
                sw.Start();
                var html = client.GetStringAsync(frUrl).Result;
                Tools.logStep($"Fronting check done in {sw.ElapsedMilliseconds:n0} ms, content: '{html.Substring(0, 50)}'");
                frontingDuration = sw.ElapsedMilliseconds;
                return true;
            }
            catch (Exception ex)
            {
                string message = ex.Message;
                Tools.logStep($"Fronting check had exception: {message}");

                // monitor exceptions
                if (!message.Contains("A task was canceled."))
                    frontingException = message;

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

            int timeout = this.downloadTimeout;

            var client = new HttpClient(handler);
            client.Timeout = TimeSpan.FromSeconds(timeout); // 2 seconds
            Tools.logStep($"Start check dl speed, proxy port: {port}, timeout: {timeout} sec, target speed: {targetSpeed.getTargetSpeed():n0} b/s");
            Stopwatch sw =  new Stopwatch();
            //ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls;

            try
            {
                sw.Start();
                string dlUrl = "https://" + ConfigManager.Instance.getAppConfig().scanDomain + targetSpeed.getTargetFileSize(timeout);
                Tools.logStep($"Starting dl url: {dlUrl}");
                var data = client.GetStringAsync(dlUrl).Result;
                Tools.logStep($"*** Download success in {sw.ElapsedMilliseconds:n0} ms, dl size: {data.Length:n0} bytes for IP {ip}");

                return data.Length == targetSpeed.getTargetSpeed() * timeout;
            }
            catch (Exception ex)
            {
                string message = ex.Message;
                Tools.logStep($"dl had exception: {message}");

                // monitor exceptions
                if (!message.Contains("A task was canceled."))
                    downloadException = message;

                if (ex.InnerException != null && ex.InnerException?.Message != "" &&  ! ex.Message.Contains(ex.InnerException?.Message))
                {
                    Tools.logStep($"Inner exception: {ex.InnerException?.Message}");
                }
                return false;
            }
            finally
            {
                downloadDuration = sw.ElapsedMilliseconds;
                if(downloadDuration > (timeout * 1000) + 500)
                {
                    Tools.logStep($"Download took too long! {downloadDuration:n0} ms for IP {ip}");
                }
                handler.Dispose();
                client.Dispose();
            }
        }

        private bool createV2rayConfigFile()
        {
            try
            {
                string configTemplate;

                // using default config or custom v2ray config?
                if (scanConfig.isDefaultConfig())
                {
                    ClientConfig clientConfig = ConfigManager.Instance.getClientConfig();
                    configTemplate = ConfigManager.Instance.v2rayConfigTemplateText;
                    configTemplate = configTemplate
                        .Replace("IDID", clientConfig.id)
                        .Replace("PORTPORT", port)
                        .Replace("HOSTHOST", clientConfig.host)
                        .Replace("CFPORTCFPORT", clientConfig.port)
                        .Replace("RANDOMHOST", clientConfig.serverName)
                        .Replace("IP.IP.IP.IP", this.ip)
                        .Replace("ENDPOINTENDPOINT", clientConfig.path);
                }
                else
                { // just replace port and ip for custom v2ray configs
                    configTemplate = scanConfig.content;
                    configTemplate = configTemplate
                        .Replace("PORTPORT", port)
                        .Replace("IP.IP.IP.IP", this.ip);
                }

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
            //if (!ConfigManager.Instance.enableDebug)
            //{
                startInfo.WindowStyle = ProcessWindowStyle.Hidden;
                startInfo.RedirectStandardOutput = true;
                startInfo.RedirectStandardError = true;
                startInfo.CreateNoWindow = true;
            //}
            startInfo.UseShellExecute = false;
            startInfo.Arguments = $"run -config=\"{v2rayConfigPath}\"";
            Tools.logStep($"Starting v2ray.exe with arg: {startInfo.Arguments}");
            bool wasSuccess = false;
            try
            {
                process = Process.Start(startInfo);
                Thread.Sleep(1500);
                wasSuccess = process.Responding && !process.HasExited;
                Tools.logStep($"v2ray.exe executed success:  {wasSuccess}");
            }
            catch (Exception ex)
            {
                Tools.logStep($"v2ray.exe execution had exception:  {ex.Message}");
            }
            
            // log error
            if (!wasSuccess)
            {
                try
                {
                    string err = process.StandardError.ReadToEnd();
                    Tools.logStep($"v2ray.exe Error: {err}");
                }
                catch (Exception) {}
            }

            return wasSuccess;
        }

        
    }
}
