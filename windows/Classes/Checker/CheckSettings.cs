using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace WinCFScan.Classes.Checker
{
    internal class CheckSettings
    {
        public readonly string ip;
        public readonly string port;
        public readonly int timeout;
        public readonly string targetUrl;
        public readonly bool isDiagnosing;
        public readonly CheckType checkType;
        public readonly ScanSpeed targetSpeed;

        public CheckSettings(string ip, string port, int timeout, string targetUrl, bool isDiagnosing, CheckType checkType, ScanSpeed targetSpeed)
        {
            this.port = port;
            this.ip = ip;
            this.timeout = timeout;
            this.targetUrl = targetUrl;
            this.isDiagnosing = isDiagnosing;
            this.checkType = checkType;
            this.targetSpeed = targetSpeed;
        }
    }
}
