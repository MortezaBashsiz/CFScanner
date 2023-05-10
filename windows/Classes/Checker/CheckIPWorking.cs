using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Net;
using System.Net.NetworkInformation;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using WinCFScan.Classes.Config;
using WinCFScan.Classes.HTTPRequest;

namespace WinCFScan.Classes.Checker
{
    internal class CheckIPWorking
    {
        private readonly string ip;
        private Process? process;
        private string port;
        private string v2rayConfigPath;
        public long downloadDuration { get; private set; }
        public long uploadDuration { get; private set; }
        public long frontingDuration { get; private set; }
        private ScanSpeed dlTargetSpeed;
        private ScanSpeed upTargetSpeed;
        private readonly CustomConfigInfo scanConfig;
        private readonly int checkTimeout;
        public string downloadException = "";
        public string uploadException = "";
        public string frontingException = "";
        private bool isDiagnosing = false;
        public bool isV2rayExecutionSuccess = false;
        public CheckType checkType { get; private set; }
        public FrontingType frontingType { get; private set; }
        private CheckResultStatus checkResultStatus;

        public CheckIPWorking(string ip, ScanSpeed dlTargetSpeed, ScanSpeed upTargetSpeed, CustomConfigInfo scanConfig, 
            CheckType checkType, FrontingType frontingType, int checkTimeout, bool isDiagnosing = false)
        {
            this.ip = ip;
            port = getPortByIP();
            v2rayConfigPath = $"v2ray-config/generated/config.{ip}.json";
            this.dlTargetSpeed = dlTargetSpeed;
            this.upTargetSpeed = upTargetSpeed;
            this.scanConfig = scanConfig;
            this.checkTimeout = checkTimeout;
            this.isDiagnosing = isDiagnosing;
            this.checkType = checkType;
            this.frontingType = frontingType;
            checkResultStatus = new CheckResultStatus(checkType);
        }

        public CheckIPWorking()
        {
        }

        public bool check()
        {
            bool v2rayDLSuccess = false;
            Tools.logStep("\n------------ Start IP Check ------------", isDiagnosing);
            Tools.logStep("IP: " + ip, isDiagnosing);

            // first of all quick test on fronting domain through cloudflare
            bool frontingSuccess = frontingType == FrontingType.YES || isDiagnosing ? checkFronting() : true;

            if (frontingSuccess || isDiagnosing) // on diagnosing we will always test v2ray
            {
                // don't speed test if that mode is selected by user
                if (dlTargetSpeed.isSpeedZero() && !isDiagnosing)
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
                string.Format(Environment.NewLine + "Fronting  Result:    {0}", frontingSuccess ? "SUCCESS" : "FAILED") + Environment.NewLine +
                string.Format("v2ray.exe Execution: {0}", isV2rayExecutionSuccess ? "SUCCESS" : "FAILED") + Environment.NewLine +
                string.Format("Download  Result:    {0}", checkResultStatus.isDownSuccess() ? "SUCCESS" : "FAILED") + Environment.NewLine +
                string.Format("Upload    Result:    {0}", checkResultStatus.isUpSuccess() ? "SUCCESS" : "FAILED"), isDiagnosing
                );

            Tools.logStep("\n------------ End IP Check ------------\n", isDiagnosing);
            return v2rayDLSuccess;

        }

        public bool checkV2ray()
        {
            bool success = false;

            // create config
            if (createV2rayConfigFile())
            {
                // start v2ray.exe process
                if (runV2rayProcess())
                {
                    // send download/upload request
                    if (checkV2raySpeed())
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
                return html.StartsWith("0000000000");
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

        private bool checkV2raySpeed()
        {
            // check download
            if (checkType is CheckType.DOWNLOAD or CheckType.BOTH || isDiagnosing)
            {
                string dlUrl = "https://" + ConfigManager.Instance.getAppConfig().downloadDomain + dlTargetSpeed.getTargetFileSize(checkTimeout);
                var cs = new CheckSettings(ip, port, checkTimeout, dlUrl, isDiagnosing, checkType, dlTargetSpeed);
                var dlChecker = new DownloadChecker(cs);
                if (dlChecker.check())
                {
                    checkResultStatus.setDownloadSuccess();
                    downloadDuration = dlChecker.checkDuration;
                }
                else
                {
                    this.downloadException = dlChecker.exceptionMessage;
                }

            }

            // check upload
            if (checkType is CheckType.UPLOAD or CheckType.BOTH || isDiagnosing){
                string upUrl = "https://" + ConfigManager.Instance.getAppConfig().uploadDomain;
                var cs = new CheckSettings(ip, port, checkTimeout, upUrl, isDiagnosing, checkType, upTargetSpeed);
                var upChecker = new UploadChecker(cs);
                if (upChecker.check())
                {
                    checkResultStatus.setUploadSuccess();
                    uploadDuration = upChecker.checkDuration;
                }
                else
                {
                    this.uploadException = upChecker.exceptionMessage;
                }
            }

            return checkResultStatus.isSuccess();
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
                        .Replace("RANDOMHOST", getRandomSNI(clientConfig.host))
                        .Replace("IP.IP.IP.IP", ip)
                        .Replace("ENDPOINTENDPOINT", clientConfig.path);
                }
                else
                { // just replace port and ip for custom v2ray configs
                    configTemplate = scanConfig.content;
                    configTemplate = configTemplate
                        .Replace("PORTPORT", port)
                        .Replace("IP.IP.IP.IP", ip);
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

        private string getRandomSNI(string host)
        {
            var urlParts = host.Split(".");
            urlParts[0] = Guid.NewGuid().ToString();
            return string.Join(".", urlParts);
        }

        // sum of ip segments plus 3000
        private string getPortByIP()
        {
            int sum = int.Parse(
                ip.Split(".").Aggregate((current, next) =>
                                      (int.Parse(current) + int.Parse(next)).ToString())
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
            //startInfo.Arguments = $"-c \"{v2rayConfigPath}\"";
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
                catch (Exception) { }
            }

            isV2rayExecutionSuccess = wasSuccess;

            return wasSuccess;
        }


    }
}
