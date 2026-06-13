using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Diagnostics;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using WinCFScan.Core;
using WinCFScan.Models;
using System.Threading;
using System.Net.NetworkInformation;

namespace WinCFScan.Forms
{
    public partial class ConfigBuilderForm : Form
    {
        private readonly ConfigParser parser =new ConfigParser();
        private readonly JsonBuilder jsonBuilder =new JsonBuilder();
        private readonly TlsTester tlsTester =new TlsTester();
        private readonly IpTester ipTester = new IpTester();
        private readonly PingTester pingTester =new PingTester();
        private readonly TcpTester tcpTester = new TcpTester();
        private readonly ProfileManager profileManager =new ProfileManager();
       
        public ConfigBuilderForm()
        {
            InitializeComponent();
            txtConfigLink.ContextMenuStrip = contextMenuConfig;
        }

        private void ConfigBuilderForm_Load(object sender, EventArgs e)
        {
            LoadProfiles();
        }

        private void btnParse_Click(object sender, EventArgs e)
        {
            rtbLog.Text = "";
            try
            {
                var info =
                     parser.Parse(
                          txtConfigLink.Text.Trim()
                                                   );

                txtHost.Text =
                    info.Host;

                txtUUID.Text =
                    info.UUID;

                txtPort.Text =
                    info.Port.ToString();

                txtPath.Text =
                    info.Path;

                txtSNI.Text =
                    info.SNI;

                txtTlsDomain.Text =
                    info.SNI;

                lblStatus.Text =
                    "Status : Config Parsed";

                addTextLog("Config parsed successfully");

                lblProtocol.Text =
                    info.Protocol.ToUpper();

                lblNetwork.Text =
                    info.Network.ToUpper();

                lblSecurity.Text =
                    info.Security.ToUpper();

                lblTemplate.Text =
                    jsonBuilder.GetTemplateName(
                        info
                    );
            }
            catch (Exception ex)
            {
                MessageBox.Show(
                    ex.Message,
                    "Error"
                );
            }


        }

        private void btnGenerateJson_Click(object sender, EventArgs e)
        {
            rtbLog.Text = "";
            try
            {
                ConfigInfo info =
                    new ConfigInfo
                    {
                        UUID = txtUUID.Text,
                        Host = txtHost.Text,
                        Port = int.Parse(txtPort.Text),
                        Path = txtPath.Text,
                        SNI = txtSNI.Text
                    };

                jsonBuilder.Generate(info);

               addTextLog(
                    "generated.json created"
                    + Environment.NewLine
                );

                addTextLog(
                    "ClientConfig.json created"
                    + Environment.NewLine
                );

                MessageBox.Show(
                    "JSON files generated successfully",
                    "Success",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Information
                );
            }
            catch (Exception ex)
            {
                MessageBox.Show(
                    ex.Message,
                    "Error"
                );
            }


        }

        private async void btnTlsTest_Click(object sender, EventArgs e)
        {
            try
            {
                string domain =
                    txtTlsDomain.Text.Trim();

                if (string.IsNullOrWhiteSpace(domain))
                {
                    MessageBox.Show(
                        "Enter domain first."
                    );

                    return;
                }

                btnTlsTest.Enabled = false;

                lblStatus.Text =
                    "Status : Testing...";

                lblStatus.ForeColor =
                    System.Drawing.Color.Orange;

                rtbLog.Clear();

                string result =
                    await Task.Run(() =>
                    {
                        return tlsTester.Run(domain);
                    });

                rtbLog.Text = result;

                if (result.Contains(
                        "handshake succeeded",
                        StringComparison.OrdinalIgnoreCase)
                    ||
                    result.Contains(
                        "tls handshake succeeded",
                        StringComparison.OrdinalIgnoreCase))
                {
                    lblStatus.Text =
                        "Status : Connected";

                    lblStatus.ForeColor =
                        System.Drawing.Color.Green;
                }
                else if (result.Contains(
                             "timeout",
                             StringComparison.OrdinalIgnoreCase))
                {
                    lblStatus.Text =
                        "Status : Timeout";

                    lblStatus.ForeColor =
                        System.Drawing.Color.DarkOrange;
                }
                else
                {
                    lblStatus.Text =
                        "Status : Failed";

                    lblStatus.ForeColor =
                        System.Drawing.Color.Red;
                }
            }
            catch (Exception ex)
            {
                lblStatus.Text =
                    "Status : Error";

                lblStatus.ForeColor =
                    System.Drawing.Color.Red;

                rtbLog.Text =
                    ex.ToString();

                MessageBox.Show(
                    ex.Message,
                    "TLS Test Error"
                );
            }
            finally
            {
                btnTlsTest.Enabled = true;
            }

        }

