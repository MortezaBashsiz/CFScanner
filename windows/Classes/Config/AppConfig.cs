using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace WinCFScan.Classes.Config
{
    internal class AppConfig : ConfigInterface
    {
        protected string appConfigFileName = "app-config.json";
        protected AppConfig? loadedInstance;

        public string frontDomain { get; set; }
        public string scanDomain { get; set; }
        public string configRealUrl { get; set; }

        // load app config
        public bool load()
        {
            try
            {        
                if (!File.Exists(appConfigFileName))
                {
                    return false;
                }

                string jsonString = File.ReadAllText(appConfigFileName);
                loadedInstance = JsonSerializer.Deserialize<AppConfig>(jsonString)!;

            }
            catch (Exception ex)
            {
                Tools.logStep($"AppConfig.load() had exception: {ex.Message}");
                return false;
            }

            return isConfigValid();
        }

        public bool isConfigValid()
        {
            return frontDomain != null && scanDomain != null && configRealUrl != null;
        }

        public AppConfig? getLoadedInstance()
        {
            return loadedInstance;
        }

    }
}
