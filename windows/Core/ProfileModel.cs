using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace WinCFScan.Core
{
    internal class ProfileModel
    {

        public string ProfileName { get; set; } = "";

        public string UUID { get; set; } = "";

        public string Host { get; set; } = "";

        public string SNI { get; set; } = "";

        public string Path { get; set; } = "";

        public int Port { get; set; }

       
    }
}
