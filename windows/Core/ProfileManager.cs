using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace WinCFScan.Core
{
    internal class ProfileManager
    {

        private readonly string profileDir;

        public ProfileManager()
        {
            profileDir =
                Path.Combine(
                    AppDomain.CurrentDomain.BaseDirectory,
                    "profiles"
                );

            Directory.CreateDirectory(profileDir);
        }

        public void Save(ProfileModel profile)
        {
            string file =
                Path.Combine(
                    profileDir,
                    profile.ProfileName + ".json"
                );

            string json =
                JsonSerializer.Serialize(
                    profile,
                    new JsonSerializerOptions
                    {
                        WriteIndented = true
                    });

            File.WriteAllText(
                file,
                json
            );
        }

        public ProfileModel? Load(
            string profileName)
        {
            string file =
                Path.Combine(
                    profileDir,
                    profileName + ".json"
                );

            if (!File.Exists(file))
                return null;

            string json =
                File.ReadAllText(file);

            return JsonSerializer.Deserialize<ProfileModel>(json);
        }

        public List<string> GetProfiles()
        {
            List<string> result =
                new List<string>();

            foreach (string file in
                Directory.GetFiles(
                    profileDir,
                    "*.json"))
            {
                result.Add(
                    Path.GetFileNameWithoutExtension(
                        file));
            }

            return result;
        }

    }
}