        private void btnOpenGenerated_Click(object sender, EventArgs e)
        {

            string path =
        Path.Combine(
            AppDomain.CurrentDomain.BaseDirectory,
            "generated"
        );

            if (Directory.Exists(path))
            {
                Process.Start(
                    "explorer.exe",
                    path
                );
            }

        }



        private void btnCopyJson_Click(object sender, EventArgs e)
        {
            string file =
            Path.Combine(
            AppDomain.CurrentDomain.BaseDirectory,
            "generated",
            "generated.json"
        );

            if (!File.Exists(file))
                return;

            Clipboard.SetText(
                File.ReadAllText(file)
            );

            MessageBox.Show(
                "generated.json copied."
            );
        }



        private void txtConfigLink_DoubleClick(object sender, EventArgs e)
        {
            txtConfigLink.Text =
            Clipboard.GetText();
            btnParse.PerformClick();
        }

        private void pastConfigFromClipBoardToolStripMenuItem_Click(object sender, EventArgs e)
        {
            txtConfigLink.Text =
            Clipboard.GetText();
            btnParse.PerformClick();
        }

        private void copyJSONToolStripMenuItem_Click(object sender, EventArgs e)
        {
            string file =
            Path.Combine(
            AppDomain.CurrentDomain.BaseDirectory,
            "generated",
            "generated.json"
        );

            if (!File.Exists(file))
                return;

            Clipboard.SetText(
                File.ReadAllText(file)
            );

            MessageBox.Show(
                "generated.json copied."
            );
        }



        private void clearToolStripMenuItem_Click(object sender, EventArgs e)
        {
            txtConfigLink.Text = "";
        }

        private async void btnPing_Click(object sender, EventArgs e)
        {

            string host =
       txtTlsDomain.Text.Trim();

            if (string.IsNullOrWhiteSpace(host))
                return;

            rtbLog.Clear();

            string result =
                await Task.Run(() =>
                    pingTester.Run(host));

            addTextLog(result);

        }

        private async void btnTcpTest_Click(object sender, EventArgs e)
        {
            string host =
       txtTlsDomain.Text.Trim();

            if (string.IsNullOrWhiteSpace(host))
                return;

            rtbLog.Clear();

            string result =
                await Task.Run(() =>
                    tcpTester.Run(
                        host,
                        443));

            addTextLog(result);
        }



        //-------------------------------------void-------------------------------------
        private void LoadProfiles()
        {
            cmbProfiles.Items.Clear();

            foreach (var profile in
                     profileManager.GetProfiles())
            {
                cmbProfiles.Items.Add(profile);
            }
        }

