using System;
using System.Collections.Concurrent;
using System.Diagnostics;
using System.Net;
using System.Security.Policy;
using System.Windows.Forms;
using WinCFScan.Classes;
using WinCFScan.Classes.Config;
using WinCFScan.Classes.HTTPRequest;
using WinCFScan.Classes.IP;
using static System.Net.Mime.MediaTypeNames;

namespace WinCFScan
{
    public partial class frmMain : Form
    {
        private const string ourGitHubUrl = "https://github.com/MortezaBashsiz/CFScanner";
        ConfigManager configManager;
        bool oneTimeChecked = false; // config checked once?
        ScanEngine scanEngine;
        private List<ResultItem> currentScanResults = new();
        private bool scanFinshed = false;
        private bool isUpdatinglistCFIP;
        private bool isAppCongigValid = true;
        private ListViewColumnSorter listResultsColumnSorter;
        private ListViewColumnSorter listCFIPsColumnSorter;
        private Version appVersion;
        private AppUpdateChecker appUpdateChecker;

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

            scanEngine = new ScanEngine();

            loadLastResultsComboList();
            comboTargetSpeed.SelectedIndex = 2;

            appVersion = AppUpdateChecker.getCurrentVersion();

            DateTime buildDate = new DateTime(2000, 1, 1)
                                    .AddDays(appVersion.Build).AddSeconds(appVersion.Revision * 2);
            string displayableVersion = $" - {appVersion}";
            this.Text += displayableVersion;

            appUpdateChecker = new AppUpdateChecker();

