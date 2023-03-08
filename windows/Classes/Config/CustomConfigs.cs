using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace WinCFScan.Classes.Config
{
    internal class CustomConfigs
    {
        public List<CustomConfigInfo> customConfigInfos = new();
        private static string customConfigsDirectory = "v2ray-config/custom-configs";
        private static string customConfigHelpUrl = "...";

        public CustomConfigs() {
            loadCustomConfigs();
        }

        public void loadCustomConfigs()
        {
            customConfigInfos.Clear();
            var customConfigAll = Directory.GetFiles(customConfigsDirectory, "*.json");
            
            foreach (var customConfig in customConfigAll)
            {
                var customConfigObj = new CustomConfigInfo(customConfig, File.ReadAllText(customConfig));
                
                // validate
                if (customConfigObj.isValid()) {
                    customConfigInfos.Add(customConfigObj);
                }
                
            }
        }

        public static bool addNewConfigFile(string fileName, out string errorMessage)
        {
            errorMessage = "";
            var validator = new CustomConfigInfo(fileName, File.ReadAllText(fileName));
            if (validator.isValid())
            {
                try
                {
                    File.Copy(fileName, customConfigsDirectory + "/" + Path.GetFileName(fileName), true);
                    return true;
                }
                catch (Exception ex)
                {
                    errorMessage = ex.Message;
                    return false;
                }
            }
            else
            {
                errorMessage = "Provided config file is not valid v2ray config. See here for more information: " + customConfigHelpUrl;
                return false;
            }
        }
    }

    public class CustomConfigInfo
    {
        public string fileName;
        public string content;

        public CustomConfigInfo(string fileName, string content)
        {
            this.fileName = fileName;
            this.content = content;
        }

        public bool isValid()
        {
            try
            {
                var tt = JsonSerializer.Deserialize<JsonDocument>(content)!;
            }
            catch (Exception)
            {
                // it is invalid json
                return false;
            }

            if (!content.Contains("\"port\": \"PORTPORT\"")) 
                return false;

            if (!content.Contains("\"address\": \"IP.IP.IP.IP\"")) 
                return false;

            return true;
        }

        public override string ToString()
        {
            return Path.GetFileName(fileName);
        }

        // is default config?
        public bool isDefaultConfig()
        {
            return fileName == content && content == "Default";
        }


    }


}
