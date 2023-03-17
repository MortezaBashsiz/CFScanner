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
            return getTargetFileSizeInt(dlDuration).ToString();
        } 
        
        public int getTargetFileSizeInt(int dlDuration = 2)
        {
            return (targetSpeed * 1000 * dlDuration);
        }

        public int getTargetSpeed()
        {
            return targetSpeed * 1000;
        }

        public bool isSpeedZero()
        {
            return targetSpeed == 0;
        }
    }
}
