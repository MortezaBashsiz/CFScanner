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
        private static string customConfigHelpUrl = "https://github.com/MortezaBashsiz/CFScanner/discussions/210";

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
                
                string msg;
                // validate
                if (customConfigObj.isValid(out msg)) {
                    customConfigInfos.Add(customConfigObj);
                }
                else
                {
                    Tools.logStep($"Invalid custom config: {msg}, File: {customConfig}");
                }
                
            }
        }

        public static bool addNewConfigFile(string fileName, out string errorMessage)
        {
            errorMessage = "";
            var validator = new CustomConfigInfo(fileName, File.ReadAllText(fileName));
            if (validator.isValid(out errorMessage))
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
                errorMessage = $"Provided config file is not valid v2ray config: {errorMessage + Environment.NewLine}See here for more information: " + customConfigHelpUrl;
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

        public bool isValid(out string message)
        {
            message = "";
            try
            {
                var tt = JsonSerializer.Deserialize<JsonDocument>(content)!;
            }
            catch (Exception)
            {
                // it is invalid json
                message = "File is not in valid json format.";
                return false;
            }

            if (!content.Contains("\"port\": \"PORTPORT\""))
            {
                message = "PORTPORT parameter is not found in the file.";
                return false;
            }

            if (!content.Contains("\"address\": \"IP.IP.IP.IP\""))
            {
                message = "IP.IP.IP.IP parameter is not found in the file.";
                return false;
            }

            
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
