using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using WinCFScan.Classes.HTTPRequest;

namespace WinCFScan.Classes.Config
{
    internal class ConfigManager
    {
        protected string v2rayTemplateConfigFileName = "v2ray-config/config.json.template";
        protected string enableDebugFileName = "enable-debug"; // debug enabler file
        public string? v2rayConfigTemplate { get; private set; }
        protected string[] mandatoryDirectories = { "v2ray-config", "v2ray-config/generated", "results" }; //this dirs must be existed

        protected AppConfig? appConfig;
        protected RealConfig? realConfig;

        public string errorMessage { get; set; } = "";
        protected bool loadedOK = true;

        public static ConfigManager? Instance { get; private set; }
        public bool enableDebug = false;

        public ConfigManager()
        {
            if (this.load() && appConfig != null)
            {
                realConfig = new RealConfig(appConfig);
            }

            // set static instance for later access of this instance
            Instance = this;

            checkDebugEnable();
        }

        protected bool load()
        {
            try
            {
                // create mandatory directories
                foreach (var dir in mandatoryDirectories)
                {
                    if (!Directory.Exists(dir));
                    {
                        Directory.CreateDirectory(dir);
                    }
                }

                // app config
                appConfig = new AppConfig();
                appConfig.load(); // this must be called
                appConfig = appConfig.getLoadedInstance();

                // load v2ray config template
                if (!File.Exists(v2rayTemplateConfigFileName))
                {
                    errorMessage = "v2ray template config file is not exists at 'v2ray-config/config.json.template'";
                    loadedOK = false;
                    return false;
                }

                v2rayConfigTemplate = File.ReadAllText(v2rayTemplateConfigFileName);

                // check existance of v2ray.exe
                if (!File.Exists("v2ray.exe"))
                {
                    errorMessage = "v2ray.exe in not exists in app directory.";
                    loadedOK = false;
                    return false;
                }

            }
            catch(Exception ex)
            {
                Tools.logStep($"ConfigManager.load() had exception: {ex.Message}");
                errorMessage = ex.Message;
                return false;
            }

            return isConfigValid();
        }

        public bool isConfigValid()
        {
            if(appConfig != null && appConfig.isConfigValid() && v2rayConfigTemplate != null && loadedOK)
            {
                return true;
            }

            return false;
        }

        // debugging enabled from outside?
        private void checkDebugEnable()
        {
            enableDebug = File.Exists(enableDebugFileName) || File.Exists(enableDebugFileName + ".txt");
        }


        public AppConfig? getAppConfig()
        {
            return this.appConfig;
        }

        public RealConfig? getRealConfig()
        {
            return this.realConfig;
        }
    }
}
