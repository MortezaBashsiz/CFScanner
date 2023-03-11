using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace WinCFScan.Classes.Config
{

    internal class ClientConfig : ConfigInterface
    {
        public string id { get; set; }
        public string host { get; set; }
        public string port { get; set; }
        public string path { get; set; }
        public string serverName { get; set; }
        public string subnetsList { get; set; }

        protected string v2rayClientConfigName = "v2ray-config/ClientConfig.json";
        protected string v2rayGeneratedConfigDir = "v2ray-config/generated";
        protected string cfIPlistFileName = "cf.local.iplist";
        protected AppConfig appConfig;
        protected ClientConfig loadedInstance;

        public ClientConfig(AppConfig appConfig)
        {
            this.appConfig = appConfig;
            this.load();
            if (isClientConfigOld())
            {
                deleteOldGeneratedConfigs();
            }
        }

        public ClientConfig() { }

        public bool load()
        {
            try
            {
                if (!File.Exists(v2rayClientConfigName))
                {
                    // if config is not found on the disk then download it from remote server
                    if (!remoteUpdateClientConfig())
                        return false;
                }

                string jsonString = File.ReadAllText(v2rayClientConfigName);
                loadedInstance = JsonSerializer.Deserialize<ClientConfig>(jsonString)!;
                loadedInstance.appConfig = appConfig;

                if (!File.Exists(cfIPlistFileName))
                {
                    // if cf subnet file is not found on the disk then download it from remote server
                    loadedInstance.remoteUpdateCFIPList();
                }

            }
            catch (Exception ex)
            {
                Tools.logStep($"ClientConfig.load() had exception: {ex.Message}");
                return false;
            }

            return true;
        }

        public ClientConfig? getLoadedInstance()
        {
            return loadedInstance;
        }


        public bool isConfigValid()
        {
            return this.id!= null && this.host != null && this.port != null && this.path != null && this.serverName != null && 
                    File.Exists(cfIPlistFileName);
        }

        private bool remoteUpdateUrl(string url, string outFile)
        {
            // download 
            var client = new HttpClient();
            try
            {
                client.Timeout = TimeSpan.FromSeconds(10);
                var html = client.GetStringAsync(url).Result;
                File.WriteAllText(outFile, html);

                return true;

            }
            catch (Exception ex)
            {
                Tools.logStep($"remoteUpdateUrl() had exception: {ex.Message}, url: {url}");
                return false;
            }
            finally
            {
                client.Dispose();
            }
        }
        
        public bool remoteUpdateClientConfig()
        {
            // download fresh conf
            bool configUpdated = remoteUpdateUrl(appConfig.clientConfigUrl, v2rayClientConfigName);  
            if (configUpdated)
            {
                // reload
                this.load();
                    
                // also update Cloudflare subnet list
                remoteUpdateCFIPList();
                return true;
            }

            return configUpdated;
        }     
        
        public bool remoteUpdateCFIPList()
        {
            return remoteUpdateUrl(this.subnetsList, cfIPlistFileName);  
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

        public bool isClientConfigOld()
        {
            return !File.Exists(v2rayClientConfigName) || isFileOld(v2rayClientConfigName);
        }

    }
}
