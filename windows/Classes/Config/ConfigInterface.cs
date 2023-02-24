using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace WinCFScan.Classes.Config
{
    internal interface ConfigInterface
    {
        bool isConfigValid();
        bool load();
    }
}