            // is debug mode enable? this line should be at bottom line
            checkEnableDebugMode();
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
                    $"Cpu Arch: {System.Runtime.InteropServices.RuntimeInformation.ProcessArchitecture}, Framework: {System.Runtime.InteropServices.RuntimeInformation.FrameworkDescription}";
                Tools.logStep($"\n\nApp started. Version: {appVersion}\n{systemInfo}");
            }
        }

        // add text log to log textbox
        delegate void SetTextCallback(string log);
        public void addTextLog(string log)
        {
            try
            {
                if (this.txtLog.InvokeRequired)
                {
                    SetTextCallback d = new SetTextCallback(addTextLog);
                    this.Invoke(d, new object[] { log });
                }
                else
                {
                    txtLog.AppendText(log + Environment.NewLine);
                }
            }
            catch (Exception)
            {
            }
        }


        private void btnScanInPrevResults_Click(object sender, EventArgs e)
        {
            startStopScan(true);
        }

        private void btnStart_Click(object sender, EventArgs e)
        {
            
        }

        private void startStopScan(bool inPrevResult = false)
        {
            if (!isAppCongigValid)
            {
                showCanNotContinueMessage();
                return;
            }

            if (isScanRunning())
            {
                // stop scan
                btnStart.Enabled= false;
                waitUntilScannerStoped();
                updateUIControlls(false);
            }
            else
            {   // start scan
                if (inPrevResult)
                {
                    if (currentScanResults.Count == 0)
                    {
                        addTextLog("Current result list is empty!");
                        return;
                    }
                    addTextLog($"Start scanning {currentScanResults.Count} IPs in previous results...");
                }
                else
                {
                    // set cf ip list to scan engine
                    string[] ipRanges = getCheckedCFIPList();
                    if (ipRanges.Length == 0)
                    {
                        tabControl1.SelectTab(0);
                        MessageBox.Show($"No Cloudflare IP ranges are selected. Please select some IP ranges.",
                             "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                        return;
                    }
                    currentScanResults = new();
                    scanEngine.setCFIPRangeList(ipRanges);
                    addTextLog($"Start scanning {ipRanges.Length} Cloudflare IP ranges...");
                }

                updateUIControlls(true);
                scanEngine.workingIPsFromPrevScan = inPrevResult ? currentScanResults : null;
                 scanEngine.targetSpeed = getTargetSpeed(); // set target speed
                var scanType = inPrevResult ? ScanType.SCAN_IN_PERV_RESULTS : ScanType.SCAN_CLOUDFLARE_IPS;

                // start scan job in new thread
                Task.Factory.StartNew(() => scanEngine.start(scanType))
                    .ContinueWith(done =>
                    {
                        scanFinshed = true;
                        this.currentScanResults = scanEngine.progressInfo.scanResults.workingIPs;
                        addTextLog($"{scanEngine.progressInfo.totalCheckedIP:n0} IPs tested and found {scanEngine.progressInfo.scanResults.totalFoundWorkingIPs:n0} working IPs.");
                    });

                tabControl1.SelectTab(1);
            }
        }

        private void updateUIControlls(bool isStarting)
        {
            if (isStarting)
            {
                loadLastResultsComboList();
                listResults.Items.Clear();
                btnStart.Text = "Stop Scan";
                btnScanInPrevResults.Enabled = false;
                btnResultsActions.Enabled = false;
                comboConcurrent.Enabled = false;
                comboTargetSpeed.Enabled = false;
                timerProgress.Enabled = true;
                btnSkipCurRange.Enabled = true;
                comboResults.Enabled = false;
                tabPageCFRanges.Enabled = false;
            }
            else
            {   // is stopping
                btnStart.Text = "Start Scan";
                btnStart.Enabled = true;
                btnScanInPrevResults.Enabled = true;
                btnResultsActions.Enabled = true;
                timerProgress.Enabled = false;
                btnSkipCurRange.Enabled = false;
                comboResults.Enabled = true;
                if (!configManager.enableDebug)
                {
                    comboConcurrent.Enabled = true;
                }
                comboTargetSpeed.Enabled = true;
                tabPageCFRanges.Enabled = true;

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
                    // delete result file if there is no woriking ip
                    scanResults.remove();
                }

                loadLastResultsComboList();

                // sort results list
                listResultsColumnSorter.Order = SortOrder.Ascending;
                listResultsColumnSorter.SortColumn = 0;
                listResults.Sort();
            }
        }

        private void timerBase_Tick(object sender, EventArgs e)
        {
            oneTimeChecks();
            if (scanFinshed)
            {
                scanFinshed = false;
                updateConrtolsProgress(true);
                updateUIControlls(false);
            }
        }

        private void timerProgress_Tick(object sender, EventArgs e)
        {
            updateConrtolsProgress();
        }

        private void updateConrtolsProgress(bool forceUpdate = false)
        {
            var pInf = scanEngine.progressInfo;
            if (isScanRunning() || forceUpdate)
            {
                lblLastIPRange.Text = $"Current IP range: {pInf.currentIPRange} ({pInf.currentIPRangesNumber:n0}/{pInf.totalIPRanges:n0})";
                labelLastIPChecked.Text = $"Last checked IP:  {pInf.lastCheckedIP} ({pInf.totalCheckedIPInCurIPRange:n0}/{pInf.currentIPRangeTotalIPs:n0})";
                lblTotalWorkingIPs.Text = $"Total working IPs found:  {pInf.scanResults.totalFoundWorkingIPs:n0}";
                if (pInf.scanResults.fastestIP != null)
                {
                    txtFastestIP.Text = $"{pInf.scanResults.fastestIP.ip}  -  {pInf.scanResults.fastestIP.delay:n0} ms";
                }

                prgOveral.Maximum = pInf.totalIPRanges;
                prgOveral.Value = pInf.currentIPRangesNumber;

                prgCurRange.Maximum = pInf.currentIPRangeTotalIPs;
                prgCurRange.Value = pInf.totalCheckedIPInCurIPRange;
                fetchWorkingIPResults();
                pInf.scanResults.autoSave();
            }
        }

        // fetch new woriking ips and add to the list view while scanning
        private void fetchWorkingIPResults()
        {
            List<ResultItem> scanResults = scanEngine.progressInfo.scanResults.fetchWorkingIPs();
            addResulItemsToListView(scanResults);
        }

        private void btnSkipCurRange_Click(object sender, EventArgs e)
        {
            scanEngine.skipCurrentIPRange();
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
                foreach (ResultItem resultItem in workingIPs)
                {
                    listResults.Items.Add(new ListViewItem(new string[] { resultItem.delay.ToString(), resultItem.ip }));
                }
                lblPrevListTotalIPs.Text = $"{listResults.Items.Count:n0} IPs";
            }
        }


        private void oneTimeChecks()
        {
            if (!oneTimeChecked && isAppCongigValid)
            {
                //Load cf ip ranges
                loadCFIPListView();
               

                // check if client config file is exists and update
                if (configManager.getClientConfig() != null && configManager.getClientConfig().isClientConfigOld())
                {
                    addTextLog("Updating client config from remote...");
                    bool result = configManager.getClientConfig().remoteUpdateClientConfig();
                    if (result)
                    {
                        addTextLog("'client config' is successfully updated.");
                        if (!configManager.getClientConfig().isConfigValid())
                        {
                            addTextLog("'client config' data is not valid!");
                        }
                    }
                    else
                    {
                        addTextLog("Failed to update client config. check your internet connection or maybe client config url is blocked by your ISP!");
                    }
                }

                // check fronting domain
                Task.Factory.StartNew(() =>
                {
                    var checker = new CheckIPWorking();
                    if (!checker.checkFronting(false, 5))
                    {
                        addTextLog($"Fronting domain is not accessible! you might need to get new fronting url from our github or check your internet connection.");
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
                else if(appUpdateChecker.updateCheckResult == UpdateCheckResult.HasError)
                {
                    addTextLog("Something went wrong while checking for update!");
                }
                else if(logNoNewVersion)
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
            try
            {
                if (scanEngine.progressInfo.scanResults?.fastestIP != null)
                {
                    Clipboard.SetText(scanEngine.progressInfo.scanResults.fastestIP.ip);
                }
            }
            catch (Exception ex) { }
        }

        // results actions
        private void btnResultsActions_MouseClick(object sender, MouseEventArgs e)
        {
           mnuResultsActions.Show(this, btnResultsActions.Left + 5 , splitContainer1.Top +  btnResultsActions.Top + btnResultsActions.Height+ 25);
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
            if (isScanRunning())
                return;

            openFileDialog1.Title = "Import IP results";
            var result = openFileDialog1.ShowDialog();

            if (result == DialogResult.OK)
            {
                fillResultsListView(openFileDialog1.FileName, true);
                tabControl1.SelectedIndex = 1;
           };

        }

        private void deleteResultItem()
        {
            if (isScanRunning())
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
        private string[] getCheckedCFIPList(bool getIPCount = false)
        {
            return listCFIPList.CheckedItems.Cast<ListViewItem>()
                                 .Select(item =>
                                 {
                                     return getIPCount ? item.SubItems[1].Text : item.SubItems[0].Text;
                                 })
                                 .ToArray<string>();


        }

        // list view right click
        private void listResults_MouseClick(object sender, MouseEventArgs e)
        {
            if (e.Button == MouseButtons.Right)
            {
                mnuListViewCopyIP.Text = "Copy IP Address " + getSelectedIPAddress();
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
            listCFIPList.BeginUpdate();
            isUpdatinglistCFIP = true;
            uint totalIPs = 0;
            addTextLog($"Loading Cloudflare IPs ranges...");

            foreach (var ipRange in scanEngine.ipListLoader.validIPRanges)
            {
                var lvwItem = listCFIPList.Items.Add(new ListViewItem(new string[] { ipRange.rangeText, $"{ipRange.totalIps:n0}" }));
                lvwItem.Checked= true;
            }

            listCFIPList.EndUpdate();
            isUpdatinglistCFIP = false;
            addTextLog($"Total {scanEngine.ipListLoader.totalIPs:n0} Cloudflare IPs are ready to be scanned.");
            updateCFIPListStatusText();
        }


        private void mnuListViewTestThisIPAddress_Click(object sender, EventArgs e)
        {
            var IPAddress = getSelectedIPAddress();
            if (IPAddress != null)
            {
                testSingleIP(IPAddress);
            }
        }

        private void testSingleIP(string IPAddress)
        {
            addTextLog($"Testing {IPAddress} ...");
            var checker = new CheckIPWorking(IPAddress, getTargetSpeed());
            var success = checker.check();
            if (success)
                addTextLog($"{IPAddress} is working. Delay: {checker.downloadDuration:n0} ms.");
            else
                addTextLog($"{IPAddress} is NOT working.");
        }

        private ScanSpeed getTargetSpeed()
        {
            int speed = int.Parse(comboTargetSpeed.SelectedItem.ToString().Replace(" KB/s", ""));
            return new ScanSpeed(speed);
        }

        private string? getSelectedIPAddress()
        {
            try
            {
                return listResults.SelectedItems[0].SubItems[1].Text;
            }
            catch (Exception ex) { }

            return null;
        }

        private void listResults_MouseDoubleClick(object sender, MouseEventArgs e)
        {
            var IPAddress = getSelectedIPAddress();
            if (IPAddress != null)
            {
                testSingleIP(IPAddress);
            }
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
            addTextLog("Exiting...");
            waitUntilScannerStoped();
        }

        private void waitUntilScannerStoped()
        {
            if (isScanRunning())
            {
                // stop scan
                scanEngine.stop();
                do
                {
                    System.Windows.Forms.Application.DoEvents();
                    Thread.Sleep(100);
                } while (isScanRunning());

            }
        }

        private void linkGithub_LinkClicked(object sender, LinkLabelLinkClickedEventArgs e)
        {
            try
            {
                ProcessStartInfo sInfo = new ProcessStartInfo(ourGitHubUrl) { UseShellExecute = true };
                Process.Start(sInfo);
            }
            catch (Exception)
            {
                addTextLog($"Visit us on {ourGitHubUrl}");
                throw;
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
            string input = Tools.ShowDialog("Enter a valid IP address:", "Test Sigle IP Address");

            if(input == "" || input == null) { return; }

            if(! IPAddressExtensions.isValidIPAddress(input))
            {
                // msg
                MessageBox.Show("Invalid IP address is entered!", "Error", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return;
            }

           testSingleIP(input);
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
                catch (Exception ex) {
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
            if (isScanRunning())
                return;

            openFileDialog1.Title = "Load custom cloudflare IP ranges";
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
            var resultIPs = isScanRunning() ? scanEngine.progressInfo.scanResults.workingIPs : currentScanResults;
            if (resultIPs.Count == 0)
            {
                addTextLog("Current results list is empty!");
                return;
            }

            saveFileDialog1.Title = $"Saving {resultIPs.Count} IP addressses";
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
            return scanEngine.progressInfo.isScanRunning;
        }

        private void exitToolStripMenuItem_Click(object sender, EventArgs e)
        {
            System.Windows.Forms.Application.Exit();
        }

        private void importScanResultsToolStripMenuItem_Click(object sender, EventArgs e)
        {
            importResults();
        }

        private void comboTargetSpeed_SelectedIndexChanged(object sender, EventArgs e)
        {

        }

        private void mnuPauseScan_Click(object sender, EventArgs e)
        {

        }

        private void btnStart_ButtonClick(object sender, EventArgs e)
        {
            startStopScan(false);
        }

        private void checkForUpdateToolStripMenuItem_Click(object sender, EventArgs e)
        {
            addTextLog("Checking for new version...");
            checkForUpdate(true);
        }
    }
}