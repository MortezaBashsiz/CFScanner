using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.NetworkInformation;
using System.Text;
using System.Threading.Tasks;

namespace WinCFScan.Core
{
    internal class PingTester
    {

        public string Run(string host)
        {
            try
            {
                using Ping ping = new Ping();

                PingReply reply =
                    ping.Send(host, 5000);

                if (reply.Status ==
                    IPStatus.Success)
                {
                    return
                        $"PING OK\r\n" +
                        $"IP: {reply.Address}\r\n" +
                        $"Delay: {reply.RoundtripTime} ms";
                }

                return
                    $"PING FAILED\r\n" +
                    $"Status: {reply.Status}";
            }
            catch (Exception ex)
            {
                return
                    $"PING ERROR\r\n" +
                    ex.Message;
            }
        }
    }
}
