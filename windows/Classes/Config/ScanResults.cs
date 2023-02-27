using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace WinCFScan.Classes.Config
{
    internal class ScanResults

    {
        public string? resultsFileName;
        private ScanResults? loadedInstance;

        public DateTime startDate { get; set; }
        public DateTime endDate { get; set; }        
        public int totalFoundWorkingIPs { get; set; }
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

        public bool save(bool sortBeforeSave = true)
        {
            try
            {
                if (sortBeforeSave)
                {
                    workingIPs = this.workingIPs.OrderBy(x => x.delay).ToList<ResultItem>();
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
                    workingIPs = this.workingIPs.OrderBy(x => x.delay).ToList<ResultItem>();
                }

                var plain = workingIPs.Select(x => $"{x.delay} - {x.ip}").ToArray<string>();
                File.WriteAllText(resultsFileName, String.Join("\n", plain));
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

        public void addIPResult(long delay, string ip )
        {
            ResultItem resultItem = new ResultItem(delay, ip);
            workingIPs.Add(resultItem);
            unFetchedWorkingIPs.Add(new ResultItem(delay, ip));
            thereIsNewWorkingIPs = true;
            totalFoundWorkingIPs++;
            totalUnsavedWorkingIPs++;
            if (fastestIP == null || resultItem.delay < fastestIP.delay)
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
        public long delay { get; set; }
        public string ip { get; set; }

        public ResultItem(long delay = 0, string ip = "")
        {
            this.delay = delay;
            this.ip = ip;
        }
    }
}
