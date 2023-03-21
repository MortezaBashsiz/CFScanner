using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Net;
using System.Text;
using System.Threading.Tasks;
using WinCFScan.Classes.Config;
using WinCFScan.Classes.HTTPRequest;
using WinCFScan.Classes.IP;

namespace WinCFScan.Classes
{
    internal class ScanEngine
    {
        protected string errMessage = "";
        protected bool hasError = false;
        public CFIPList ipListLoader;

        private string[] cfIPRangeList;
        public ScanProgressInfo progressInfo { get; protected set; }
        private CancellationTokenSource cts;
        public List<ResultItem>? workingIPsFromPrevScan { get; set; }
        public int concurrentProcess = 4;
        public ScanSpeed targetSpeed;
        public CustomConfigInfo scanConfig;
        public int downloadTimeout = 2;
        private bool skipAfterFoundIPsEnabled;
        private bool skipAfterAWhileEnabled;
        private Stopwatch curRangeTimer;
        private List<string> logMessages = new List<string>();
        private bool skipAfterPercentDone;
        private int skipMinPercent;

        public ScanEngine()
        {
            resetProgressInfo();
            // load cf ip list
            loadCFIPList();
        }

        public bool start(ScanType scanType = ScanType.SCAN_CLOUDFLARE_IPS)
        {
            if(progressInfo.isScanRunning) {
                return false;
            }

            resetProgressInfo();
            
            progressInfo.isScanRunning = true;
            progressInfo.scanResults = new Config.ScanResults( "results/" + DateTime.Now.ToString("yyyyMMdd-HHmmss") + "-results.json");
            progressInfo.scanResults.startDate = DateTime.Now;

            if (scanType == ScanType.SCAN_CLOUDFLARE_IPS)
                scanInCfIPRanges();
            else
                scanInPervResults();


            progressInfo.isScanRunning = false;

            return true;
        }

        // scan in previous scan result
        private void scanInPervResults()
        {
            progressInfo.totalIPRanges = 1;

            if (workingIPsFromPrevScan == null)
                return;

            var listIP = workingIPsFromPrevScan.Select(x => x.ip).ToList();
            progressInfo.currentIPRangeTotalIPs = listIP.Count;

            LogControl.Write($"Start scanning {listIP.Count:n0} ip in on previous scan results");
            Stopwatch sw = Stopwatch.StartNew();
            parallelScan(listIP);
            LogControl.Write($"End of scanning {listIP.Count:n0} ip in {sw.Elapsed.TotalSeconds:n0} sec\n");
            progressInfo.currentIPRangesNumber = 2;
        }

        // scan in all cloudflare ip range
        private void scanInCfIPRanges()
        {

            if (hasError)
                return;

            progressInfo.totalIPRanges = cfIPRangeList.Count();
            progressInfo.currentIPRangesNumber = 1;


            foreach (var cfIP in cfIPRangeList)
            {
                // stop scan?
                if (progressInfo.stopRequested == true)
                {
                    break;
                }

                progressInfo.totalCheckedIPInCurIPRange = 0;
                progressInfo.scanResults.totalFoundWorkingIPsCurrentRange = 0;

                if (isValidIPRange(cfIP))
                {
                    List<string> ipRange = IPAddressExtensions.getIPRange(cfIP);
                    progressInfo.currentIPRange = cfIP;
                    progressInfo.currentIPRangeTotalIPs = ipRange.Count();
                    LogControl.Write(String.Format("Start scanning {0} ip in {1}", ipRange.Count, cfIP));
                    logMessages.Add($"Starting range: {cfIP} ...");
                    curRangeTimer = Stopwatch.StartNew();
                    parallelScan(ipRange);
                    LogControl.Write(String.Format("End of scanning {0} {1} ip in {2} sec\n\n", cfIP, ipRange.Count, curRangeTimer.Elapsed.TotalSeconds));

                    progressInfo.currentIPRangesNumber++;

                    // skip current range?
                    if (progressInfo.skipCurrentIPRange == true)
                    {
                        LogControl.Write(String.Format("IP range skipped by user {0}", cfIP));
                        progressInfo.skipCurrentIPRange = false;
                    }
                }
            }
        }

        public void setCFIPRangeList(string[] list)
        {
            this.cfIPRangeList = list;
        }

        private bool isValidIPRange(string cfIP)
        {
            return cfIP != "" && cfIP.Contains('/') && cfIP.Contains('.');
        }

