using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using WinCFScan.Classes.Config;

namespace WinCFScan.Classes.Checker
{
    internal class DownloadChecker : AbstractChecker
    {
        public DownloadChecker(CheckSettings checkSettings) : base(checkSettings)
        {
        }

        public override bool check()
        {
            // note: 'cs' mean checkSettings

            Tools.logStep(Environment.NewLine + "----- Download Test -----", cs.isDiagnosing);
            Tools.logStep($"Start check dl speed, proxy port: {cs.port}, timeout: {cs.timeout} sec, target speed: {cs.targetSpeed.getTargetSpeed():n0} b/s", cs.isDiagnosing);
            
            Stopwatch sw = new Stopwatch();

            try
            {
                sw.Start();
                Tools.logStep($"Starting dl url: {cs.targetUrl}", cs.isDiagnosing);
                var data = client.GetStringAsync(cs.targetUrl).Result;

                Tools.logStep($"*** Download success in {sw.ElapsedMilliseconds:n0} ms, dl size: {data.Length:n0} bytes for IP {cs.ip}", cs.isDiagnosing);

                return data.Length == cs.targetSpeed.getTargetSpeed() * cs.timeout;
            }
            catch (Exception ex)
            {
                this.handleException(ex);

                return false;
            }
            finally
            {
                checkDuration = sw.ElapsedMilliseconds;
                if (checkDuration > cs.timeout * 1000 + 500)
                {
                    Tools.logStep($"Download took too long! {checkDuration:n0} ms for IP {cs.ip}", cs.isDiagnosing);
                }

                client.Dispose();
            }
        }
    }
}