        private void btnSaveProfile_Click(object sender, EventArgs e)
        {
            if (string.IsNullOrWhiteSpace(txtUUID.Text) || string.IsNullOrWhiteSpace(txtHost.Text))
            {
                MessageBox.Show(
                    "Please import and parse a config first."
                );

                return;
            }

            string name =
        Microsoft.VisualBasic.Interaction.InputBox("Profile Name","Save Profile");

            if (string.IsNullOrWhiteSpace(name))
            {
                MessageBox.Show(
                    "Profile name is required."
                );

                return;
            }

            if (string.IsNullOrWhiteSpace(txtUUID.Text))
            {
                MessageBox.Show(
                    "UUID is empty."
                );

                txtUUID.Focus();

                return;
            }

            if (string.IsNullOrWhiteSpace(txtHost.Text))
            {
                MessageBox.Show(
                    "Host is empty."
                );

                txtHost.Focus();

                return;
            }

            if (string.IsNullOrWhiteSpace(txtSNI.Text))
            {
                MessageBox.Show(
                    "SNI is empty."
                );

                txtSNI.Focus();

                return;
            }

            if (string.IsNullOrWhiteSpace(txtPath.Text))
            {
                MessageBox.Show(
                    "Path is empty."
                );

                txtPath.Focus();

                return;
            }

            if (!int.TryParse(txtPort.Text, out int port))
            {
                MessageBox.Show(
                    "Port is invalid."
                );

                txtPort.Focus();

                return;
            }

            ProfileModel profile =
                new ProfileModel
                {
                    ProfileName = name,
                    UUID = txtUUID.Text.Trim(),
                    Host = txtHost.Text.Trim(),
                    SNI = txtSNI.Text.Trim(),
                    Path = txtPath.Text.Trim(),
                    Port = port
                };

            profileManager.Save(profile);

            LoadProfiles();

            MessageBox.Show(
                "Profile saved successfully."
            );
        }



        private void btnClearLog_Click(object sender, EventArgs e)
        {
            rtbLog.Text = "";
        }

        private void cmbProfiles_SelectedIndexChanged(object sender, EventArgs e)
        {
            if (cmbProfiles.SelectedItem == null)
                return;

            ProfileModel? profile =
                profileManager.Load(cmbProfiles.SelectedItem.ToString());

            if (profile == null)
                return;

            txtUUID.Text =
                profile.UUID;

            txtHost.Text =
                profile.Host;

            txtSNI.Text =
                profile.SNI;

            txtPath.Text =
                profile.Path;

            txtPort.Text =
                profile.Port
                    .ToString();
            txtTlsDomain.Text =
                profile.SNI;
            MessageBox.Show(
                "Profile loaded."
            );

        }
        private void addTextLog(string text)
        {
            rtbLog.AppendText($"{DateTime.Now:HH:mm:ss} Log is :{Environment.NewLine} {text}{Environment.NewLine}");
        }
        private void btnCreateTemplate_Click(object sender, EventArgs e)
        {
            rtbLog.Text = "";
           
            try
            {
                string basePath = AppDomain.CurrentDomain.BaseDirectory;
                string folder = Path.Combine(basePath, "Templates");
               
                if (Directory.Exists(folder))
                {

                    if(!File.Exists("trojan_ws_tls.json"))
                    {
                        TemplateFiles.WriteTemplate("trojan_ws_tls.json", TemplateFiles.trojan_ws_tls, addTextLog);
                    }
                    if (!File.Exists("vless_grpc_tls.json"))
                    {
                        TemplateFiles.WriteTemplate("vless_grpc_tls.json", TemplateFiles.vless_grpc_tls, addTextLog);
                    }
                    if (!File.Exists("vless_ws_tls.json"))
                    {
                        TemplateFiles.WriteTemplate("vless_ws_tls.json", TemplateFiles.vless_ws_tls, addTextLog);
                    }
                    if (!File.Exists("vmess_ws_tls.json"))
                    {
                        TemplateFiles.WriteTemplate("vmess_ws_tls.json", TemplateFiles.vmess_ws_tls, addTextLog);
                    }

                }

                              
               
            }
            catch (Exception ex)
            {

                addTextLog("Create Templates Faild!" + ex);
            }
            

           
        }

        //----------------------End Void---------------------------------------------------
    }
}
