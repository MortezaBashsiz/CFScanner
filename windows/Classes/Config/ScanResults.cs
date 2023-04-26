using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using WinCFScan.Classes.IP;

namespace WinCFScan.Classes.Config
{
    internal class ScanResults

    {
        public string? resultsFileName;
        private ScanResults? loadedInstance;

        public DateTime startDate { get; set; }
        public DateTime endDate { get; set; }        
        public int totalFoundWorkingIPs { get; set; }
        public int totalFoundWorkingIPsCurrentRange { get; set; }
        public ResultItem fastestIP;
        public List<ResultItem> workingIPs { get; set; }

        private List<ResultItem> unFetchedWorkingIPs { get; set; }
        private bool thereIsNewWorkingIPs = false;
        private int totalUnsavedWorkingIPs = 0;


        public ScanResults(string fileName) {
            workingIPs = new List<ResultItem>();
            unFetchedWorkingIPs = new List<ResultItem>();
            resultsFileName = fileName;
        }
        
        public ScanResults(List<ResultItem> workingIPs, string resultsFileName) {
            this.workingIPs = workingIPs;
            unFetchedWorkingIPs = new List<ResultItem>();
            this.resultsFileName = resultsFileName;
            totalFoundWorkingIPs= workingIPs.Count; 
        }

        public ScanResults() : this("") {}

        // load app config
        public bool load()
        {
            try
            {
                if (!File.Exists(resultsFileName))
                {
                    return false;
                }

                string jsonString = File.ReadAllText(resultsFileName);
                loadedInstance = JsonSerializer.Deserialize<ScanResults>(jsonString)!;

            }
            catch (Exception ex)
            {
                Tools.logStep($"ScanResults.load() had exception: {ex.Message}");
                return false;
            }

            return true;
        }

        public bool loadPlain()
        {
            try
            {
                if (!File.Exists(resultsFileName) || (new FileInfo(resultsFileName)).Length > 2 * 1_000_000)
                {
                    return false;
                }

                string plainString = File.ReadAllText(resultsFileName);
                long DLDelay = 0, UPDelay = 0; string ip;
                
                var lines = plainString.Split(Environment.NewLine);

                if (lines.Length == 1 ) { // support old format: LF only
                    lines = plainString.Split("\n");
                }

                foreach (var line in lines)
                {
                    ip = line;
                    // DL     UP    IP
                    // 1023 - 902 - 192.168.1.2
                    if(line.Contains(" - "))
                    {
                        var splited = line.Split(" - ");
                        if (splited.Length == 3)
                        {
                            long.TryParse(splited[0], out DLDelay);
                            long.TryParse(splited[1], out UPDelay);
                            ip = splited[2];
                        }
                        else if(splited.Length == 2) // old format, dl only
                        {
                            long.TryParse(splited[0], out DLDelay);
                            ip = splited[1];
                        }
                    }

                    if(IPAddressExtensions.isValidIPAddress(ip))
                    {
                        this.addIPResult(DLDelay, UPDelay, ip);
                    }

                }
                loadedInstance = this;

            }
            catch (Exception ex)
            {
                Tools.logStep($"ScanResults.loadPlain() had exception: {ex.Message}");
                return false;
            }

            return this.totalFoundWorkingIPs > 0;
        }

        public bool save(bool sortBeforeSave = true)
        {
            try
            {
                if (sortBeforeSave)
                {
                    workingIPs = this.workingIPs.OrderBy(x => x.downloadDelay).ToList<ResultItem>();
                }
                endDate = DateTime.Now;
                JsonSerializerOptions options= new JsonSerializerOptions();
                options.WriteIndented= true;
                string jsonString = JsonSerializer.Serialize<ScanResults>(this, options);
                File.WriteAllText(resultsFileName, jsonString);
                totalUnsavedWorkingIPs = 0;
            }
            catch (Exception ex)
            {
                Tools.logStep($"ScanResults.save() had exception: {ex.Message}");

                return false;
            }

            return true;
        }

        public bool savePlain(bool sortBeforeSave = true)
        {
            try
            {
                if (sortBeforeSave)
                {
                    workingIPs = this.workingIPs.OrderBy(x => x.downloadDelay).ToList<ResultItem>();
                }

                var plain = workingIPs.Select(x => $"{x.downloadDelay} - {x.uploadDelay} - {x.ip}").ToArray<string>();
                File.WriteAllText(resultsFileName, String.Join(Environment.NewLine, plain));
            }
            catch (Exception ex)
            {
                Tools.logStep($"ScanResults.savePlain() had exception: {ex.Message}");

                return false;
            }

            return true;
        }

        public ScanResults? getLoadedInstance()
        {
            return loadedInstance;
        }

        public void addIPResult(long downloadDelay, long uploadDelay, string ip )
        {
            ResultItem resultItem = new ResultItem(downloadDelay, uploadDelay, ip, downloadDelay);
            workingIPs.Add(resultItem);
            unFetchedWorkingIPs.Add(new ResultItem(downloadDelay, uploadDelay, ip, downloadDelay));
            thereIsNewWorkingIPs = true;
            totalFoundWorkingIPs++;
            totalFoundWorkingIPsCurrentRange++;
            totalUnsavedWorkingIPs++;
            if (fastestIP == null || resultItem.downloadDelay < fastestIP.downloadDelay || resultItem.uploadDelay < fastestIP.uploadDelay)
            {
                fastestIP = resultItem;
            }
        }

        public void autoSave(int threshold = 10)
        {
            if(totalUnsavedWorkingIPs >= threshold) {
                save(true);
            }
        }

        public List<ResultItem>? fetchWorkingIPs()
        {
            if (thereIsNewWorkingIPs)
            {
                List<ResultItem> returnResult = unFetchedWorkingIPs;
                unFetchedWorkingIPs = new List<ResultItem>();
                thereIsNewWorkingIPs = false;
                return returnResult;
            }

            return null;
        }

        internal void remove()
        {
            try
            {
                File.Delete(this.resultsFileName);
            }
            catch(Exception ex) { }
        }
    }


    internal class ResultItem
    {
        public long delay { get; set; } // keeping it only for compatibility reasons
        public long downloadDelay { get; set; }
        public long uploadDelay { get; set; }
        public string ip { get; set; }

        public ResultItem(long downloadDelay = 0, long uploadDelay = 0, string ip = "", long delay = 0)
        {
            this.ip = ip;

            // for compatibility of old releases which only have delay field
            if (delay != 0 && downloadDelay ==0  && uploadDelay == 0)
            {
                this.delay = delay;
                this.downloadDelay = delay;
                return;
            }

            this.delay = downloadDelay; // remove it on future releases
            this.downloadDelay = downloadDelay;
            this.uploadDelay = uploadDelay;
            
        }
    }
}
