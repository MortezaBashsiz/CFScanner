namespace WinCFScan.Forms
{
    partial class ConfigBuilderForm
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            components = new System.ComponentModel.Container();
            txtConfigLink = new TextBox();
            btnParse = new Button();
            btnGenerateJson = new Button();
            txtHost = new TextBox();
            txtUUID = new TextBox();
            txtTlsDomain = new TextBox();
            txtPath = new TextBox();
            txtSNI = new TextBox();
            txtPort = new TextBox();
            label2 = new Label();
            label3 = new Label();
            label4 = new Label();
            label5 = new Label();
            label6 = new Label();
            btnTlsTest = new Button();
            lblStatus = new Label();
            rtbLog = new RichTextBox();
            groupBox1 = new GroupBox();
            groupBox5 = new GroupBox();
            btnTcpTest = new Button();
            btnPing = new Button();
            label1 = new Label();
            groupBox4 = new GroupBox();
            btnOpenGenerated = new Button();
            groupBox2 = new GroupBox();
            label10 = new Label();
            lblTemplate = new Label();
            label9 = new Label();
            lblSecurity = new Label();
            label8 = new Label();
            lblNetwork = new Label();
            label7 = new Label();
            lblProtocol = new Label();
            groupBox3 = new GroupBox();
            btnClearLog = new Button();
            contextMenuConfig = new ContextMenuStrip(components);
            pastConfigFromClipBoardToolStripMenuItem = new ToolStripMenuItem();
            toolStripMenuItem2 = new ToolStripSeparator();
            copyJSONToolStripMenuItem = new ToolStripMenuItem();
            toolStripMenuItem1 = new ToolStripSeparator();
            clearToolStripMenuItem = new ToolStripMenuItem();
            groupBox6 = new GroupBox();
            label11 = new Label();
            btnSaveProfile = new Button();
            cmbProfiles = new ComboBox();
            btnCreateTemplate = new Button();
            groupBox1.SuspendLayout();
            groupBox5.SuspendLayout();
            groupBox4.SuspendLayout();
            groupBox2.SuspendLayout();
            groupBox3.SuspendLayout();
            contextMenuConfig.SuspendLayout();
            groupBox6.SuspendLayout();
            SuspendLayout();
            // 
            // txtConfigLink
            // 
            txtConfigLink.Location = new Point(11, 16);
            txtConfigLink.Multiline = true;
            txtConfigLink.Name = "txtConfigLink";
            txtConfigLink.Size = new Size(412, 124);
            txtConfigLink.TabIndex = 0;
            txtConfigLink.DoubleClick += txtConfigLink_DoubleClick;
            // 
            // btnParse
            // 
            btnParse.Location = new Point(429, 16);
            btnParse.Name = "btnParse";
            btnParse.Size = new Size(114, 31);
            btnParse.TabIndex = 2;
            btnParse.Text = "Parse Config";
            btnParse.UseVisualStyleBackColor = true;
            btnParse.Click += btnParse_Click;
            // 
            // btnGenerateJson
            // 
            btnGenerateJson.Location = new Point(429, 48);
            btnGenerateJson.Name = "btnGenerateJson";
            btnGenerateJson.Size = new Size(114, 31);
            btnGenerateJson.TabIndex = 3;
            btnGenerateJson.Text = "Generate JSON";
            btnGenerateJson.UseVisualStyleBackColor = true;
            btnGenerateJson.Click += btnGenerateJson_Click;
            // 
            // txtHost
            // 
            txtHost.Location = new Point(41, 22);
            txtHost.Name = "txtHost";
            txtHost.ReadOnly = true;
            txtHost.Size = new Size(496, 23);
            txtHost.TabIndex = 4;
            // 
            // txtUUID
            // 
            txtUUID.Location = new Point(41, 51);
            txtUUID.Name = "txtUUID";
            txtUUID.ReadOnly = true;
            txtUUID.Size = new Size(496, 23);
            txtUUID.TabIndex = 5;
            // 
            // txtTlsDomain
            // 
            txtTlsDomain.Location = new Point(51, 22);
            txtTlsDomain.Name = "txtTlsDomain";
            txtTlsDomain.Size = new Size(486, 23);
            txtTlsDomain.TabIndex = 6;
            // 
            // txtPath
            // 
            txtPath.Location = new Point(41, 80);
            txtPath.Name = "txtPath";
            txtPath.ReadOnly = true;
            txtPath.Size = new Size(496, 23);
            txtPath.TabIndex = 7;
            // 
            // txtSNI
            // 
            txtSNI.Location = new Point(41, 109);
            txtSNI.Name = "txtSNI";
            txtSNI.ReadOnly = true;
            txtSNI.Size = new Size(496, 23);
            txtSNI.TabIndex = 8;
            // 
            // txtPort
            // 
            txtPort.Location = new Point(41, 138);
            txtPort.Name = "txtPort";
            txtPort.ReadOnly = true;
            txtPort.Size = new Size(100, 23);
            txtPort.TabIndex = 9;
            // 
            // label2
            // 
            label2.AutoSize = true;
            label2.Location = new Point(6, 25);
            label2.Name = "label2";
            label2.Size = new Size(32, 15);
            label2.TabIndex = 10;
            label2.Text = "Host";
            // 
            // label3
            // 
            label3.AutoSize = true;
            label3.Location = new Point(6, 59);
            label3.Name = "label3";
            label3.Size = new Size(34, 15);
            label3.TabIndex = 10;
            label3.Text = "UUID";
            // 
            // label4
            // 
            label4.AutoSize = true;
            label4.Location = new Point(6, 141);
            label4.Name = "label4";
            label4.Size = new Size(29, 15);
            label4.TabIndex = 10;
            label4.Text = "Port";
            // 
            // label5
            // 
            label5.AutoSize = true;
            label5.Location = new Point(6, 88);
            label5.Name = "label5";
            label5.Size = new Size(31, 15);
            label5.TabIndex = 10;
            label5.Text = "Path";
            // 
            // label6
            // 
            label6.AutoSize = true;
            label6.Location = new Point(6, 112);
            label6.Name = "label6";
            label6.Size = new Size(23, 15);
            label6.TabIndex = 10;
            label6.Text = "Sni";
            // 
            // btnTlsTest
            // 
            btnTlsTest.Location = new Point(79, 57);
            btnTlsTest.Name = "btnTlsTest";
            btnTlsTest.Size = new Size(133, 51);
            btnTlsTest.TabIndex = 2;
            btnTlsTest.Text = " Run TLS Test";
            btnTlsTest.UseVisualStyleBackColor = true;
            btnTlsTest.Click += btnTlsTest_Click;
            // 
            // lblStatus
            // 
            lblStatus.AutoSize = true;
            lblStatus.Location = new Point(6, 25);
            lblStatus.Name = "lblStatus";
            lblStatus.Size = new Size(80, 15);
            lblStatus.TabIndex = 10;
            lblStatus.Text = "Status : Ready";
            // 
            // rtbLog
            // 
            rtbLog.Location = new Point(6, 48);
            rtbLog.Name = "rtbLog";
            rtbLog.ReadOnly = true;
            rtbLog.Size = new Size(455, 449);
            rtbLog.TabIndex = 11;
            rtbLog.Text = "";
            // 
            // groupBox1
            // 
            groupBox1.Controls.Add(groupBox5);
            groupBox1.Controls.Add(groupBox4);
            groupBox1.Controls.Add(groupBox2);
            groupBox1.Location = new Point(15, 96);
            groupBox1.Name = "groupBox1";
            groupBox1.Size = new Size(561, 557);
            groupBox1.TabIndex = 12;
            groupBox1.TabStop = false;
            groupBox1.Text = "Config";
            // 
            // groupBox5
            // 
            groupBox5.Controls.Add(btnTcpTest);
            groupBox5.Controls.Add(btnPing);
            groupBox5.Controls.Add(txtTlsDomain);
            groupBox5.Controls.Add(label1);
            groupBox5.Controls.Add(btnTlsTest);
            groupBox5.Location = new Point(6, 437);
            groupBox5.Name = "groupBox5";
            groupBox5.Size = new Size(543, 114);
            groupBox5.TabIndex = 11;
            groupBox5.TabStop = false;
            groupBox5.Text = "TLS Check";
            // 
            // btnTcpTest
            // 
            btnTcpTest.Location = new Point(362, 57);
            btnTcpTest.Name = "btnTcpTest";
            btnTcpTest.Size = new Size(130, 51);
            btnTcpTest.TabIndex = 12;
            btnTcpTest.Text = "Tcp";
            btnTcpTest.UseVisualStyleBackColor = true;
            btnTcpTest.Click += btnTcpTest_Click;
            // 
            // btnPing
            // 
            btnPing.Location = new Point(218, 57);
            btnPing.Name = "btnPing";
            btnPing.Size = new Size(138, 51);
            btnPing.TabIndex = 11;
            btnPing.Text = "Ping";
            btnPing.UseVisualStyleBackColor = true;
            btnPing.Click += btnPing_Click;
            // 
            // label1
            // 
            label1.AutoSize = true;
            label1.Location = new Point(3, 26);
            label1.Name = "label1";
            label1.Size = new Size(52, 15);
            label1.TabIndex = 10;
            label1.Text = "Domain:";
            // 
            // groupBox4
            // 
            groupBox4.Controls.Add(btnCreateTemplate);
            groupBox4.Controls.Add(txtConfigLink);
            groupBox4.Controls.Add(btnParse);
            groupBox4.Controls.Add(btnGenerateJson);
            groupBox4.Controls.Add(btnOpenGenerated);
            groupBox4.Location = new Point(6, 22);
            groupBox4.Name = "groupBox4";
            groupBox4.Size = new Size(549, 151);
            groupBox4.TabIndex = 8;
            groupBox4.TabStop = false;
            // 
            // btnOpenGenerated
            // 
            btnOpenGenerated.Location = new Point(429, 79);
            btnOpenGenerated.Name = "btnOpenGenerated";
            btnOpenGenerated.Size = new Size(114, 31);
            btnOpenGenerated.TabIndex = 3;
            btnOpenGenerated.Text = "Open Generated Folder";
            btnOpenGenerated.UseVisualStyleBackColor = true;
            btnOpenGenerated.Click += btnOpenGenerated_Click;
            // 
            // groupBox2
            // 
            groupBox2.Controls.Add(txtHost);
            groupBox2.Controls.Add(txtUUID);
            groupBox2.Controls.Add(label10);
            groupBox2.Controls.Add(lblTemplate);
            groupBox2.Controls.Add(label6);
            groupBox2.Controls.Add(label9);
            groupBox2.Controls.Add(lblSecurity);
            groupBox2.Controls.Add(txtPath);
            groupBox2.Controls.Add(label8);
            groupBox2.Controls.Add(lblNetwork);
            groupBox2.Controls.Add(label7);
            groupBox2.Controls.Add(lblProtocol);
            groupBox2.Controls.Add(label5);
            groupBox2.Controls.Add(txtSNI);
            groupBox2.Controls.Add(txtPort);
            groupBox2.Controls.Add(label4);
            groupBox2.Controls.Add(label2);
            groupBox2.Controls.Add(label3);
            groupBox2.Location = new Point(6, 179);
            groupBox2.Name = "groupBox2";
            groupBox2.Size = new Size(543, 250);
            groupBox2.TabIndex = 4;
            groupBox2.TabStop = false;
            groupBox2.Text = "Config Information";
            // 
            // label10
            // 
            label10.AutoSize = true;
            label10.Location = new Point(11, 224);
            label10.Name = "label10";
            label10.Size = new Size(58, 15);
            label10.TabIndex = 5;
            label10.Text = "Template:";
            // 
            // lblTemplate
            // 
            lblTemplate.AutoSize = true;
            lblTemplate.Location = new Point(76, 224);
            lblTemplate.Name = "lblTemplate";
            lblTemplate.Size = new Size(68, 15);
            lblTemplate.TabIndex = 5;
            lblTemplate.Text = "lblTemplate";
            // 
            // label9
            // 
            label9.AutoSize = true;
            label9.Location = new Point(11, 209);
            label9.Name = "label9";
            label9.Size = new Size(52, 15);
            label9.TabIndex = 5;
            label9.Text = "Security:";
            // 
            // lblSecurity
            // 
            lblSecurity.AutoSize = true;
            lblSecurity.Location = new Point(79, 209);
            lblSecurity.Name = "lblSecurity";
            lblSecurity.Size = new Size(62, 15);
            lblSecurity.TabIndex = 5;
            lblSecurity.Text = "lblSecurity";
            // 
            // label8
            // 
            label8.AutoSize = true;
            label8.Location = new Point(11, 194);
            label8.Name = "label8";
            label8.Size = new Size(55, 15);
            label8.TabIndex = 5;
            label8.Text = "Network:";
            // 
            // lblNetwork
            // 
            lblNetwork.AutoSize = true;
            lblNetwork.Location = new Point(78, 194);
            lblNetwork.Name = "lblNetwork";
            lblNetwork.Size = new Size(65, 15);
            lblNetwork.TabIndex = 5;
            lblNetwork.Text = "lblNetwork";
            // 
            // label7
            // 
            label7.AutoSize = true;
            label7.Location = new Point(11, 179);
            label7.Name = "label7";
            label7.Size = new Size(55, 15);
            label7.TabIndex = 5;
            label7.Text = "Protocol:";
            // 
            // lblProtocol
            // 
            lblProtocol.AutoSize = true;
            lblProtocol.Location = new Point(78, 179);
            lblProtocol.Name = "lblProtocol";
            lblProtocol.Size = new Size(65, 15);
            lblProtocol.TabIndex = 5;
            lblProtocol.Text = "lblProtocol";
            // 
            // groupBox3
            // 
            groupBox3.Controls.Add(btnClearLog);
            groupBox3.Controls.Add(lblStatus);
            groupBox3.Controls.Add(rtbLog);
            groupBox3.Location = new Point(582, 101);
            groupBox3.Name = "groupBox3";
            groupBox3.Size = new Size(467, 546);
            groupBox3.TabIndex = 13;
            groupBox3.TabStop = false;
            groupBox3.Text = "Log";
            // 
            // btnClearLog
            // 
            btnClearLog.Location = new Point(171, 503);
            btnClearLog.Name = "btnClearLog";
            btnClearLog.Size = new Size(75, 37);
            btnClearLog.TabIndex = 12;
            btnClearLog.Text = "Clear Log";
            btnClearLog.UseVisualStyleBackColor = true;
            btnClearLog.Click += btnClearLog_Click;
            // 
            // contextMenuConfig
            // 
            contextMenuConfig.Items.AddRange(new ToolStripItem[] { pastConfigFromClipBoardToolStripMenuItem, toolStripMenuItem2, copyJSONToolStripMenuItem, toolStripMenuItem1, clearToolStripMenuItem });
            contextMenuConfig.Name = "contextMenuConfig";
            contextMenuConfig.Size = new Size(152, 82);
            // 
            // pastConfigFromClipBoardToolStripMenuItem
            // 
            pastConfigFromClipBoardToolStripMenuItem.Name = "pastConfigFromClipBoardToolStripMenuItem";
            pastConfigFromClipBoardToolStripMenuItem.Size = new Size(151, 22);
            pastConfigFromClipBoardToolStripMenuItem.Text = "Past ClipBoard";
            pastConfigFromClipBoardToolStripMenuItem.Click += pastConfigFromClipBoardToolStripMenuItem_Click;
            // 
            // toolStripMenuItem2
            // 
            toolStripMenuItem2.Name = "toolStripMenuItem2";
            toolStripMenuItem2.Size = new Size(148, 6);
            // 
            // copyJSONToolStripMenuItem
            // 
            copyJSONToolStripMenuItem.Name = "copyJSONToolStripMenuItem";
            copyJSONToolStripMenuItem.Size = new Size(151, 22);
            copyJSONToolStripMenuItem.Text = "Copy JSON";
            copyJSONToolStripMenuItem.Click += copyJSONToolStripMenuItem_Click;
            // 
            // toolStripMenuItem1
            // 
            toolStripMenuItem1.Name = "toolStripMenuItem1";
            toolStripMenuItem1.Size = new Size(148, 6);
            // 
            // clearToolStripMenuItem
            // 
            clearToolStripMenuItem.Name = "clearToolStripMenuItem";
            clearToolStripMenuItem.Size = new Size(151, 22);
            clearToolStripMenuItem.Text = "Clear";
            clearToolStripMenuItem.Click += clearToolStripMenuItem_Click;
            // 
            // groupBox6
            // 
            groupBox6.Controls.Add(label11);
            groupBox6.Controls.Add(btnSaveProfile);
            groupBox6.Controls.Add(cmbProfiles);
            groupBox6.Location = new Point(12, 12);
            groupBox6.Name = "groupBox6";
            groupBox6.Size = new Size(1037, 71);
            groupBox6.TabIndex = 14;
            groupBox6.TabStop = false;
            groupBox6.Text = "Profile";
            // 
            // label11
            // 
            label11.AutoSize = true;
            label11.Location = new Point(13, 25);
            label11.Name = "label11";
            label11.Size = new Size(44, 15);
            label11.TabIndex = 2;
            label11.Text = "Profile:";
            // 
            // btnSaveProfile
            // 
            btnSaveProfile.Location = new Point(609, 11);
            btnSaveProfile.Name = "btnSaveProfile";
            btnSaveProfile.Size = new Size(108, 43);
            btnSaveProfile.TabIndex = 1;
            btnSaveProfile.Text = "SaveProfile";
            btnSaveProfile.UseVisualStyleBackColor = true;
            btnSaveProfile.Click += btnSaveProfile_Click;
            // 
            // cmbProfiles
            // 
            cmbProfiles.DropDownStyle = ComboBoxStyle.DropDownList;
            cmbProfiles.FormattingEnabled = true;
            cmbProfiles.Location = new Point(60, 22);
            cmbProfiles.Name = "cmbProfiles";
            cmbProfiles.Size = new Size(543, 23);
            cmbProfiles.TabIndex = 0;
            cmbProfiles.SelectedIndexChanged += cmbProfiles_SelectedIndexChanged;
            // 
            // btnCreateTemplate
            // 
            btnCreateTemplate.Location = new Point(429, 110);
            btnCreateTemplate.Name = "btnCreateTemplate";
            btnCreateTemplate.Size = new Size(114, 33);
            btnCreateTemplate.TabIndex = 4;
            btnCreateTemplate.Text = "Create Template";
            btnCreateTemplate.UseVisualStyleBackColor = true;
            btnCreateTemplate.Click += btnCreateTemplate_Click;
            // 
            // ConfigBuilderForm
            // 
            AutoScaleDimensions = new SizeF(7F, 15F);
            AutoScaleMode = AutoScaleMode.Font;
            ClientSize = new Size(1060, 665);
            Controls.Add(groupBox6);
            Controls.Add(groupBox3);
            Controls.Add(groupBox1);
            MaximizeBox = false;
            MinimizeBox = false;
            Name = "ConfigBuilderForm";
            ShowIcon = false;
            ShowInTaskbar = false;
            Text = "ConfigBuilder";
            Load += ConfigBuilderForm_Load;
            groupBox1.ResumeLayout(false);
            groupBox5.ResumeLayout(false);
            groupBox5.PerformLayout();
            groupBox4.ResumeLayout(false);
            groupBox4.PerformLayout();
            groupBox2.ResumeLayout(false);
            groupBox2.PerformLayout();
            groupBox3.ResumeLayout(false);
            groupBox3.PerformLayout();
            contextMenuConfig.ResumeLayout(false);
            groupBox6.ResumeLayout(false);
            groupBox6.PerformLayout();
            ResumeLayout(false);
        }

        #endregion

        private TextBox txtConfigLink;
        private Button btnParse;
        private Button btnGenerateJson;
        private TextBox txtHost;
        private TextBox txtUUID;
        private TextBox txtTlsDomain;
        private TextBox txtPath;
        private TextBox txtSNI;
        private TextBox txtPort;
        private Label label2;
        private Label label3;
        private Label label4;
        private Label label5;
        private Label label6;
        private Button btnTlsTest;
        private Label lblStatus;
        private RichTextBox rtbLog;
        private GroupBox groupBox1;
        private GroupBox groupBox2;
        private GroupBox groupBox3;
        private Label label1;
        private Button btnOpenGenerated;
        private Label lblProtocol;
        private Label lblTemplate;
        private Label lblSecurity;
        private Label lblNetwork;
        private ContextMenuStrip contextMenuConfig;
        private ToolStripSeparator toolStripMenuItem1;
        private ToolStripMenuItem copyJSONToolStripMenuItem;
        private ToolStripMenuItem pastConfigFromClipBoardToolStripMenuItem;
        private ToolStripSeparator toolStripMenuItem2;
        private ToolStripMenuItem clearToolStripMenuItem;
        private GroupBox groupBox4;
        private Label label10;
        private Label label9;
        private Label label8;
        private Label label7;
        private GroupBox groupBox5;
        private Button btnTcpTest;
        private Button btnPing;
        private GroupBox groupBox6;
        private Button btnSaveProfile;
        private ComboBox cmbProfiles;
        private Button btnClearLog;
        private Label label11;
        private Button btnCreateTemplate;
    }
}