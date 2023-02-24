using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using WinCFScan.Classes.Config;

namespace WinCFScan.Classes
{
    internal class ScanProgressInfo
    {
        public bool isScanRunning = false;
        public string currentIPRange = "";
        public string lastErrMessage = "";
        public bool hasError = false;
        public bool stopRequested = false; // is requested to stop scan
        internal string lastCheckedIP;
        internal ScanResults scanResults;
        internal bool skipCurrentIPRange;
        internal int totalIPRanges;
        internal int currentIPRangesNumber = 0;
        internal int currentIPRangeTotalIPs = 0;
        internal int totalCheckedIPInCurIPRange = 0;
        internal int totalCheckedIP = 0;
    }
}
