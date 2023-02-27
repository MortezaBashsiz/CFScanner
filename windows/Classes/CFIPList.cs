using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Cryptography.X509Certificates;
using System.Text;
using System.Threading.Tasks;
using WinCFScan.Classes.IP;


namespace WinCFScan.Classes
{
    // load cloudflare ip ranges from 'cf.local.iplist' file
    internal class CFIPList
    {
        protected string[] ipList { get; set; }
        public List<RangeInfo> validIPRanges = new();
        public uint totalIPs = 0;

        public CFIPList(string fileName)
        {
            loadList(fileName);
        }

        private bool loadList(string fileName)
        {
            if (!File.Exists(fileName) && (new FileInfo(fileName)).Length < 2 * 1_000_000)
            {
                return false;
            }

            string fileData = File.ReadAllText(fileName);

            checkIPList(fileData.Split());

            return true;
        }

        private void checkIPList(string[] loadedIPList)
        {
            validIPRanges.Clear();
            totalIPs = 0;
            foreach (var ipRange in loadedIPList)
            {
                if (IPAddressExtensions.isValidIPRange(ipRange))
                {
                    var rangeTotalIPs = IPAddressExtensions.getIPRangeTotalIPs(ipRange);
                    if (rangeTotalIPs > 0)
                    {
                        this.validIPRanges.Add(new RangeInfo(ipRange, rangeTotalIPs));
                        totalIPs += rangeTotalIPs;
                    }
                }
            }
        }

        public string[] getIPList()
        {
            return validIPRanges.Select(x => x.rangeText).ToArray();
        }

        public bool isIPListValid()
        {
            return validIPRanges.Count > 0 && totalIPs > 0;
        }
    }

    public class RangeInfo{
        public string rangeText;
        public uint totalIps;

        public RangeInfo(string rangeText, uint totalIps)
        {
            this.rangeText = rangeText;
            this.totalIps = totalIps;
        }
    }

}
