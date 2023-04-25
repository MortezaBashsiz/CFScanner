using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Security.Policy;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace WinCFScan.Classes.Checker
{
    internal class UploadChecker : AbstractChecker
    {
        public UploadChecker(CheckSettings checkSettings) : base(checkSettings)
        {
        }

        public override bool check()
        {
            // note: 'cs' mean checkSettings

            Tools.logStep(Environment.NewLine + "----- Upload Test -----", cs.isDiagnosing);
            Tools.logStep($"Start check up speed, proxy port: {cs.port}, timeout: {cs.timeout} sec, target speed: {cs.targetSpeed.getTargetSpeed():n0} b/s", cs.isDiagnosing);

            Stopwatch sw = new Stopwatch();

            try
            {
                sw.Start();
                Tools.logStep($"Starting up url: {cs.targetUrl}", cs.isDiagnosing);

                //upload
                int uploadSize = cs.targetSpeed.getTargetFileSizeInt(cs.timeout);
                HttpContent c = new StringContent(new string('*', uploadSize), Encoding.UTF8, "text/plain");
                var data = client.PostAsync(cs.targetUrl, c).Result;

                //string responseString = data.Content.ReadAsStringAsync().Result;

                Tools.logStep($"*** Upload success in {sw.ElapsedMilliseconds:n0} ms, upload size: {uploadSize:n0} bytes for IP {cs.ip}", cs.isDiagnosing);

                return data.IsSuccessStatusCode;

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
                    Tools.logStep($"Upload took too long! {checkDuration:n0} ms for IP {cs.ip}", cs.isDiagnosing);
                }

                client.Dispose();
            }
        }
    }
}
