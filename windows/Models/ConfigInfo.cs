using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace WinCFScan.Models
{
    internal class ConfigInfo
    {

        public string Protocol { get; set; } = "";

        public string UUID { get; set; } = "";

        public string Password { get; set; } = "";

        public string Host { get; set; } = "";

        public string SNI { get; set; } = "";

        public string Path { get; set; } = "";

        public int Port { get; set; }

        public string Network { get; set; } = "ws";

        public string Security { get; set; } = "tls";
    }
}
