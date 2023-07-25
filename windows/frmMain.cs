using System;
using System.Collections;
using System.Collections.Concurrent;
using System.Diagnostics;
using System.Diagnostics.Eventing.Reader;
using System.Net;
using System.Net.NetworkInformation;
using System.Security.Policy;
using System.Text;
using System.Threading;
using System.Windows.Forms;
using System.Windows.Forms.Automation;
using System.Windows.Forms.Design;
using WinCFScan.Classes;
using WinCFScan.Classes.Checker;
using WinCFScan.Classes.Config;
using WinCFScan.Classes.HTTPRequest;
using WinCFScan.Classes.IP;
using WinCFScan.Forms;
using static System.Net.Mime.MediaTypeNames;

namespace WinCFScan
{
    public partial class frmMain : Form
    {
        private const string ourGitHubUrl = "https://github.com/MortezaBashsiz/CFScanner";
        private const string helpCustomConfigUrl = "https://github.com/MortezaBashsiz/CFScanner/discussions/210";
        private const string helpDiagnoseUrl = "https://github.com/MortezaBashsiz/CFScanner/discussions/331";
        private const string buyMeCoffeeUrl = "https://www.buymeacoffee.com/Bashsiz";
        ConfigManager configManager;
        bool oneTimeChecked = false; // config checked once?
        ScanEngine scanEngine;
        private List<ResultItem> currentScanResults = new();
        private bool scanFinished = false;
        private bool isUpdatinglistCFIP;
        private bool isAppCongigValid = true;
        private bool isManualTesting = false; // is testing ips 
        private ListViewColumnSorter listResultsColumnSorter;
        private ListViewColumnSorter listCFIPsColumnSorter;
        private Version appVersion;
        private AppUpdateChecker appUpdateChecker;
        private bool stopAvgTetingIsRequested;
        private Stopwatch screenReaderStopWatch = new();
        private string diagnoseIPAddress = ""; // ip to use for diagnosing tests
        private bool showDiagnosResultMessageBox = false; // only when new config is added
        private bool isInDiagnosingMode = false;

        private ScanType? lastScanType; // in all - in prev, diagnose
        private CheckType checkType; // download - upload - both

        public frmMain()
        {
            InitializeComponent();

            listResultsColumnSorter = new ListViewColumnSorter();
            this.listResults.ListViewItemSorter = listResultsColumnSorter;

            listCFIPsColumnSorter = new ListViewColumnSorter();
            this.listCFIPList.ListViewItemSorter = listCFIPsColumnSorter;

            // load configs
            configManager = new();
            if (!configManager.isConfigValid())
            {
                addTextLog("App config is not valid! we can not continue.");

                if (configManager.errorMessage != "")
                {
                    addTextLog(configManager.errorMessage);
                }

                isAppCongigValid = false;
            }

            //var client = new HttpClient();
            //string dlUrl = "https://" + ConfigManager.Instance.getAppConfig().scanDomain;
            //HttpContent c = new StringContent(new String('*', 900_000), Encoding.UTF8, "text/plain");
            //client.Timeout = TimeSpan.FromSeconds(10);
            //var rr = client.PostAsync(dlUrl, c).Result;



            scanEngine = new ScanEngine();

            loadLastResultsComboList();
            comboDLTargetSpeed.SelectedIndex = 3;   // 100kb/s
            comboUpTargetSpeed.SelectedIndex = 2;   // 50kb/s
            comboCheckType.SelectedIndex = 0;       // Only Download
            comboFronting.SelectedIndex = 0;       // With Fronting
            comboDownloadTimeout.SelectedIndex = 0; // 2 seconds timeout
            loadCustomConfigsComboList();

            appVersion = AppUpdateChecker.getCurrentVersion();

            DateTime buildDate = new DateTime(2000, 1, 1)
                                    .AddDays(appVersion.Build).AddSeconds(appVersion.Revision * 2);
            string displayableVersion = $" - {appVersion}";
            this.Text += displayableVersion;

            appUpdateChecker = new AppUpdateChecker();

            screenReaderStopWatch.Start();

            // is debug mode enable? this line should be at bottom line
            checkEnableDebugMode();
        }

        private void loadCustomConfigsComboList(string selectedConfigFileName = "")
        {
            if (!configManager.isConfigValid() || configManager.customConfigs.customConfigInfos.Count == 0)
            {
                comboConfigs.SelectedIndex = 0;
                return;
            }

            comboConfigs.Items.Clear();
            comboConfigs.Items.Add(new CustomConfigInfo("Default", "Default"));

            foreach (var customConfig in configManager.customConfigs.customConfigInfos)
            {
                comboConfigs.Items.Add(customConfig);
            }

            int selectedIndex = 0;
            if (selectedConfigFileName != "")
            {
                selectedIndex = comboConfigs.FindStringExact(selectedConfigFileName);
            }

            comboConfigs.SelectedIndex = selectedIndex;
        }

        public CustomConfigInfo getSelectedV2rayConfig()
        {
            if (comboConfigs.SelectedItem is not null and not (object)"Default")
            {
                return (CustomConfigInfo)comboConfigs.SelectedItem;
            }

            // return default config if nothing is selected
            return new CustomConfigInfo("Default", "Default");
        }

        private void checkEnableDebugMode()
        {
            if (configManager.enableDebug)
            {
                comboConcurrent.Text = "1";
                comboConcurrent.Enabled = false;
                lblDebugMode.Visible = true;
                addTextLog("Debug mode is enabled. In this mode concurrent process is set to 1 and you can see scan debug data in 'debug.txt' file in the app directory.");
                addTextLog("To exit debug mode delete 'enable-debug' file from the app directory and then re-open the app.");

                string systemInfo = $"OS: {System.Runtime.InteropServices.RuntimeInformation.OSDescription} {System.Runtime.InteropServices.RuntimeInformation.OSArchitecture}, " +
                    $"CPU Arch: {System.Runtime.InteropServices.RuntimeInformation.ProcessArchitecture}, Framework: {System.Runtime.InteropServices.RuntimeInformation.FrameworkDescription}";
                Tools.logStep($"\n\nApp started. Version: {appVersion}\n{systemInfo}");
            }
        }

        // add text log to log textbox
        delegate void SetTextCallback(string log, bool sendToScreenReaderToo = false);
        public void addTextLog(string log, bool sendToScreenReaderToo = false)
        {
            try
            {
                if (this.txtLog.InvokeRequired)
                {
                    SetTextCallback d = new SetTextCallback(addTextLog);
                    this.Invoke(d, new object[] { log, sendToScreenReaderToo });
                }
                else
                {
                    txtLog.AppendText(log + Environment.NewLine);
                    if (sendToScreenReaderToo)
                        sendScreenReaderMsg(log);
                }
            }
            catch (Exception ex)
            {
            }
        }


        private void btnScanInPrevResults_Click(object sender, EventArgs e)
        {
            startStopScan(ScanType.SCAN_IN_PERV_RESULTS);
        }

        private void btnStart_ButtonClick(object sender, EventArgs e)
        {
            var scanType = lastScanType is null or ScanType.DIAGNOSING ? ScanType.SCAN_CLOUDFLARE_IPS : lastScanType;
            startStopScan((ScanType)scanType);
        }

