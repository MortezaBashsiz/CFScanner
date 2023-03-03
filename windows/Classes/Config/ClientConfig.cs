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
        protected AppConfig appConfig;
        protected ClientConfig loadedInstance;

        public ClientConfig(AppConfig appConfig)
        {
            this.appConfig = appConfig;
            this.load();
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
            return this.id!= null && this.host != null && this.port != null && this.path != null && this.serverName != null;
        }

        // if 'client config' file is not exists or is too old then download it from remote
        public bool remoteUpdateClientConfig()
        {
            if (isClientConfigOld())
            {
                deleteOldGeneratedConfigs();

                // download fresh conf
                var client = new HttpClient();
                try
                {
                    client.Timeout = TimeSpan.FromSeconds(10);
                    var html = client.GetStringAsync(this.appConfig.clientConfigUrl).Result;
                    File.WriteAllText(v2rayClientConfigName, html);
                    
                    // reload
                    this.load();
                    return true;

                }
                catch (Exception ex)
                {
                    Tools.logStep($"remoteUpdateClientConfig() had exception: {ex.Message}");
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

        public bool isClientConfigOld()
        {
            return !File.Exists(v2rayClientConfigName) || isFileOld(v2rayClientConfigName);
        }

    }
}
