using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using WinCFScan.Classes.Config;

namespace WinCFScan.Classes
{
    internal class AppUpdateChecker
    {
        private string localVersion;
        private string remoteVersion;

        private string remoteVersionUrl = "https://raw.githubusercontent.com/MortezaBashsiz/CFScanner/main/windows/Properties/latest-version";
        private string localVersionFileName = "local-version";
        private bool foundNewVersion = false;

        public static Version getCurrentVersion()
        {
            return System.Reflection.Assembly.GetExecutingAssembly().GetName().Version;
        }

        public AppUpdateChecker() {
            localVersion = getCurrentVersion().ToString();
        }

        // check for update
        public UpdateCheckResult check()
        {
            if(getRemoteVersion())
            {
                foundNewVersion = isNewVersionAvailable();
                return foundNewVersion ? UpdateCheckResult.NewVersionAvailable : UpdateCheckResult.NewVersionAvailable;
            }
            else
            {
                return UpdateCheckResult.HasError;
            }
        }

        public bool isFoundNewVersion()
        {
            return foundNewVersion;
        }

        public string getUpdateVersion()
        {
            return remoteVersion;
        }

        public bool shouldCheck()
        {
            return isFileOld(localVersionFileName);
        }

        // is there new version?
        private bool isNewVersionAvailable()
        {
            return isRemoteVersionValid() && compareVersions();
        }

        // is valid string with format v:1.0.xxx.xxx
        private bool isRemoteVersionValid()
        {
            return remoteVersion != null && remoteVersion.StartsWith("v:") && remoteVersion.Split(".").Length == 4;
        }

        // return true if remote version is higher
        private bool compareVersions() {

            var remoteV = remoteVersion.Substring(2).Split(".").Sum(p => Convert.ToUInt32(p));
            var localV = localVersion.Split(".").Sum(p => Convert.ToUInt32(p));

            return remoteV > localV;
        }

        private bool isFileOld(string file)
        {
            return !File.Exists(file) || (DateTime.Now - File.GetLastWriteTime(file)).TotalDays > 1;
        }

        private bool saveCurrentLocalVersion()
        {
            try
            {
                File.WriteAllText(localVersionFileName, localVersion);
                return true;
            }
            catch (Exception) { }
            return false;
        }

        private bool getRemoteVersion()
        {
            HttpClient client = new HttpClient();
            client.Timeout = TimeSpan.FromSeconds(5);

            try
            {
                remoteVersion = client.GetStringAsync(remoteVersionUrl).Result;

                if (isRemoteVersionValid())
                {
                    // whenever check remote version then update timestamp of local version file
                    // to prevent frequent remote checks
                    saveCurrentLocalVersion();

                    return true;
                }                
            }
            catch (Exception ex)
            {
                Tools.logStep($"getRemoteVersion had exception: {ex.Message}");
            }
            finally
            {
                client.Dispose();
            }

            return false;
        }
    }

    public enum UpdateCheckResult {
        NewVersionAvailable = 0,
        NewVersionNotAvailable = 1,
        HasError = 3
    }
}
