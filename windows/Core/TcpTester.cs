using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Net.Sockets;
using System.Text;
using System.Threading.Tasks;

namespace WinCFScan.Core
{
    internal class TcpTester
    {

        public string Run(
           string host,
           int port)
        {
            try
            {
                Stopwatch sw =
                    Stopwatch.StartNew();

                using TcpClient client =
                    new TcpClient();

                IAsyncResult result =
                    client.BeginConnect(
                        host,
                        port,
                        null,
                        null);

                bool success =
                    result.AsyncWaitHandle
                        .WaitOne(5000);

                if (!success)
                {
                    return
                        "TCP TIMEOUT";
                }

                client.EndConnect(result);

                sw.Stop();

                return
                    $"TCP OK\r\n" +
                    $"Port: {port}\r\n" +
                    $"Delay: {sw.ElapsedMilliseconds} ms";
            }
            catch (Exception ex)
            {
                return
                    $"TCP FAILED\r\n" +
                    ex.Message;
            }
        }

    }
}
