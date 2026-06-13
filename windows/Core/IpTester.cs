using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace WinCFScan.Core
{
    internal class IpTester
    {
        public string TestIp(string ip)
        {
            string baseDir =
                AppDomain.CurrentDomain.BaseDirectory;

            string originalConfig =
                Path.Combine(
                    baseDir,
                    "generated",
                    "generated.json"
                );

            string testConfig =
                Path.Combine(
                    baseDir,
                    "generated",
                    "test.json"
                );

            string json =
                File.ReadAllText(
                    originalConfig
                );

            json = json.Replace(
                "IP.IP.IP.IP",
                ip
            );

            File.WriteAllText(
                testConfig,
                json
            );

            string v2ray =
                Path.Combine(
                    baseDir,
                    "bin",
                    "v2ray.exe"
                );

            ProcessStartInfo psi =
                new ProcessStartInfo
                {
                    FileName = v2ray,

                    Arguments =
                        $"test -config \"{testConfig}\"",

                    RedirectStandardOutput = true,
                    RedirectStandardError = true,

                    UseShellExecute = false,
                    CreateNoWindow = true
                };

            using Process p =
                Process.Start(psi);

            string output =
                p.StandardOutput.ReadToEnd();

            output +=
                p.StandardError.ReadToEnd();

            p.WaitForExit();

            return output;
        }
    }
}
