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
        private bool isDiagnosing = false;
        public bool isV2rayExecutionSuccess = false;

        public CheckIPWorking(string ip, ScanSpeed targetSpeed, CustomConfigInfo scanConfig, int downloadTimeout, bool isDiagnosing = false)
        {
            this.ip = ip;
            this.port = getPortByIP();
            v2rayConfigPath = $"v2ray-config/generated/config.{ip}.json";
            this.targetSpeed = targetSpeed;
            this.scanConfig = scanConfig;
            this.downloadTimeout = downloadTimeout;
            this.isDiagnosing = isDiagnosing;
        }

        public CheckIPWorking()
        {
        }

         public bool check()
        {
            bool v2rayDLSuccess = false;
            Tools.logStep("\n------------ Start IP Check ------------", isDiagnosing);
            Tools.logStep("IP: " + this.ip, isDiagnosing);

            // first of all quick test on fronting domain through cloudflare
            bool frontingSuccess = checkFronting();

            if (frontingSuccess || isDiagnosing) // on diagnosing we will always test v2ray
            {
                // don't speed test if that mode is selected by user
                if (targetSpeed.isSpeedZero() && !isDiagnosing)
                {
                    v2rayDLSuccess = true;
                }
                else
                {
                    // then test quality of connection by downloading small file through v2ray vpn
                    v2rayDLSuccess = checkV2ray();
                }

            }

            Tools.logStep(
                string.Format(Environment.NewLine +  "Fronting  Result:    {0}", frontingSuccess ? "SUCCESS" : "FAILED") + Environment.NewLine +
                string.Format("v2ray.exe Execution: {0}", isV2rayExecutionSuccess ? "SUCCESS" : "FAILED") + Environment.NewLine +
                string.Format("Download  Result:    {0}", v2rayDLSuccess ? "SUCCESS" : "FAILED"), isDiagnosing
                );

            Tools.logStep("\n------------ End IP Check ------------\n", isDiagnosing);
            return v2rayDLSuccess;

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
            Tools.logStep(Environment.NewLine + "----- Fronting Test -----", isDiagnosing);

            Tools.logStep($"Start fronting check, timeout: {timeout}, Resolver IP: {ip}, withCustumDNSResolver: {withCustumDNSResolver.ToString()}", isDiagnosing);
            Stopwatch sw = new Stopwatch();
            try
            {
                
                string frUrl = "https://" + ConfigManager.Instance.getAppConfig()?.frontDomain;
                Tools.logStep($"Fronting check with url: {frUrl}", isDiagnosing);
                sw.Start();
                var html = client.GetStringAsync(frUrl).Result;
                Tools.logStep($"Fronting check done in {sw.ElapsedMilliseconds:n0} ms, content: '{html.Substring(0, 50)}'", isDiagnosing);
                frontingDuration = sw.ElapsedMilliseconds;
                return true;
            }
            catch (Exception ex)
            {
                string message = ex.Message;
                if (isTimeoutException(ex))
                {
                    Tools.logStep("Fronting timed out.", isDiagnosing);
                }
                else
                {
                    Tools.logStep($"Fronting check had exception: {message}", isDiagnosing);

                    // monitor exceptions
                    frontingException = message;
                }

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
            Tools.logStep(Environment.NewLine + "----- Download Test -----", isDiagnosing);
            Tools.logStep($"Start check dl speed, proxy port: {port}, timeout: {timeout} sec, target speed: {targetSpeed.getTargetSpeed():n0} b/s", isDiagnosing);
            Stopwatch sw =  new Stopwatch();
            //ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls;

            try
            {
                sw.Start();
                string dlUrl = "https://" + ConfigManager.Instance.getAppConfig().scanDomain + targetSpeed.getTargetFileSize(timeout);
                Tools.logStep($"Starting dl url: {dlUrl}", isDiagnosing);
                var data = client.GetStringAsync(dlUrl).Result;
                Tools.logStep($"*** Download success in {sw.ElapsedMilliseconds:n0} ms, dl size: {data.Length:n0} bytes for IP {ip}", isDiagnosing);

                return data.Length == targetSpeed.getTargetSpeed() * timeout;
            }
            catch (Exception ex)
            {
                string message = ex.Message;
                if (isTimeoutException(ex))
                {
                    Tools.logStep("Download timed out.", isDiagnosing);
                }
                else
                {
                    Tools.logStep($"Download had exception: {message}", isDiagnosing);
                    // monitor exceptions
                    downloadException = message;
                
                    if (ex.InnerException != null && ex.InnerException?.Message != "" &&  ! ex.Message.Contains(ex.InnerException?.Message))
                    {
                        Tools.logStep($"Inner exception: {ex.InnerException?.Message}", isDiagnosing);
                    }
                }

                return false;
            }
            finally
            {
                downloadDuration = sw.ElapsedMilliseconds;
                if(downloadDuration > (timeout * 1000) + 500)
                {
                    Tools.logStep($"Download took too long! {downloadDuration:n0} ms for IP {ip}", isDiagnosing);
                }
                handler.Dispose();
                client.Dispose();
            }
        }
        private bool isTimeoutException(Exception ex)
        {
            string msg = ex.Message;
            return msg.Contains("The request was aborted") ||
                    msg.Contains("A task was canceled.");
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
                Tools.logStep($"createV2rayConfigFile has exception: {ex.Message}", isDiagnosing);
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
            Tools.logStep(Environment.NewLine + "----- Running v2ray.exe -----", isDiagnosing);
            Tools.logStep($"Starting v2ray.exe with arg: {startInfo.Arguments}", isDiagnosing);
            bool wasSuccess = false;
            try
            {
                process = Process.Start(startInfo);
                Thread.Sleep(1500);
                wasSuccess = process.Responding && !process.HasExited;
                Tools.logStep($"v2ray.exe executed success: {wasSuccess}", isDiagnosing);
            }
            catch (Exception ex)
            {
                Tools.logStep($"v2ray.exe execution had exception:  {ex.Message}", isDiagnosing);
            }
            
            // log error
            if (!wasSuccess)
            {
                try
                {
                    string err = process.StandardError.ReadToEnd();
                    string message = $"v2ray.exe Error: {err}";
                    Tools.logStep(message, isDiagnosing);
                    downloadException = message;
                }
                catch (Exception) {}
            }

            isV2rayExecutionSuccess = wasSuccess;

            return wasSuccess;
        }

        
    }
}