        // setting Fronting to NO and also DL Target Speed to Zero at the same time is not a valid scan setting
        // and here we will prevent it.
        private bool isValidScanSettings()
        {
            if (getSelectedFrontingType() == FrontingType.NO && getDownloadTargetSpeed().isSpeedZero())
                return false;

            return true;
        }


        private void startStopScan(ScanType scanType = ScanType.SCAN_CLOUDFLARE_IPS)
        {
            if (!isAppCongigValid)
            {
                showCanNotContinueMessage();
                return;
            }

            if (isManualTesting)
            {
                addTextLog($"Can not start while app is scanning.");
                return;
            }

            if (!isValidScanSettings())
            {
                addTextLog($"You are not allowed to set Fronting test to NO and No Speed Test at the same time!");
                return;
            }

            // stop scan
            if (isScanRunning())
            {
                btnStart.Enabled = false;
                waitUntilScannerStoped();
                updateUIControls(false);
                return;
            }

            // do scan            
            switch (scanType)
            {
                // diagnose
                case ScanType.DIAGNOSING:
                    scanEngine.setPrevResults(new ResultItem(0, 0, diagnoseIPAddress));
                    addTextLog($"Start diagnosing for {diagnoseIPAddress}...");
                    break;

                // in previous results
                case ScanType.SCAN_IN_PERV_RESULTS:
                    if (isScanPaused())
                    {
                        addTextLog("Resuming scan...", true);
                        break;
                    }
                    if (currentScanResults.Count == 0)
                    {
                        addTextLog("Current result list is empty!");
                        return;
                    }
                    scanEngine.setPrevResults(currentScanResults);
                    addTextLog($"Start scanning {currentScanResults.Count} IPs in previous results...", true);
                    break;

                // in selected cloudflare ip ranges
                case ScanType.SCAN_CLOUDFLARE_IPS:
                    {
                        if (isScanPaused())
                        {
                            addTextLog("Resuming scan...", true);
                            break;
                        }
                        // set cf ip list to scan engine
                        string[] ipRanges = getCheckedCFIPList();
                        if (ipRanges.Length == 0)
                        {
                            tabControl1.SelectTab(0);
                            MessageBox.Show($"No Cloudflare IP ranges are selected. Please select some IP ranges.",
                                    "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                            return;
                        }
                        currentScanResults.Clear();
                        scanEngine.setCFIPRangeList(ipRanges);
                        addTextLog($"Start scanning {ipRanges.Length} Cloudflare IP ranges...", true);
                        break;
                    }
            }

            updateUIControls(true, scanType);

            // prepare scan engine
            bool isDiagnosing = scanType == ScanType.DIAGNOSING;
            scanEngine.dlTargetSpeed = getDownloadTargetSpeed(); // set dl target speed
            scanEngine.scanConfig = getSelectedV2rayConfig(); // set scan config
            scanEngine.checkTimeout = getDownloadTimeout(); // set download timeout
            scanEngine.isDiagnosing = isDiagnosing; // is diagnosing
            scanEngine.isRandomScan = checkScanInRandomOrder.Checked; // is random scan
            scanEngine.checkType = getSelectedCheckType(); // upload - download - both
            scanEngine.frontingType = getSelectedFrontingType(); // YES - NO Fronting
            scanEngine.upTargetSpeed = getUploadTargetSpeed(); // set upload target speed

            string scanConfigContent = scanEngine.scanConfig.content;
            Tools.logStep($"Starting scan engine with target speed: {scanEngine.dlTargetSpeed.getTargetSpeed():n0}, dl timeout: {scanEngine.checkTimeout}, " +
                $"config: '{scanEngine.scanConfig}' => " +
                $"{scanConfigContent.Substring(0, Math.Min(150, scanConfigContent.Length))}...", isDiagnosing);

            if (scanEngine.isRandomScan && scanType == ScanType.SCAN_CLOUDFLARE_IPS)
            {
                addTextLog("Scan in random order is enabled.", true);
            }

            tabControl1.SelectTab(1);

            var diagnoseFrm = new frmLogsDialog();

            if (isDiagnosing)
            {
                diagnoseFrm.Text = $"Diagnose Results for  {diagnoseIPAddress}";
                diagnoseFrm.LogText = $"Start diagnosing for {diagnoseIPAddress} at {DateTime.Now}...";
                diagnoseFrm.Show();
            }

            lastScanType = scanType;

            // start scan job in new thread
            Task.Factory.StartNew(() => isScanPaused() ? scanEngine.resume(scanType) : scanEngine.start(scanType))
                .ContinueWith(done =>
                {
                    scanFinished = true;

                    if (!scanEngine.progressInfo.pauseRequested)
                    {
                        lastScanType = null;
                    }

                    // don't update results in diagnose test
                    if (!isDiagnosing)
                        currentScanResults = scanEngine.progressInfo.scanResults.workingIPs;

                    if (isDiagnosing)
                    {
                        isInDiagnosingMode = true;
                        showDiagnoseResults(diagnoseFrm);
                    }
                    else if (isScanPaused())
                        addTextLog($"Scan Paused after {scanEngine.progressInfo.totalCheckedIP:n0} IPs tested and found {scanEngine.progressInfo.scanResults.totalFoundWorkingIPs:n0} working IPs.", true);
                    else // stopped or finished
                        addTextLog($"{scanEngine.progressInfo.totalCheckedIP:n0} IPs tested and found {scanEngine.progressInfo.scanResults.totalFoundWorkingIPs:n0} working IPs.", true);
                });

        }

        // after diagnose is finished we show the results
        private void showDiagnoseResults(frmLogsDialog diagnoseFrm)
        {
            addTextLog("Diagnosing finished.");

            // show diagnose results window
            diagnoseFrm.LogText = string.Join(Environment.NewLine, Tools.diagnoseLogs);

            // show msg box only for diagnosing custom config for first time
            if (showDiagnosResultMessageBox)
            {
                var msg = scanEngine.isV2rayExecutionSuccess ?
                    "It seems your custom config is working and v2ray.exe could run with your config." :
                    "v2ray.exe could not run with your custom config:\n\n" + scanEngine.v2rayDiagnosingMessage;
                showDiagnosResultMessageBox = false;
                diagnoseFrm.showResultsMessage(msg, scanEngine.isV2rayExecutionSuccess);

            }

            Tools.clearDiagnoseLogs();
            //diagnoseFrm.ShowDialog();
        }

        private int getDownloadTimeout()
        {
            string timeoutStr = comboDownloadTimeout.SelectedItem.ToString().Replace(" Seconds", "");

            if (int.TryParse(timeoutStr, out int downloadTimeout))
                return downloadTimeout;
            else
                return 2;
        }

        private void updateUIControls(bool isStarting, ScanType scanType = ScanType.SCAN_CLOUDFLARE_IPS)
        {
            if (isStarting)
            {
                // don't clear results list in diagnose or paused mode
                if (scanType != ScanType.DIAGNOSING && !isScanPaused())
                {
                    loadLastResultsComboList();
                    listResults.Items.Clear();
                }
                btnStart.Text = "Stop Scan";
                mnuPauseScan.Text = "Pause Scan";
                lblScanPaused.Visible = false;
                //sendScreenReaderMsg("Scan Started");
                btnScanInPrevResults.Enabled = false;
                btnResultsActions.Enabled = false;
                comboConcurrent.Enabled = false;
                comboUpTargetSpeed.Enabled = false;
                comboDLTargetSpeed.Enabled = false;
                comboConfigs.Enabled = false;
                comboCheckType.Enabled = false;
                comboFronting.Enabled = false;
                timerProgress.Enabled = true;
                //btnSkipCurRange.Enabled = true;
                comboResults.Enabled = false;
                tabPageCFRanges.Enabled = false;
            }
            else
            {   // is stopping or pausing
                btnStart.Text = isScanPaused() ? "Resume Scan" : "Start Scan";
                lblScanPaused.Visible = isScanPaused();
                mnuPauseScan.Text = isScanPaused() ? "Stop Scan" : "Pause Scan";
                btnStart.Enabled = true;

                btnResultsActions.Enabled = true;
                timerProgress.Enabled = false;
                lblRunningWorkers.Text = $"Threads: 0";

                if (!configManager.enableDebug)
                {
                    comboConcurrent.Enabled = true;
                }
                comboDLTargetSpeed.Enabled = true;
                comboUpTargetSpeed.Enabled = true;
                comboConfigs.Enabled = true;
                comboCheckType.Enabled = true;
                comboFronting.Enabled = true;

                if (!isScanPaused())
                {
                    comboResults.Enabled = true;
                    btnScanInPrevResults.Enabled = true;
                    tabPageCFRanges.Enabled = true;
                }

                // don't do this stuff in diagnosing mode
                if (scanType != ScanType.DIAGNOSING)
                {
                    // save result file if found working IPs
                    var scanResults = scanEngine.progressInfo.scanResults;
                    if (scanResults.totalFoundWorkingIPs != 0)
                    {
                        // save results into disk
                        if (!scanResults.save())
                        {
                            addTextLog($"Could not save scan result into the file: {scanResults.resultsFileName}");
                        }
                    }
                    else
                    {
                        // delete result file if there is no working ip
                        scanResults.remove();
                    }

                    loadLastResultsComboList();

                    // sort results list
                    listResultsColumnSorter.Order = SortOrder.Ascending;
                    listResultsColumnSorter.SortColumn = 1;
                    listResults.Sort();
                }
            }
        }
        private void updateConrtolsProgress(bool forceUpdate = false)
        {
            var pInf = scanEngine.progressInfo;
            if (isScanRunning() || forceUpdate)
            {
                int curRangeNumber = Math.Max(pInf.currentIPRangesNumber - 1, 0);

                lblLastIPRange.Text = $"Current IP range: {pInf.currentIPRange} ({curRangeNumber:n0}/{pInf.totalIPRanges:n0})";
                labelLastIPChecked.Text = $"Last checked IP:  {pInf.lastCheckedIP} ({pInf.totalCheckedIPInCurIPRange:n0}/{pInf.currentIPRangeTotalIPs:n0})";
                lblTotalWorkingIPs.Text = $"Total working IPs found:  {pInf.scanResults.totalFoundWorkingIPs:n0}";
                if (pInf.scanResults.fastestIP != null)
                {
                    long delay = scanEngine.checkType == CheckType.UPLOAD ? pInf.scanResults.fastestIP.uploadDelay : pInf.scanResults.fastestIP.downloadDelay;
                    txtFastestIP.Text = $"{pInf.scanResults.fastestIP.ip}  -  {delay:n0} ms";
                }

                lblRunningWorkers.Text = $"Threads: {pInf.curentWorkingThreads}";

                prgOveral.Maximum = pInf.totalIPRanges;

                prgOveral.Value = curRangeNumber;

                prgCurRange.Maximum = pInf.currentIPRangeTotalIPs;
                prgCurRange.Value = Math.Min(pInf.totalCheckedIPInCurIPRange, prgCurRange.Maximum);
                prgCurRange.ToolTipText = $"Current IP range progress: {pInf.getCurrentRangePercentIsDone():f1}%";

                fetchWorkingIPResults();
                pInf.scanResults.autoSave();

                fetchScanEngineLogMessages();

                // exception rate
                pInf.frontingExceptions.setControlColorStyles(btnFrontingErrors);
                pInf.downloadUploadExceptions.setControlColorStyles(btnDownloadErrors);
                btnFrontingErrors.Text = $"Fronting Errors : {pInf.frontingExceptions.getErrorRate():f1}%";
                btnDownloadErrors.Text = $"DL && UP Errors : {pInf.downloadUploadExceptions.getErrorRate():f1}%";
                btnFrontingErrors.ToolTipText = $"Total errors: {pInf.downloadUploadExceptions.getTotalErros()}";
                btnDownloadErrors.ToolTipText = $"Total errors: {pInf.frontingExceptions.getTotalErros()}";
            }
        }

        private void sendScreenReaderMsg(string msg, bool skipIfTooFrequent = false, AutomationNotificationProcessing notifProcessing = AutomationNotificationProcessing.All)
        {
            var totalElapsed = screenReaderStopWatch.Elapsed.TotalSeconds;

            // dont send it too frequent
            if (totalElapsed < 5 && skipIfTooFrequent)
            {
                //addTextLog("sendScreenReaderMsg skipped " + totalElapsed);
                return;
            }


            // if its to frequent wait a few
            if (totalElapsed < 1)
            {
                var sw = new Stopwatch();
                sw.Start();
                do
                {
                    System.Windows.Forms.Application.DoEvents();
                    Thread.Sleep(100);
                } while (sw.Elapsed.TotalMilliseconds <= 1000);
                //addTextLog($"waited for {sw.Elapsed.TotalMilliseconds} ms");

            }
            this.ActiveControl.AccessibilityObject.RaiseAutomationNotification(AutomationNotificationKind.Other, notifProcessing, msg);
            //this.AccessibilityObject.RaiseAutomationNotification(AutomationNotificationKind.Other, notifProcessing, msg);
            screenReaderStopWatch.Restart();
        }

        private void timerBase_Tick(object sender, EventArgs e)
        {
            oneTimeChecks();
            if (scanFinished)
            {
                scanFinished = false;
                updateConrtolsProgress(true);
                updateUIControls(false, isInDiagnosingMode ? ScanType.DIAGNOSING : ScanType.SCAN_CLOUDFLARE_IPS);
                isInDiagnosingMode = false;
            }

            btnStopAvgTest.Visible = isManualTesting;
        }

        private void timerProgress_Tick(object sender, EventArgs e)
        {
            updateConrtolsProgress();
        }


        private void fetchScanEngineLogMessages()
        {
            var messages = scanEngine.fetchLogMessages();
            foreach (var message in messages)
            {
                addTextLog(message);
                if (message.StartsWith("Starting range: "))
                    sendScreenReaderMsg("Starting next IP range...", true);
            }
        }

        // fetch new working ips and add to the list view while scanning
        private void fetchWorkingIPResults()
        {
            List<ResultItem> scanResults = scanEngine.progressInfo.scanResults.fetchWorkingIPs();
            addResulItemsToListView(scanResults);
        }

        private void loadLastResultsComboList()
        {
            comboResults.Items.Clear();
            comboResults.Items.Add("Current Scan Results");
            var resultFiles = Directory.GetFiles("results/", "*.json");
            foreach (var resultFile in resultFiles)
            {
                comboResults.Items.Add(resultFile);
            }
            comboResults.SelectedIndex = 0;
        }

        private void comboResults_SelectedIndexChanged(object sender, EventArgs e)
        {
            string? filename = getSelectedScanResultFilename();
            if (filename != null)
            {
                fillResultsListView(filename);
            }
        }

        // get filename of selected scan result
        private string? getSelectedScanResultFilename()
        {
            string? filename = comboResults.SelectedItem.ToString();
            if (filename != null && File.Exists(filename) && filename != "Current Scan Results")
            {
                return filename;
            }

            return null;
        }

        // load previous results into list view
        private void fillResultsListView(string resultsFileName, bool plainFile = false)
        {
            var results = new ScanResults(resultsFileName);
            bool isLoaded = plainFile ? results.loadPlain() : results.load();
            if (isLoaded)
            {
                listResults.Items.Clear();
                var loadedResults = results.getLoadedInstance();
                this.currentScanResults = loadedResults.workingIPs;
                addResulItemsToListView(currentScanResults);

                addTextLog($"'{resultsFileName}' loaded with {loadedResults.totalFoundWorkingIPs:n0} working IPs, scan time: {loadedResults.startDate}");
            }
            else
            {
                addTextLog($"Could not load scan result file from disk: '{resultsFileName}'");
            }
        }

        private void addResulItemsToListView(List<ResultItem>? workingIPs)
        {
            if (workingIPs != null)
            {
                int index = 0;
                listResults.BeginUpdate();
                listResults.ListViewItemSorter = null;
                foreach (ResultItem resultItem in workingIPs)
                {
                    index++;
                    listResults.Items.Add(new ListViewItem(new string[] { resultItem.ip, resultItem.downloadDelay.ToString(), resultItem.uploadDelay.ToString() }));
                }
                listResults.EndUpdate();
                listResults.ListViewItemSorter = listResultsColumnSorter;
                lblPrevListTotalIPs.Text = $"{listResults.Items.Count:n0} IPs";

                //if (screenReaderStopWatch.Elapsed.TotalSeconds > 5)
                //    sendScreenReaderMsg($"Found {workingIPs.Count} new IP address.", true);
            }
        }


        private void oneTimeChecks()
        {
            if (!oneTimeChecked && isAppCongigValid)
            {

                // check if client config file is exists and update
                if (configManager.getClientConfig() != null && configManager.getClientConfig().isClientConfigOld())
                {
                    remoteUpdateClientConfig();
                }

                //Load cf ip ranges
                loadCFIPListView();

                // check fronting domain
                Task.Factory.StartNew(() =>
                {
                    var checker = new CheckIPWorking();
                    if (!checker.checkFronting(false, 5))
                    {
                        addTextLog($"Fronting domain is not accessible! you might need to get new fronting url from our github or check your Internet connection.");
                    }
                });

                // check for updates
                if (appUpdateChecker.shouldCheck())
                {
                    checkForUpdate();
                }

                oneTimeChecked = true;
            }
        }

        // update client config and cf ip list
        private bool remoteUpdateClientConfig()
        {
            addTextLog("Updating client config from remote...");
            bool result = configManager.getClientConfig().remoteUpdateClientConfig();
            if (result)
            {
                // get new client config
                configManager.reloadClientConfig(); // important

                addTextLog("Client config and Cloudflare subnets are successfully updated.");
                if (!configManager.getClientConfig().isConfigValid())
                {
                    addTextLog("'client config' data is not valid!");
                }

                // reload cf subnet list
                scanEngine.loadCFIPList();
            }
            else
            {
                addTextLog("Failed to update client config. check your Internet connection or maybe client config url is blocked by your ISP!");
            }

            return result;
        }

        // check for update
        private void checkForUpdate(bool logNoNewVersion = false)
        {
            Task.Factory.StartNew(() => { appUpdateChecker.check(); })
            .ContinueWith(done =>
            {
                if (appUpdateChecker.isFoundNewVersion())
                {
                    addTextLog($"There is a new version available ({appUpdateChecker.getUpdateVersion()}) Download it from here: {ourGitHubUrl}/releases");
                }
                else if (appUpdateChecker.updateCheckResult == UpdateCheckResult.HasError)
                {
                    addTextLog("Something went wrong while checking for update!");
                }
                else if (logNoNewVersion)
                {
                    addTextLog("Everything is up to date.");
                }
            });
        }

        private void comboConcurrent_TextChanged(object sender, EventArgs e)
        {
            var con = getConcurentProcess();
            comboConcurrent.Text = con.ToString();
            scanEngine.concurrentProcess = con;
        }

        private int getConcurentProcess()
        {
            int val = 0;
            bool isInteger = int.TryParse(comboConcurrent.Text, out val);
            if (!isInteger || val < 1 || val > 128)
            {
                val = 4;
            }

            return val;
        }

        private void btnCopyFastestIP_Click(object sender, EventArgs e)
        {
            if (scanEngine.progressInfo.scanResults?.fastestIP != null)
            {
                setClipboard(scanEngine.progressInfo.scanResults.fastestIP.ip);
            }
        }

        private bool setClipboard(string text)
        {
            try
            {
                Clipboard.SetText(text);
                return true;
            }
            catch (Exception ex) { }

            return false;
        }

        // results actions
        private void btnResultsActions_MouseClick(object sender, MouseEventArgs e)
        {
            showResultsActionsMenu();
        }

        private void showResultsActionsMenu()
        {
            mnuResultsActions.Show(this, btnResultsActions.Left + 5, splitContainer1.Top + btnResultsActions.Top + btnResultsActions.Height + 25);
        }

        private void deleteResultsToolStripMenuItem_Click(object sender, EventArgs e)
        {
            deleteResultItem();
        }

        private void exportResultsToolStripMenuItem_Click(object sender, EventArgs e)
        {
            exportResults();
        }

        private void importResultsToolStripMenuItem_Click(object sender, EventArgs e)
        {
            importResults();
        }

        private void importResults()
        {
            if (isScanRunningOrPaused())
                return;

            openFileDialog1.Title = "Import IP results";
            openFileDialog1.Filter = "";

            var result = openFileDialog1.ShowDialog();

            if (result == DialogResult.OK)
            {
                fillResultsListView(openFileDialog1.FileName, true);
                tabControl1.SelectedIndex = 1;
            };

        }

        private void deleteResultItem()
        {
            if (isScanRunningOrPaused())
                return;

            string? filename = getSelectedScanResultFilename();
            if (filename != null)
            {
                var result = MessageBox.Show($"Are you sure you want to delete {filename}?",
                            "Warning", MessageBoxButtons.YesNo, MessageBoxIcon.Warning);
                if (result == DialogResult.Yes)
                {
                    try
                    {
                        File.Delete(filename);
                        addTextLog($"'{filename}' has been deleted.");
                        loadLastResultsComboList();
                        listResults.Items.Clear();
                        currentScanResults = new();
                        lblPrevListTotalIPs.Text = "0 IPs";

                    }
                    catch (Exception)
                    {
                        addTextLog($"Could not delete '{filename}'");
                    }
                }
            }
        }

        // user selected ip list of cloudflare
        private string[] getCheckedCFIPList(bool getIPCount = false, bool inRandomeOrder = false)
        {
            var ipList = listCFIPList.CheckedItems.Cast<ListViewItem>()
                                 .Select(item =>
                                 {
                                     return getIPCount ? item.SubItems[1].Text : item.SubItems[0].Text;
                                 })
                                 .ToArray<string>();

            if (checkScanInRandomOrder.Checked || inRandomeOrder)
            {
                Random rnd = new Random();
                ipList = ipList.OrderBy(x => rnd.Next()).ToArray();
            }

            return ipList;


        }

        // list view right click
        private void listResults_MouseClick(object sender, MouseEventArgs e)
        {
            if (e.Button == MouseButtons.Right)
            {
                mnuListViewCopyIP.Text = "Copy IP address " + getSelectedIPAddress();
                mnuTestThisIP.Text = "Test this IP address " + getSelectedIPAddress();
                mnuListView.Show(listResults, e.X, e.Y);
            }
        }

        // add cloudflare ip ranges to list view
        private void loadCFIPListView()
        {
            if (!scanEngine.ipListLoader.isIPListValid())
            {
                addTextLog("Cloudflare IP range file is not valid!");
                lblCFIPListStatus.Text = "Failed to load IP ranges.";
                lblCFIPListStatus.ForeColor = Color.Red;
                return;
            }
            else
            {
                lblCFIPListStatus.ForeColor = Color.Black;
            }

            listCFIPList.Items.Clear();

            isUpdatinglistCFIP = true;
            addTextLog($"Loading Cloudflare IPs ranges...");

            var items = scanEngine.ipListLoader.validIPRanges.Select(ipRange =>
            new ListViewItem(new[] { ipRange.rangeText, $"{ipRange.totalIps:n0}" })
            {
                Checked = true
            }).ToArray();
            listCFIPList.Items.AddRange(items);

            isUpdatinglistCFIP = false;
            addTextLog($"Total {scanEngine.ipListLoader.totalIPs:n0} Cloudflare IPs are ready to be scanned.");
            updateCFIPListStatusText();
        }

        private CheckType getSelectedCheckType()
        {
            return (CheckType)comboCheckType.SelectedIndex;
        }

        private FrontingType getSelectedFrontingType()
        {
            return (FrontingType)comboFronting.SelectedIndex;
        }

        private ScanSpeed getDownloadTargetSpeed()
        {
            int speed;

            if (int.TryParse(comboDLTargetSpeed.SelectedItem.ToString().Replace(" KB/s", ""), out speed))
                return new ScanSpeed(speed);
            else
                return new ScanSpeed(0);
        }

        private ScanSpeed getUploadTargetSpeed()
        {
            int speed;

            if (int.TryParse(comboUpTargetSpeed.SelectedItem.ToString().Replace(" KB/s", ""), out speed))
                return new ScanSpeed(speed);
            else
                return new ScanSpeed(0);
        }

        private string? getSelectedIPAddress()
        {
            try
            {
                return listResults.SelectedItems[0].SubItems[0].Text;
            }
            catch (Exception ex) { }

            return null;
        }

        private void listResults_MouseDoubleClick(object sender, MouseEventArgs e)
        {
            testSelectedIP();
        }

        private void updateCFIPListStatusText()
        {
            try
            {
                var ipRangeCounts = getCheckedCFIPList(true);
                uint sum = 0;
                foreach (var item in ipRangeCounts)
                {
                    sum += uint.Parse(item.Replace(",", ""));
                }
                lblCFIPListStatus.Text = $"{ipRangeCounts.Length} Cloudflare IP ranges are selected, contains {sum:n0} IPs";
            }
            catch (Exception)
            { }
        }

        private void btnSelectAllIPRanges_Click(object sender, EventArgs e)
        {
            changeCFListViewItemsCheckState(true);
        }

        private void btnSelectNoneIPRanges_Click(object sender, EventArgs e)
        {
            changeCFListViewItemsCheckState(false);
        }

        private void changeCFListViewItemsCheckState(bool isChecked)
        {
            listCFIPList.BeginUpdate();
            isUpdatinglistCFIP = true;
            foreach (ListViewItem item in listCFIPList.Items)
            {
                item.Checked = isChecked;
            }
            listCFIPList.EndUpdate();
            isUpdatinglistCFIP = false;
            updateCFIPListStatusText();
        }

        private void listCFIPList_ItemChecked(object sender, ItemCheckedEventArgs e)
        {
            if (!isUpdatinglistCFIP)
                updateCFIPListStatusText();
        }

        private void showCanNotContinueMessage()
        {
            MessageBox.Show("App configuration file is not valid. We can not continue.", "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);

        }

        private void frmMain_FormClosing(object sender, FormClosingEventArgs e)
        {
            sendScreenReaderMsg("Exiting the App.");
            addTextLog("Exiting...");
            waitUntilScannerStoped();

            try
            {
                System.Windows.Forms.Application.Exit();
            }
            catch (Exception)
            { }
        }

        private void waitUntilScannerStoped()
        {
            if (isScanRunning())
            {
                // stop scan
                var sw = new Stopwatch();
                sw.Start();
                scanEngine.stop();
                do
                {
                    System.Windows.Forms.Application.DoEvents();
                    Thread.Sleep(100);
                } while (isScanRunning() && sw.Elapsed.TotalSeconds < 7);

            }
        }

        private void openUrl(string url)
        {
            try
            {
                ProcessStartInfo sInfo = new ProcessStartInfo(url) { UseShellExecute = true };
                Process.Start(sInfo);
            }
            catch (Exception)
            {
                addTextLog($"Open this url in your browser: {url}");
            }
        }

        // sort CF Ranges listview
        private void listCFIPList_ColumnClick(object sender, ColumnClickEventArgs e)
        {
            sortListView(listCFIPList, listCFIPsColumnSorter, e.Column);
        }

        // sort Results listview
        private void listResults_ColumnClick(object sender, ColumnClickEventArgs e)
        {
            sortListView(listResults, listResultsColumnSorter, e.Column);
        }

        // sort listview
        private void sortListView(ListView listView, ListViewColumnSorter columnSorter, int columnNumber)
        {
            // Determine if clicked column is already the column that is being sorted.
            if (columnNumber == columnSorter.SortColumn)
            {
                // Reverse the current sort direction for this column.
                if (columnSorter.Order == SortOrder.Ascending)
                {
                    columnSorter.Order = SortOrder.Descending;
                }
                else
                {
                    columnSorter.Order = SortOrder.Ascending;
                }
            }
            else
            {
                // Set the column number that is to be sorted; default to ascending.
                columnSorter.SortColumn = columnNumber;
                columnSorter.Order = SortOrder.Ascending;
            }

            // Perform the sort with these new sort options.
            listView.Sort();
        }

        // test user provided ip
        private void scanASingleIPAddressToolStripMenuItem_Click(object sender, EventArgs e)
        {
            string ipAddr;
            if (getIPFromUser(out ipAddr, "Test Single IP Address"))
            {
                testAvgSingleIP(ipAddr, 1, getDownloadTargetSpeed(), getUploadTargetSpeed(), getSelectedV2rayConfig(), getDownloadTimeout(), getSelectedCheckType(), getSelectedFrontingType());
            }
        }

        private bool getIPFromUser(out string ipAddr, string title)
        {
            ipAddr = Tools.ShowDialog("Enter a valid IP address:", title);

            if (ipAddr == "" || ipAddr == null) { return false; }

            if (!IPAddressExtensions.isValidIPAddress(ipAddr))
            {
                // msg
                MessageBox.Show("Invalid IP address is entered!", "Error", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return false;
            }

            return true;
        }

        private void mnuListViewCopyIP_Click(object sender, EventArgs e)
        {
            var IPAddr = getSelectedIPAddress();
            if (IPAddr != null)
            {
                try
                {
                    Clipboard.SetText(IPAddr);
                }
                catch (Exception ex)
                {
                    addTextLog("Could not copy to clipboard!");
                }
            }
        }

        private void btnLoadIPRanges_Click(object sender, EventArgs e)
        {
            loadCustomCPIPList();
        }

        private void loadCustomIPRangesToolStripMenuItem_Click(object sender, EventArgs e)
        {
            loadCustomCPIPList();
        }

        // load custom ip ranges from disk by user input
        private void loadCustomCPIPList()
        {
            if (isScanRunningOrPaused())
                return;

            openFileDialog1.Title = "Load custom cloudflare IP ranges";
            openFileDialog1.Filter = "";

            var result = openFileDialog1.ShowDialog();

            if (result == DialogResult.OK)
            {
                if (scanEngine.loadCFIPList(openFileDialog1.FileName))
                {
                    loadCFIPListView();
                    tabControl1.SelectedIndex = 0;
                }
                else
                {
                    addTextLog($"Could not find any valid IP ranges in '{openFileDialog1.FileName}'");
                }
            };

        }

        private void exportScanResultsToolStripMenuItem_Click(object sender, EventArgs e)
        {
            exportResults();
        }

        // export results
        private void exportResults()
        {
            var resultIPs = isScanRunningOrPaused() ? scanEngine.progressInfo.scanResults.workingIPs : currentScanResults;
            if (resultIPs.Count == 0)
            {
                addTextLog("Current results list is empty!");
                return;
            }

            saveFileDialog1.Title = $"Saving {resultIPs.Count} IP addresses";
            saveFileDialog1.FileName = $"scan-results-{DateTime.Now.ToString("yyyy-MM-dd-HH-mm-ss")}.txt";
            saveFileDialog1.Filter = "txt files (*.txt)|*.txt|All files (*.*)|*.*";
            var result = saveFileDialog1.ShowDialog();

            if (result == DialogResult.OK)
            {
                var scanResults = new ScanResults(resultIPs, saveFileDialog1.FileName);
                bool isSaved = scanResults.savePlain();
                if (isSaved)
                {
                    addTextLog($"{scanResults.totalFoundWorkingIPs} IPs exported into '{saveFileDialog1.FileName}'");
                }
                else
                {
                    addTextLog($"Could save into '{saveFileDialog1.FileName}'");
                }
            };
        }

        private bool isScanRunning()
        {
            return scanEngine.progressInfo.scanStatus == ScanStatus.RUNNING;
        }
        private bool isScanPaused()
        {
            return scanEngine.progressInfo.scanStatus == ScanStatus.PAUSED;
        }

        private bool isScanRunningOrPaused()
        {
            return isScanRunning() || isScanPaused();
        }

        private void exitToolStripMenuItem_Click(object sender, EventArgs e)
        {
            System.Windows.Forms.Application.Exit();
        }

        private void importScanResultsToolStripMenuItem_Click(object sender, EventArgs e)
        {
            importResults();
        }



        private void checkForUpdateToolStripMenuItem_Click(object sender, EventArgs e)
        {
            addTextLog("Checking for new version...");
            checkForUpdate(true);
        }

        private void comboConfigs_SelectedIndexChanged(object sender, EventArgs e)
        {
            CustomConfigInfo customConfigInfo = getSelectedV2rayConfig();
            if (!customConfigInfo.isDefaultConfig())
            {
                addTextLog($"Custom v2ray config is selected: '{customConfigInfo.fileName}'");
            }
        }

        private void addCustomV2rayConfigToolStripMenuItem_Click(object sender, EventArgs e)
        {
            if (isScanRunningOrPaused())
            {
                addTextLog("Can not do this while scanning or paused");
                return;
            }

            openFileDialog1.Title = "Add custom v2ray config file";
            openFileDialog1.Filter = "Json files (*.json)|*.json";
            var result = openFileDialog1.ShowDialog();

            if (result == DialogResult.OK)
            {
                bool isDone = CustomConfigs.addNewConfigFile(openFileDialog1.FileName, out string errorMessage);
                if (isDone)
                {
                    // add config
                    configManager.customConfigs.loadCustomConfigs();
                    loadCustomConfigsComboList(Path.GetFileName(openFileDialog1.FileName));
                    addTextLog("New custom v2ray config is added.");

                    // diagnose with this config
                    result = MessageBox.Show($"Your custom config is successfully added.\nDo you want to diagnose with this config to see if it works?",
                            "Diagnose Custom Config", MessageBoxButtons.YesNo, MessageBoxIcon.Question);

                    if (result == DialogResult.Yes)
                    {
                        showDiagnosResultMessageBox = true;
                        doRandomIPDiagnose();
                    }
                }
                else
                {
                    addTextLog($"Adding custom config is failed: {errorMessage}");
                }
            };
        }

        // Monitoring exceptions:
        private void btnFrontingErrors_ButtonClick(object sender, EventArgs e)
        {
            ExceptionMonitor frontingExceptions = scanEngine.progressInfo.frontingExceptions;
            if (frontingExceptions.hasException())
            {
                addTextLog(frontingExceptions.getTopExceptions());
            }
        }

        private void btnDownloadErrors_ButtonClick(object sender, EventArgs e)
        {
            ExceptionMonitor downloadExceptions = scanEngine.progressInfo.downloadUploadExceptions;
            if (downloadExceptions.hasException())
            {
                addTextLog(downloadExceptions.getTopExceptions());
            }
        }

        private void mnuCopyDownloadErrors_Click(object sender, EventArgs e)
        {
            if (setClipboard(scanEngine.progressInfo.downloadUploadExceptions.getTopExceptions(7)))
            {
                addTextLog("Errors copied to the clipboard.");
            }
            else
            {
                addTextLog("Could not copy to the clipboard!");
            }
        }

        private void mnuCopyFrontingErrors_Click(object sender, EventArgs e)
        {
            if (setClipboard(scanEngine.progressInfo.frontingExceptions.getTopExceptions(7)))
            {
                addTextLog("Errors copied to the clipboard.");
            }
            else
            {
                addTextLog("Could not copy to the clipboard!");
            }
        }

        private void comboDownloadTimeout_SelectedIndexChanged(object sender, EventArgs e)
        {
            int timeout = getDownloadTimeout();

            if (timeout > 2)
            {
                addTextLog($"Download timeout is set to {timeout} seconds. Only use higher timeout values if your v2ray server's response is slow.");
            }
        }

        private void btnSkipCurRange_ButtonClick(object sender, EventArgs e)
        {
            if (isScanRunning())
                scanEngine.skipCurrentIPRange();

        }

        private void mnuSkipAfterFoundIPs_Click(object sender, EventArgs e)
        {
            setAutoSkip(mnuSkipAfterFoundIPs.Checked, "Auto skip current IP range after founding 5 working IPs is");
            scanEngine.setSkipAfterFoundIPs(mnuSkipAfterFoundIPs.Checked);
            setAutoSkipStatus();
        }

        private void mnuSkipAfterAWhile_Click(object sender, EventArgs e)
        {
            setAutoSkip(mnuSkipAfterAWhile.Checked, "Auto skip current IP range after 3 minutes of scanning is");
            scanEngine.setSkipAfterAWhile(mnuSkipAfterAWhile.Checked);
            setAutoSkipStatus();
        }

        private void mnuSkipAfter10Percent_Click(object sender, EventArgs e)
        {
            skipAfterPercent(mnuSkipAfter10Percent);
        }

        private void mnuSkipAfter30Percent_Click(object sender, EventArgs e)
        {
            skipAfterPercent(mnuSkipAfter30Percent);
        }

        private void mnuSkipAfter50Percent_Click(object sender, EventArgs e)
        {
            skipAfterPercent(mnuSkipAfter50Percent);
        }

        private void setAutoSkipStatus()
        {
            lblAutoSkipStatus.Visible = mnuSkipAfter10Percent.Checked || mnuSkipAfter30Percent.Checked || mnuSkipAfter50Percent.Checked ||
                mnuSkipAfterAWhile.Checked || mnuSkipAfterFoundIPs.Checked;

            seperatorAutoSkip.Visible = lblAutoSkipStatus.Visible;
        }

        private void skipAfterPercent(ToolStripMenuItem menu)
        {
            // note: menu must have Tag
            if (menu.Checked)
                selectMinimumPercentOfAutoSkipMenu(menu.Tag.ToString());

            int minPercent = getMinimumPercentOfAutoSkip();

            if (minPercent == -1)
            {
                setAutoSkip(false, $"Auto skip current IP range after specific percentage is");
            }
            else
            {
                setAutoSkip(true, $"Auto skip current IP range after {minPercent}% is");
            }

            scanEngine.setSkipAfterScanPercent(minPercent != -1, minPercent);
            setAutoSkipStatus();

        }

        private void selectMinimumPercentOfAutoSkipMenu(string selectedMenu)
        {
            switch (selectedMenu)
            {
                case "10":
                    mnuSkipAfter30Percent.Checked = mnuSkipAfter50Percent.Checked = false;
                    break;
                case "30":
                    mnuSkipAfter10Percent.Checked = mnuSkipAfter50Percent.Checked = false;
                    break;
                case "50":
                    mnuSkipAfter10Percent.Checked = mnuSkipAfter30Percent.Checked = false;
                    break;
            }
        }

        private int getMinimumPercentOfAutoSkip()
        {
            if (mnuSkipAfter10Percent.Checked)
            {
                return 10;
            }
            else if (mnuSkipAfter30Percent.Checked)
            {
                return 30;
            }
            else if (mnuSkipAfter50Percent.Checked)
            {
                return 50;
            }

            return -1;
        }

        private void setAutoSkip(bool enabled, string message)
        {
            string enabledStatus = enabled ? "enabled" : "disabled";
            addTextLog($"{message} {enabledStatus}.");

        }

        private void mnuHelpCustomConfig_Click(object sender, EventArgs e)
        {
            openUrl(helpCustomConfigUrl);
        }

        private void mnuHelpDiagnose_Click(object sender, EventArgs e)
        {
            openUrl(helpDiagnoseUrl);
        }

        private void mnuHelpOurGitHub_Click(object sender, EventArgs e)
        {
            openUrl(ourGitHubUrl);
        }

        private void linkBuyMeCoffee_Click(object sender, EventArgs e)
        {
            openUrl(buyMeCoffeeUrl);
        }

        private void linkGithub_Click(object sender, EventArgs e)
        {
            openUrl(ourGitHubUrl);
        }

        private void updateClientConfigCloudflareSubnetsToolStripMenuItem_Click(object sender, EventArgs e)
        {
            if (configManager.getClientConfig() != null)
            {
                if (remoteUpdateClientConfig())
                {
                    // reload cf ip ranges
                    loadCFIPListView();
                };
            }
            else
                addTextLog("ClientConfig is null!");
        }


        private void testAvgSingleIP(string IPAddress, int rounds, ScanSpeed dlSpeed, ScanSpeed upSpeed, CustomConfigInfo v2rayConfig, int downloadTimeout, CheckType checkType, FrontingType frontingType)
        {

            addTextLog($"{Environment.NewLine}Testing {IPAddress} for {rounds} round(s), Scan type: {checkType}...");

            int totalSuccessCount = 0, totalFailedCount = 0;
            long bestDLDuration = 99999, bestUPDuration = 9999, bestFrontingDuration = 99999, totalDLDuration = 0, totalUPDuration = 0, totalFrontingDuration = 0;
            long averageDLDuration = 0, averageUPDuration = 0, averageFrontingDuration = 0;

            for (int i = 1; i <= rounds; i++)
            {
                // test
                var checker = new CheckIPWorking(IPAddress, dlSpeed, upSpeed, v2rayConfig, checkType, frontingType, downloadTimeout);
                var success = checker.check();

                long DLDuration = checker.downloadDuration;
                long UPDuration = checker.uploadDuration;
                long FrontingDuration = checker.frontingDuration;

                if (success)
                {
                    totalSuccessCount++;
                    bestDLDuration = Math.Min(DLDuration, bestDLDuration);
                    bestUPDuration = Math.Min(UPDuration, bestUPDuration);
                    bestFrontingDuration = Math.Min(FrontingDuration, bestFrontingDuration);
                    totalDLDuration += DLDuration;
                    totalUPDuration += UPDuration;
                    totalFrontingDuration += FrontingDuration;
                }
                else
                {
                    totalFailedCount++;
                }

                if (stopAvgTetingIsRequested)
                    break;
            }

            if (totalSuccessCount > 0)
            {
                averageDLDuration = totalDLDuration / totalSuccessCount;
                averageUPDuration = totalUPDuration / totalSuccessCount;
                averageFrontingDuration = totalFrontingDuration / totalSuccessCount;

                // print results
                string results = $"{IPAddress} => {totalSuccessCount}/{rounds} test(s) was successful." + Environment.NewLine +
                    (bestDLDuration > 0 ? $"\tDownload: Best {bestDLDuration:n0} ms, Average: {averageDLDuration:n0} ms" + Environment.NewLine : "") +
                    (bestUPDuration > 0 ? $"\tUpload  : Best {bestUPDuration:n0} ms, Average: {averageUPDuration:n0} ms" + Environment.NewLine : "") +
                    $"\tFronting: Best {bestFrontingDuration:n0} ms, Average: {averageFrontingDuration:n0} ms" + Environment.NewLine;

                addTextLog(results);
            }
            else
            {
                addTextLog($"{IPAddress} is NOT working.");
            }

        }

        private void testSelectedIPAddresses(int rounds = 1)
        {
            if (scanEngine.progressInfo.isScanRunning || isManualTesting)
            {
                addTextLog($"Can not test while app is scanning.");
                return;
            }

            isManualTesting = true;
            stopAvgTetingIsRequested = false;

            var selectedIPs = listResults.SelectedItems.Cast<ListViewItem>()
                                 .Select(item => item.SubItems[0].Text)
                                 .ToArray<string>(); ;


            var dlSpeed = getDownloadTargetSpeed();
            var upSpeed = getUploadTargetSpeed();
            var checkType = getSelectedCheckType();
            var frontingType = getSelectedFrontingType();
            var conf = getSelectedV2rayConfig();
            var timeout = getDownloadTimeout();

            addTextLog($"Start testing {selectedIPs.Length} IPs for {rounds} rounds..." + Environment.NewLine +
                $"\tTest spec: download size: {dlSpeed.getTargetFileSizeInt(timeout) / 1000} KB in {timeout} seconds." + Environment.NewLine);

            if (dlSpeed.isSpeedZero())
            {
                addTextLog("** Warning: Testing in NO VPN mode. Choose a target download speed from above settings so we can test base on that target speed.");
            }

            btnStopAvgTest.Visible = true;
            btnStopAvgTest.Enabled = true;

            Task.Factory.StartNew(() =>
            {
                for (int i = 0; i < selectedIPs.Length; i++)
                {
                    var ip = selectedIPs[i];
                    testAvgSingleIP(ip, rounds, dlSpeed, upSpeed, conf, timeout, checkType, frontingType);

                    // stop requested
                    if (stopAvgTetingIsRequested)
                        break;
                }
            })
            .ContinueWith(done =>
            {
                if (stopAvgTetingIsRequested)
                    addTextLog("Test stopped by user.");
                else
                    addTextLog("Test finished.");
                isManualTesting = false;
                stopAvgTetingIsRequested = false;
            });

        }

        private void mnuTesIP2Times_Click(object sender, EventArgs e)
        {
            testSelectedIPAddresses(2);
        }

        private void mnuTesIP3Times_Click(object sender, EventArgs e)
        {
            testSelectedIPAddresses(3);
        }

        private void mnuTesIP5Times_Click(object sender, EventArgs e)
        {
            testSelectedIPAddresses(5);
        }

        private void btnResultsActions_Click(object sender, EventArgs e)
        {

        }

        private void testThisIPAddressToolStripMenuItem_Click(object sender, EventArgs e)
        {
            testSelectedIP();
        }

        private void testSelectedIP()
        {
            var ip = getSelectedIPAddress();

            if (ip != null)
            {
                testAvgSingleIP(ip, 1, getDownloadTargetSpeed(), getUploadTargetSpeed(), getSelectedV2rayConfig(), getDownloadTimeout(), getSelectedCheckType(), getSelectedFrontingType());
            }
        }

        private void btnStopAvgTest_Click(object sender, EventArgs e)
        {
            stopAvgTetingIsRequested = true;
            btnStopAvgTest.Enabled = false;
        }

        // Global Hotkeys
        private void frmMain_KeyDown(object sender, KeyEventArgs e)
        {
            // start stop
            if (e.Control && e.KeyCode == Keys.F5)
            {
                startStopScan(ScanType.SCAN_CLOUDFLARE_IPS);
            }

            // skip
            if (e.Control && e.KeyCode == Keys.N)
            {
                if (scanEngine.progressInfo.isScanRunning)
                    scanEngine.skipCurrentIPRange();
            }
        }

        // show scan status
        private void mnushowScanStatus_Click(object sender, EventArgs e)
        {
            var sInf = scanEngine.progressInfo;
            string status = "";
            if (sInf.isScanRunning)
            {
                status = $"Found {sInf.scanResults.totalFoundWorkingIPs} working IPs out of {sInf.totalCheckedIP} scanned IPs." + Environment.NewLine +
                    $"{sInf.getCurrentRangePercentIsDone():f0}% of current range is done. This is {sInf.currentIPRangesNumber} of total {sInf.totalIPRanges} IP ranges.";
            }
            else
            {
                status = "Scan is not running.";
            }

            addTextLog(status);
            sendScreenReaderMsg(status, false, AutomationNotificationProcessing.ImportantAll);
        }

        private void btnResultsActions_KeyDown(object sender, KeyEventArgs e)
        {
            if (e.KeyCode == Keys.Space)
            {
                showResultsActionsMenu();
            }
        }

        private void btnResultsActions_KeyPress(object sender, KeyPressEventArgs e)
        {

        }

        private void frmMain_KeyPress(object sender, KeyPressEventArgs e)
        {

        }

        private void listResults_SelectedIndexChanged(object sender, EventArgs e)
        {
            //sendScreenReaderMsg(getSelectedIPAddress() ?? "");
        }

        private void comboTargetSpeed_SelectedIndexChanged(object sender, EventArgs e)
        {
            if (comboDLTargetSpeed.SelectedIndex == 0)
            {
                addTextLog("By selecting this option we won't test download speed via VPN and just quickly return all resolvable IPs.");
            }
        }

        // ********************
        // ***** Diagnose *****
        // ********************
        private void mnuDiagnoseRandomIP_Click(object sender, EventArgs e)
        {
            doRandomIPDiagnose();
        }

        private void doRandomIPDiagnose()
        {
            // get a random ip range
            var allIPs = IPAddressExtensions.getAllIPInRange(getCheckedCFIPList(false, true).First<string>());
            Random rnd = new Random();
            // get a random ip in the ip range
            doDiagnose(allIPs[rnd.Next(allIPs.Count)]);
        }

        // user entered ip
        private void mnuDiagnoseWithUserEnteredIP_Click(object sender, EventArgs e)
        {
            string ipAddr;
            if (!isScanRunningOrPaused() && getIPFromUser(out ipAddr, "Diagnose with IP Address"))
            {
                doDiagnose(ipAddr);
            }
        }

        // selected ip
        private void diagnoseWithThisIPAddressToolStripMenuItem_Click(object sender, EventArgs e)
        {
            var selectedIP = getSelectedIPAddress();
            if (selectedIP != null)
            {
                doDiagnose(selectedIP);
            }
        }

        private void doDiagnose(string ipAddress)
        {
            if (isScanRunningOrPaused())
            {
                addTextLog("Can not diagnose while scanning or paused.");
                return;
            }

            diagnoseIPAddress = ipAddress;

            startStopScan(ScanType.DIAGNOSING);
        }

        private void mnuAddIPToList_Click(object sender, EventArgs e)
        {
            string ipAddr;
            if (getIPFromUser(out ipAddr, "Add IP To List"))
            {
                listResults.Items.Add(new ListViewItem(new string[] { ipAddr, "0", "0" }));
                currentScanResults.Add(new ResultItem(0, 0, ipAddr));
            }
        }

        private void mnuPauseScan_Click(object sender, EventArgs e)
        {
            if (isScanRunning())
                scanEngine.pause();
            else if (isScanPaused())
            {
                // stop scan from pause
                scanEngine.progressInfo.scanStatus = ScanStatus.STOPPED;
                scanEngine.progressInfo.resumeRequested = false;
                updateUIControls(false);

                addTextLog("Paused scan is stopped.", true);
            }
        }
    }

}