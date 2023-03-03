using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace WinCFScan.Classes
{
    internal class ScanSpeed
    {
        private int targetSpeed;

        public ScanSpeed(int speed)
        {
            targetSpeed = speed;
        }

        public string getTargetFileSize(int dlDuration = 2)
        {
            return (targetSpeed * 1000 * dlDuration).ToString();
        }

        public int getTargetSpeed()
        {
            return targetSpeed * 1000;
        }
    }
}
