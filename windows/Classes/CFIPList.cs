using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace WinCFScan.Classes
{
    // load cloudflare ip ranges from 'cf.local.iplist' file
    internal class CFIPList
    {
        protected string[] ipList { get; set; }

        public CFIPList(string fileName = "cf.local.iplist")
        {
            loadList(fileName);
        }

        private bool loadList(string fileName)
        {
            if (!File.Exists(fileName))
            {
                return false;
            }

            string fileData = File.ReadAllText(fileName);

            ipList = fileData.Split();

            return true;
        }

        public string[] getIPList()
        {
            return ipList;
        }

        public bool isIPListValid()
        {
            return ipList != null && ipList.Length > 0;
        }
    }
}
