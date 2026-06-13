using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using static System.Runtime.InteropServices.JavaScript.JSType;

namespace WinCFScan.Core
{
    internal class TlsTester
    {
        public string Run(string domain)
        {
            try
            {
                

                if (!File.Exists("v2ray.exe"))
                {
                    return "v2ray.exe not found.";
                }

                ProcessStartInfo psi =
                    new ProcessStartInfo("v2ray.exe")
                    {
                       
                        Arguments = $"tls ping {domain}",

                        RedirectStandardOutput = true,
                        RedirectStandardError = true,

                        UseShellExecute = false,
                        CreateNoWindow = true
                    };

                using Process process =
                    Process.Start(psi);

                if (process == null)
                {
                    return "Unable to start v2ray.";
                }

                
                

                string output = process.StandardOutput.ReadToEnd();

                string error =process.StandardError.ReadToEnd();
                process.WaitForExit();

                return output + Environment.NewLine + error;
            }
            catch (Exception ex)
            {
                return
                    "TLS test failed." +
                    Environment.NewLine +
                    ex.Message;
            }
        }

    }
}
