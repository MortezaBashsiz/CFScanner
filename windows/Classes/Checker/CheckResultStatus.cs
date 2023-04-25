using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace WinCFScan.Classes.Checker
{
    internal class CheckResultStatus
    {
        private bool isDownloadSuccess = false;
        private bool isUploadSuccess = false;
        private CheckType checkType;

        public CheckResultStatus(CheckType scanType)
        {
            this.checkType = scanType;
        }

        // return true or false base on check type
        public bool isSuccess()
        {
            switch(checkType) {
                default:
                case CheckType.DOWNLOAD:
                    return isDownloadSuccess;
                case CheckType.UPLOAD:
                    return isUploadSuccess;
                case CheckType.BOTH:
                    return isDownloadSuccess && isUploadSuccess;
            }
        }

        public bool isDownSuccess()
        {
            return isDownloadSuccess;
        }

        public bool isUpSuccess()
        {
            return isUploadSuccess;
        }

        public void setDownloadSuccess()
        {
            this.isDownloadSuccess = true;
        }

        public void setUploadSuccess()
        {
            this.isUploadSuccess = true;
        }
    }
}
