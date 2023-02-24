using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace WinCFScan.Classes.Config
{

    internal class RealConfig : ConfigInterface
    {
        public string id { get; private set; }
        public string host { get; private set; }
        public string port { get; private set; }
        public string path { get; private set; }
        public string serverName { get; private set; }

        protected string v2rayConfigRealFileName = "v2ray-config/config.real";
        protected string v2rayGeneratedConfigDir = "v2ray-config/generated";
        protected AppConfig appConfig;

        public RealConfig(AppConfig appConfig)
        {
            this.appConfig = appConfig;
            this.load();
        }

        // load real config
        public bool load()
        {          
            if (!File.Exists(v2rayConfigRealFileName))
            {
                return false;
            }

            try
            {
                string[] lines = File.ReadAllLines(v2rayConfigRealFileName);
                foreach (string line in lines)
                {
                    string[] parts = line.Split(": ");
                    switch (parts[0].ToLower())
                    {
                        case "id":
                            this.id = parts[1];
                            break;
                        case "host":
                            this.host = parts[1];
                            break;
                        case "port":
                            this.port = parts[1];
                            break;
                        case "path":
                            this.path = parts[1];
                            break;
                        case "servername":
                            this.serverName = parts[1];
                            break;
                    }
                }
            }
            catch (Exception ex) {
                return false;
            }

            return isConfigValid();
        }

        public bool isConfigValid()
        {
            return this.id!= null && this.host != null && this.port != null && this.path != null && this.serverName != null;
        }

        // if 'config real' file is not exists or is too old then download it from remote
        public bool remoteUpdateConfigReal()
        {
            if (isConfigRealOld())
            {
                deleteOldGeneratedConfigs();

                // download fresh conf
                var client = new HttpClient();
                try
                {
                    client.Timeout = TimeSpan.FromSeconds(10);
                    var html = client.GetStringAsync(this.appConfig.configRealUrl).Result;
                    File.WriteAllText(v2rayConfigRealFileName, html);
                    
                    // reload
                    this.load();
                    return true;

                }
                catch (Exception ex)
                {
                    return false;
                }
                finally
                {
                    client.Dispose();
                }
            }

            return true;
        }

        private void deleteOldGeneratedConfigs()
        {
            try
            {                
                var resultFiles = Directory.GetFiles(v2rayGeneratedConfigDir, "*.json");
                foreach (var resultFile in resultFiles)
                {
                    if (isFileOld(resultFile))
                    {
                        File.Delete(resultFile);
                    }
                }
            }
            catch (Exception)
            {

            }
        }

        private bool isFileOld(string file)
        {
            return (DateTime.Now - File.GetLastWriteTime(file)).TotalDays > 1;
        }

        public bool isConfigRealOld()
        {
            return !File.Exists(v2rayConfigRealFileName) || isFileOld(v2rayConfigRealFileName);
        }

    }
}
