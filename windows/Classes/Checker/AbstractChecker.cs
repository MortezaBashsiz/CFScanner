using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Text;
using System.Threading.Tasks;

namespace WinCFScan.Classes.Checker
{
    internal abstract class AbstractChecker
    {
        public HttpClient client;
        public CheckSettings checkSettings { get; }
        public CheckSettings cs { get; } // abbreviated of above checkSettings
        public string exceptionMessage = "";
        public long checkDuration;


        public AbstractChecker(CheckSettings checkSettings)
        {
            this.checkSettings = cs = checkSettings;

            var proxy = new WebProxy();
            proxy.Address = new Uri($"socks5://127.0.0.1:{cs.port}");
            var handler = new HttpClientHandler
            {
                Proxy = proxy
            };

            this.client = new HttpClient(handler);
            this.client.Timeout = TimeSpan.FromSeconds(cs.timeout);            
        }

        public abstract bool check();

        protected void handleException(Exception ex)
        {
            string message = ex.Message;
            if (isTimeoutException(ex))
            {
                Tools.logStep($"{checkSettings.checkType} timed out.", cs.isDiagnosing);
            }
            else
            {
                Tools.logStep($"{checkSettings.checkType} had exception: {message}", cs.isDiagnosing);

                exceptionMessage = message;

                if (ex.InnerException != null && ex.InnerException?.Message != "" && !ex.Message.Contains(ex.InnerException?.Message))
                {
                    Tools.logStep($"Inner exception: {ex.InnerException?.Message}", cs.isDiagnosing);
                }
            }
        }

        private bool isTimeoutException(Exception ex)
        {
            string msg = ex.Message;
            return msg.Contains("The request was aborted") ||
                    msg.Contains("A task was canceled.");
        }
    }

    public enum CheckType
    {
        DOWNLOAD,
        UPLOAD,
        BOTH
    }
    public enum FrontingType
    {
        YES,
        NO
    }

}