        private void parallelScan(List<string> ipRange)
        {
            //var bag = new ConcurrentBag<string>();
            cts = new CancellationTokenSource();
            ParallelOptions po = new ParallelOptions();
            po.CancellationToken = cts.Token;
            po.MaxDegreeOfParallelism = concurrentProcess; //System.Environment.ProcessorCount;

            try
            {
                object locker = new object();
                Parallel.ForEach(ipRange, po, (ip, state, index) =>
                {
                    lock (locker) {
                        progressInfo.curentWorkingThreads++;
                    }
                    var checker = new CheckIPWorking(ip, targetSpeed, scanConfig, downloadTimeout);
                    bool isOK = checker.check();

                    lock (locker) {
                        progressInfo.curentWorkingThreads--;
                        progressInfo.lastCheckedIP = ip;
                        progressInfo.totalCheckedIPInCurIPRange++;
                        progressInfo.totalCheckedIP++;
                    }

                    //Thread.Sleep(1);
                    LogControl.Write($"{ip.PadRight(15)} is {isOK.ToString().PadRight(5)} front in: {checker.frontingDuration:n0} ms, dl in: {checker.downloadDuration:n0} ms");

                    if (isOK)
                    {
                        progressInfo.scanResults.addIPResult(checker.downloadDuration, ip);
                    }

                    // should we auto skip?
                    checkForAutoSkips();

                    // monitoring exceptions rate
                    monitoExceptions(checker);
                }
                );
            }
            catch (OperationCanceledException ex)
            {
                //logMessages.Add("Scan cancel requested.");
            }
            catch (Exception ex) {
                logMessages.Add($"Unknown Error on Scan Engine: {ex.Message}");
            }
            finally
            {
                cts.Dispose();
                
            }
        }

        private void monitoExceptions(CheckIPWorking checker)
        {
            // monitoring exceptions rate
            if (checker.downloadException != "")
                progressInfo.downloadExceptions.addError(checker.downloadException);
            else
                progressInfo.downloadExceptions.addScuccess();

            if (checker.frontingException != "")
                progressInfo.frontingExceptions.addError(checker.frontingException);
            else
                progressInfo.frontingExceptions.addScuccess();
        }

        private void checkForAutoSkips()
        {
            
            // skip after 2 minute
            if (skipAfterAWhileEnabled)
            {
                if(curRangeTimer.Elapsed.TotalMinutes >= 3)
                {
                    if (!progressInfo.skipCurrentIPRange)
                        logMessages.Add($"Auto skipping {progressInfo.currentIPRange} after founding 3 minutes of scanning.");

                    skipCurrentIPRange();
                }
            }

            // skip after 5 minute
            if (skipAfterFoundIPsEnabled)
            {
                if (progressInfo.scanResults.totalFoundWorkingIPsCurrentRange >= 5)
                {
                    if(!progressInfo.skipCurrentIPRange)
                        logMessages.Add($"Auto skipping {progressInfo.currentIPRange} after founding 5 working IPs.");

                    skipCurrentIPRange();
                }
            }

            // skip after percent done
            if (skipAfterPercentDone && skipMinPercent > 0)
            {
                if (progressInfo.getCurrentRangePercentIsDone() >= skipMinPercent)
                {
                    if (!progressInfo.skipCurrentIPRange)
                        logMessages.Add($"Auto skipping {progressInfo.currentIPRange} after {skipMinPercent}% of range is scanned.");

                    skipCurrentIPRange();
                }
            }
        }

        protected void resetProgressInfo()
        {
            progressInfo = new ScanProgressInfo();
        }

        public List<string> fetchLogMessages()
        {
            List<string> curLogMessages = logMessages;
            logMessages = new();
            return curLogMessages;
        }


        public bool loadCFIPList(string fileName = "cf.local.iplist")
        {
            CFIPList ipList = new CFIPList(fileName);
            if (ipList.isIPListValid())
            {
                cfIPRangeList = ipList.getIPList();
                ipListLoader = ipList;
                return true;
            }

            progressInfo.lastErrMessage = "Invalid cloudflare IP list";
            progressInfo.hasError = true;
            return false;
        }


        internal void stop()
        {
            try
            {
                if (progressInfo.isScanRunning)
                {
                    progressInfo.stopRequested = true;
                    cts.Cancel();
                }
            }
            catch (Exception)
            {}
        }

        public void skipCurrentIPRange() {
            progressInfo.skipCurrentIPRange = true;
            cts.Cancel();
        }

        internal void setSkipAfterFoundIPs(bool enabled)
        {
            this.skipAfterFoundIPsEnabled = enabled;
        }

        internal void setSkipAfterAWhile(bool enabled)
        {
            this.skipAfterAWhileEnabled = enabled;
        }

        internal void setSkipAfterScanPercent(bool enabled, int minPercent)
        {
            this.skipAfterPercentDone = enabled;
            this.skipMinPercent = minPercent;
        }
    }

    enum ScanType
    {
        SCAN_CLOUDFLARE_IPS,
        SCAN_IN_PERV_RESULTS
    }
}
