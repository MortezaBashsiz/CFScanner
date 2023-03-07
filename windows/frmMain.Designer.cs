namespace WinCFScan
{
    partial class frmMain
    {
        /// <summary>
        ///  Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        ///  Clean up any resources being used.
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
        ///  Required method for Designer support - do not modify
        ///  the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            components = new System.ComponentModel.Container();
            System.ComponentModel.ComponentResourceManager resources = new System.ComponentModel.ComponentResourceManager(typeof(frmMain));
            txtLog = new TextBox();
            timerBase = new System.Windows.Forms.Timer(components);
            labelLastIPChecked = new Label();
            lblLastIPRange = new Label();
            timerProgress = new System.Windows.Forms.Timer(components);
            groupBox1 = new GroupBox();
            toolStrip1 = new ToolStrip();
            btnStart = new ToolStripSplitButton();
            mnuPauseScan = new ToolStripMenuItem();
            toolStripSeparator2 = new ToolStripSeparator();
            comboConcurrent = new ToolStripComboBox();
            lblConcurrent = new ToolStripLabel();
            comboTargetSpeed = new ToolStripComboBox();
            lblTargetSpeed = new ToolStripLabel();
            comboConfigs = new ToolStripComboBox();
            toolStripLabel3 = new ToolStripLabel();
            toolStripSeparator1 = new ToolStripSeparator();
            prgOveral = new ToolStripProgressBar();
            toolStripLabel1 = new ToolStripLabel();
            btnSkipCurRange = new ToolStripButton();
            prgCurRange = new ToolStripProgressBar();
            toolStripLabel2 = new ToolStripLabel();
            lblDebugMode = new Label();
            linkGithub = new LinkLabel();
            btnCopyFastestIP = new Button();
            txtFastestIP = new TextBox();
            lblTotalWorkingIPs = new Label();
            toolTip1 = new ToolTip(components);
            comboResults = new ComboBox();
            btnScanInPrevResults = new Button();
            btnLoadIPRanges = new Button();
            listResults = new ListView();
            hdrDelay = new ColumnHeader();
            hdrIP = new ColumnHeader();
            mnuListView = new ContextMenuStrip(components);
            mnuListViewCopyIP = new ToolStripMenuItem();
            mnuListViewTestThisIPAddress = new ToolStripMenuItem();
            splitContainer1 = new SplitContainer();
            tabControl1 = new TabControl();
            tabPageCFRanges = new TabPage();
            lblCFIPListStatus = new Label();
            btnSelectNoneIPRanges = new Button();
            btnSelectAllIPRanges = new Button();
            listCFIPList = new ListView();
            headIPRange = new ColumnHeader();
            headTotalIPs = new ColumnHeader();
            tabPageResults = new TabPage();
            lblPrevListTotalIPs = new Label();
            lblPrevResults = new Label();
            btnResultsActions = new Button();
            mnuMain = new MenuStrip();
            fileToolStripMenuItem = new ToolStripMenuItem();
            loadCustomIPRangesToolStripMenuItem = new ToolStripMenuItem();
            toolStripMenuItem1 = new ToolStripSeparator();
            importScanResultsToolStripMenuItem = new ToolStripMenuItem();
            exportScanResultsToolStripMenuItem = new ToolStripMenuItem();
            toolStripMenuItem2 = new ToolStripSeparator();
            exitToolStripMenuItem = new ToolStripMenuItem();
            toolsToolStripMenuItem = new ToolStripMenuItem();
            scanASingleIPAddressToolStripMenuItem = new ToolStripMenuItem();
            addCustomV2rayConfigToolStripMenuItem = new ToolStripMenuItem();
            downloadTimeoutToolStripMenuItem = new ToolStripMenuItem();
            comboDownloadTimeout = new ToolStripComboBox();
            checkForUpdateToolStripMenuItem = new ToolStripMenuItem();
            openFileDialog1 = new OpenFileDialog();
            saveFileDialog1 = new SaveFileDialog();
            mnuResultsActions = new ContextMenuStrip(components);
            exportResultsToolStripMenuItem = new ToolStripMenuItem();
            importResultsToolStripMenuItem = new ToolStripMenuItem();
            deleteResultsToolStripMenuItem = new ToolStripMenuItem();
            toolStripBottom = new ToolStrip();
            btnFrontingErrors = new ToolStripSplitButton();
            mnuCopyFrontingErrors = new ToolStripMenuItem();
            toolStripSeparator3 = new ToolStripSeparator();
            btnDownloadErrors = new ToolStripSplitButton();
            mnuCopyDownloadErrors = new ToolStripMenuItem();
            groupBox1.SuspendLayout();
            toolStrip1.SuspendLayout();
            mnuListView.SuspendLayout();
            ((System.ComponentModel.ISupportInitialize)splitContainer1).BeginInit();
            splitContainer1.Panel1.SuspendLayout();
            splitContainer1.Panel2.SuspendLayout();
            splitContainer1.SuspendLayout();
            tabControl1.SuspendLayout();
            tabPageCFRanges.SuspendLayout();
            tabPageResults.SuspendLayout();
            mnuMain.SuspendLayout();
            mnuResultsActions.SuspendLayout();
            toolStripBottom.SuspendLayout();
            SuspendLayout();
            // 
            // txtLog
            // 
            txtLog.BackColor = Color.FromArgb(21, 23, 24);
            txtLog.Dock = DockStyle.Fill;
            txtLog.Font = new Font("Consolas", 9F, FontStyle.Regular, GraphicsUnit.Point);
            txtLog.ForeColor = Color.PaleTurquoise;
            txtLog.Location = new Point(0, 0);
            txtLog.Multiline = true;
            txtLog.Name = "txtLog";
            txtLog.ReadOnly = true;
            txtLog.ScrollBars = ScrollBars.Vertical;
            txtLog.Size = new Size(830, 178);
            txtLog.TabIndex = 1;
            txtLog.Text = "Welcome to Cloudflare IP Scanner.\r\n";
            // 
            // timerBase
            // 
            timerBase.Enabled = true;
            timerBase.Interval = 1500;
            timerBase.Tick += timerBase_Tick;
            // 
            // labelLastIPChecked
            // 
            labelLastIPChecked.AutoSize = true;
            labelLastIPChecked.Location = new Point(11, 52);
            labelLastIPChecked.Name = "labelLastIPChecked";
            labelLastIPChecked.Size = new Size(91, 15);
            labelLastIPChecked.TabIndex = 1;
            labelLastIPChecked.Text = "Last checked IP:";
            // 
            // lblLastIPRange
            // 
            lblLastIPRange.AutoSize = true;
            lblLastIPRange.Location = new Point(11, 73);
            lblLastIPRange.Name = "lblLastIPRange";
            lblLastIPRange.Size = new Size(99, 15);
            lblLastIPRange.TabIndex = 0;
            lblLastIPRange.Text = "Current IP Range:";
            // 
            // timerProgress
            // 
            timerProgress.Interval = 300;
            timerProgress.Tick += timerProgress_Tick;
            // 
            // groupBox1
            // 
            groupBox1.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            groupBox1.Controls.Add(toolStrip1);
            groupBox1.Controls.Add(lblDebugMode);
            groupBox1.Controls.Add(linkGithub);
            groupBox1.Controls.Add(btnCopyFastestIP);
            groupBox1.Controls.Add(txtFastestIP);
            groupBox1.Controls.Add(lblTotalWorkingIPs);
            groupBox1.Controls.Add(labelLastIPChecked);
            groupBox1.Controls.Add(lblLastIPRange);
            groupBox1.Location = new Point(12, 21);
            groupBox1.Name = "groupBox1";
            groupBox1.Padding = new Padding(3, 0, 3, 3);
            groupBox1.Size = new Size(830, 120);
            groupBox1.TabIndex = 3;
            groupBox1.TabStop = false;
            // 
            // toolStrip1
            // 
            toolStrip1.GripStyle = ToolStripGripStyle.Hidden;
            toolStrip1.Items.AddRange(new ToolStripItem[] { btnStart, toolStripSeparator2, comboConcurrent, lblConcurrent, comboTargetSpeed, lblTargetSpeed, comboConfigs, toolStripLabel3, toolStripSeparator1, prgOveral, toolStripLabel1, btnSkipCurRange, prgCurRange, toolStripLabel2 });
            toolStrip1.Location = new Point(3, 16);
            toolStrip1.Name = "toolStrip1";
            toolStrip1.RightToLeft = RightToLeft.Yes;
            toolStrip1.Size = new Size(824, 33);
            toolStrip1.TabIndex = 16;
            toolStrip1.Text = "toolStrip1";
            // 
            // btnStart
            // 
            btnStart.AutoSize = false;
            btnStart.BackColor = SystemColors.Control;
            btnStart.DisplayStyle = ToolStripItemDisplayStyle.Text;
            btnStart.DropDownItems.AddRange(new ToolStripItem[] { mnuPauseScan });
            btnStart.Font = new Font("Segoe UI Semibold", 9F, FontStyle.Bold, GraphicsUnit.Point);
            btnStart.ForeColor = SystemColors.ControlText;
            btnStart.Image = (Image)resources.GetObject("btnStart.Image");
            btnStart.ImageTransparentColor = Color.Magenta;
            btnStart.Name = "btnStart";
            btnStart.RightToLeft = RightToLeft.No;
            btnStart.Size = new Size(95, 30);
            btnStart.Text = "Start Scan";
            btnStart.ToolTipText = "Scan in selected IP ranges of Cloudflare";
            btnStart.ButtonClick += btnStart_ButtonClick;
            btnStart.Click += btnStart_Click;
            // 
            // mnuPauseScan
            // 
            mnuPauseScan.Name = "mnuPauseScan";
            mnuPauseScan.Size = new Size(134, 22);
            mnuPauseScan.Text = "Pause Scan";
            mnuPauseScan.Visible = false;
            mnuPauseScan.Click += mnuPauseScan_Click;
            // 
            // toolStripSeparator2
            // 
            toolStripSeparator2.Name = "toolStripSeparator2";
            toolStripSeparator2.Size = new Size(6, 33);
            // 
            // comboConcurrent
            // 
            comboConcurrent.AutoSize = false;
            comboConcurrent.DropDownWidth = 50;
            comboConcurrent.FlatStyle = FlatStyle.System;
            comboConcurrent.Items.AddRange(new object[] { "1", "2", "4", "8", "16", "32" });
            comboConcurrent.Name = "comboConcurrent";
            comboConcurrent.RightToLeft = RightToLeft.No;
            comboConcurrent.Size = new Size(50, 23);
            comboConcurrent.Text = "4";
            comboConcurrent.ToolTipText = "Number of parallel scan processes";
            comboConcurrent.TextChanged += comboConcurrent_TextChanged;
            // 
            // lblConcurrent
            // 
            lblConcurrent.Name = "lblConcurrent";
            lblConcurrent.Size = new Size(67, 30);
            lblConcurrent.Text = "Concurrent";
            // 
            // comboTargetSpeed
            // 
            comboTargetSpeed.AutoSize = false;
            comboTargetSpeed.DropDownStyle = ComboBoxStyle.DropDownList;
            comboTargetSpeed.FlatStyle = FlatStyle.System;
            comboTargetSpeed.Items.AddRange(new object[] { "20 KB/s", "50 KB/s", "100 KB/s", "200 KB/s", "500 KB/s" });
            comboTargetSpeed.Name = "comboTargetSpeed";
            comboTargetSpeed.RightToLeft = RightToLeft.No;
            comboTargetSpeed.Size = new Size(70, 23);
            comboTargetSpeed.ToolTipText = "Target Speed";
            comboTargetSpeed.SelectedIndexChanged += comboTargetSpeed_SelectedIndexChanged;
            // 
            // lblTargetSpeed
            // 
            lblTargetSpeed.Name = "lblTargetSpeed";
            lblTargetSpeed.Size = new Size(74, 30);
            lblTargetSpeed.Text = "Target Speed";
            // 
            // comboConfigs
            // 
            comboConfigs.AutoSize = false;
            comboConfigs.DropDownStyle = ComboBoxStyle.DropDownList;
            comboConfigs.DropDownWidth = 130;
            comboConfigs.FlatStyle = FlatStyle.System;
            comboConfigs.Items.AddRange(new object[] { "Default" });
            comboConfigs.Name = "comboConfigs";
            comboConfigs.RightToLeft = RightToLeft.No;
            comboConfigs.Size = new Size(90, 23);
            comboConfigs.ToolTipText = "v2ray configs";
            comboConfigs.SelectedIndexChanged += comboConfigs_SelectedIndexChanged;
            // 
            // toolStripLabel3
            // 
            toolStripLabel3.Name = "toolStripLabel3";
            toolStripLabel3.Size = new Size(43, 30);
            toolStripLabel3.Text = "Config";
            // 
            // toolStripSeparator1
            // 
            toolStripSeparator1.Name = "toolStripSeparator1";
            toolStripSeparator1.Size = new Size(6, 33);
            // 
            // prgOveral
            // 
            prgOveral.AutoSize = false;
            prgOveral.Name = "prgOveral";
            prgOveral.Size = new Size(90, 22);
            prgOveral.ToolTipText = "Overal progress";
            // 
            // toolStripLabel1
            // 
            toolStripLabel1.Name = "toolStripLabel1";
            toolStripLabel1.Size = new Size(41, 30);
            toolStripLabel1.Text = "Overal";
            // 
            // btnSkipCurRange
            // 
            btnSkipCurRange.AutoSize = false;
            btnSkipCurRange.DisplayStyle = ToolStripItemDisplayStyle.Text;
            btnSkipCurRange.Enabled = false;
            btnSkipCurRange.Image = (Image)resources.GetObject("btnSkipCurRange.Image");
            btnSkipCurRange.ImageTransparentColor = Color.Magenta;
            btnSkipCurRange.Name = "btnSkipCurRange";
            btnSkipCurRange.Size = new Size(33, 23);
            btnSkipCurRange.Text = "Skip";
            btnSkipCurRange.ToolTipText = "Skip curent IP range";
            btnSkipCurRange.Click += btnSkipCurRange_Click;
            // 
            // prgCurRange
            // 
            prgCurRange.AutoSize = false;
            prgCurRange.Name = "prgCurRange";
            prgCurRange.Size = new Size(90, 22);
            prgCurRange.ToolTipText = "Current IP range progress";
            // 
            // toolStripLabel2
            // 
            toolStripLabel2.Name = "toolStripLabel2";
            toolStripLabel2.Size = new Size(47, 30);
            toolStripLabel2.Text = "Current";
            // 
            // lblDebugMode
            // 
            lblDebugMode.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            lblDebugMode.AutoSize = true;
            lblDebugMode.BackColor = SystemColors.Control;
            lblDebugMode.Font = new Font("Segoe UI", 9F, FontStyle.Bold, GraphicsUnit.Point);
            lblDebugMode.ForeColor = Color.Red;
            lblDebugMode.Location = new Point(600, 91);
            lblDebugMode.Name = "lblDebugMode";
            lblDebugMode.Size = new Size(143, 15);
            lblDebugMode.TabIndex = 13;
            lblDebugMode.Text = "DEBUG MODE is Enabled";
            toolTip1.SetToolTip(lblDebugMode, "To exit debug mode delete 'enable-debug' file in the app directory.\r\nIn debug mode you can not set concurrent processes.");
            lblDebugMode.Visible = false;
            // 
            // linkGithub
            // 
            linkGithub.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            linkGithub.Image = Properties.Resources.github_mark24;
            linkGithub.ImageAlign = ContentAlignment.MiddleLeft;
            linkGithub.Location = new Point(749, 88);
            linkGithub.Name = "linkGithub";
            linkGithub.Size = new Size(71, 23);
            linkGithub.TabIndex = 12;
            linkGithub.TabStop = true;
            linkGithub.Text = "GitHub";
            linkGithub.TextAlign = ContentAlignment.MiddleRight;
            toolTip1.SetToolTip(linkGithub, "Visit us on GitHub.com");
            linkGithub.LinkClicked += linkGithub_LinkClicked;
            // 
            // btnCopyFastestIP
            // 
            btnCopyFastestIP.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnCopyFastestIP.Location = new Point(673, 55);
            btnCopyFastestIP.Name = "btnCopyFastestIP";
            btnCopyFastestIP.Size = new Size(151, 25);
            btnCopyFastestIP.TabIndex = 10;
            btnCopyFastestIP.Text = "Copy fastest IP";
            btnCopyFastestIP.UseVisualStyleBackColor = true;
            btnCopyFastestIP.Click += btnCopyFastestIP_Click;
            // 
            // txtFastestIP
            // 
            txtFastestIP.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            txtFastestIP.BackColor = Color.White;
            txtFastestIP.Font = new Font("Segoe UI", 9F, FontStyle.Bold, GraphicsUnit.Point);
            txtFastestIP.ForeColor = Color.Green;
            txtFastestIP.Location = new Point(459, 57);
            txtFastestIP.Name = "txtFastestIP";
            txtFastestIP.PlaceholderText = "Fastest IP";
            txtFastestIP.ReadOnly = true;
            txtFastestIP.Size = new Size(208, 23);
            txtFastestIP.TabIndex = 11;
            // 
            // lblTotalWorkingIPs
            // 
            lblTotalWorkingIPs.AutoSize = true;
            lblTotalWorkingIPs.Location = new Point(11, 94);
            lblTotalWorkingIPs.Name = "lblTotalWorkingIPs";
            lblTotalWorkingIPs.Size = new Size(143, 15);
            lblTotalWorkingIPs.TabIndex = 6;
            lblTotalWorkingIPs.Text = "Total working IPs found: 0";
            // 
            // comboResults
            // 
            comboResults.DropDownStyle = ComboBoxStyle.DropDownList;
            comboResults.FormattingEnabled = true;
            comboResults.Location = new Point(128, 10);
            comboResults.Name = "comboResults";
            comboResults.Size = new Size(215, 23);
            comboResults.TabIndex = 5;
            toolTip1.SetToolTip(comboResults, "List of last scan results");
            comboResults.SelectedIndexChanged += comboResults_SelectedIndexChanged;
            // 
            // btnScanInPrevResults
            // 
            btnScanInPrevResults.Location = new Point(396, 10);
            btnScanInPrevResults.Name = "btnScanInPrevResults";
            btnScanInPrevResults.Size = new Size(131, 24);
            btnScanInPrevResults.TabIndex = 6;
            btnScanInPrevResults.Text = "Scan in results";
            toolTip1.SetToolTip(btnScanInPrevResults, "Scan again in this ip results");
            btnScanInPrevResults.UseVisualStyleBackColor = true;
            btnScanInPrevResults.Click += btnScanInPrevResults_Click;
            // 
            // btnLoadIPRanges
            // 
            btnLoadIPRanges.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnLoadIPRanges.Location = new Point(531, 8);
            btnLoadIPRanges.Name = "btnLoadIPRanges";
            btnLoadIPRanges.Size = new Size(94, 23);
            btnLoadIPRanges.TabIndex = 4;
            btnLoadIPRanges.Text = "Load IP ranges";
            toolTip1.SetToolTip(btnLoadIPRanges, "Load your custom IP ranges");
            btnLoadIPRanges.UseVisualStyleBackColor = true;
            btnLoadIPRanges.Click += btnLoadIPRanges_Click;
            // 
            // listResults
            // 
            listResults.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            listResults.Columns.AddRange(new ColumnHeader[] { hdrDelay, hdrIP });
            listResults.FullRowSelect = true;
            listResults.GridLines = true;
            listResults.Location = new Point(0, 40);
            listResults.Name = "listResults";
            listResults.Size = new Size(822, 174);
            listResults.TabIndex = 4;
            listResults.UseCompatibleStateImageBehavior = false;
            listResults.View = View.Details;
            listResults.ColumnClick += listResults_ColumnClick;
            listResults.MouseClick += listResults_MouseClick;
            listResults.MouseDoubleClick += listResults_MouseDoubleClick;
            // 
            // hdrDelay
            // 
            hdrDelay.Text = "Delay";
            hdrDelay.Width = 90;
            // 
            // hdrIP
            // 
            hdrIP.Text = "IP Address";
            hdrIP.Width = 160;
            // 
            // mnuListView
            // 
            mnuListView.Items.AddRange(new ToolStripItem[] { mnuListViewCopyIP, mnuListViewTestThisIPAddress });
            mnuListView.Name = "mnuListView";
            mnuListView.Size = new Size(173, 48);
            // 
            // mnuListViewCopyIP
            // 
            mnuListViewCopyIP.Name = "mnuListViewCopyIP";
            mnuListViewCopyIP.Size = new Size(172, 22);
            mnuListViewCopyIP.Text = "Copy IP address";
            mnuListViewCopyIP.Click += mnuListViewCopyIP_Click;
            // 
            // mnuListViewTestThisIPAddress
            // 
            mnuListViewTestThisIPAddress.Name = "mnuListViewTestThisIPAddress";
            mnuListViewTestThisIPAddress.Size = new Size(172, 22);
            mnuListViewTestThisIPAddress.Text = "Test this IP address";
            mnuListViewTestThisIPAddress.Click += mnuListViewTestThisIPAddress_Click;
            // 
            // splitContainer1
            // 
            splitContainer1.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            splitContainer1.Location = new Point(12, 147);
            splitContainer1.Name = "splitContainer1";
            splitContainer1.Orientation = Orientation.Horizontal;
            // 
            // splitContainer1.Panel1
            // 
            splitContainer1.Panel1.Controls.Add(tabControl1);
            // 
            // splitContainer1.Panel2
            // 
            splitContainer1.Panel2.Controls.Add(txtLog);
            splitContainer1.Size = new Size(830, 424);
            splitContainer1.SplitterDistance = 242;
            splitContainer1.TabIndex = 7;
            // 
            // tabControl1
            // 
            tabControl1.Controls.Add(tabPageCFRanges);
            tabControl1.Controls.Add(tabPageResults);
            tabControl1.Dock = DockStyle.Fill;
            tabControl1.Location = new Point(0, 0);
            tabControl1.Name = "tabControl1";
            tabControl1.SelectedIndex = 0;
            tabControl1.Size = new Size(830, 242);
            tabControl1.TabIndex = 9;
            // 
            // tabPageCFRanges
            // 
            tabPageCFRanges.Controls.Add(btnLoadIPRanges);
            tabPageCFRanges.Controls.Add(lblCFIPListStatus);
            tabPageCFRanges.Controls.Add(btnSelectNoneIPRanges);
            tabPageCFRanges.Controls.Add(btnSelectAllIPRanges);
            tabPageCFRanges.Controls.Add(listCFIPList);
            tabPageCFRanges.Location = new Point(4, 24);
            tabPageCFRanges.Name = "tabPageCFRanges";
            tabPageCFRanges.Padding = new Padding(3);
            tabPageCFRanges.Size = new Size(822, 214);
            tabPageCFRanges.TabIndex = 1;
            tabPageCFRanges.Text = "Cloudflare IP ranges";
            tabPageCFRanges.UseVisualStyleBackColor = true;
            // 
            // lblCFIPListStatus
            // 
            lblCFIPListStatus.AutoSize = true;
            lblCFIPListStatus.Location = new Point(6, 12);
            lblCFIPListStatus.Name = "lblCFIPListStatus";
            lblCFIPListStatus.Size = new Size(110, 15);
            lblCFIPListStatus.TabIndex = 3;
            lblCFIPListStatus.Text = "Loading IP ranges...";
            // 
            // btnSelectNoneIPRanges
            // 
            btnSelectNoneIPRanges.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnSelectNoneIPRanges.Location = new Point(631, 9);
            btnSelectNoneIPRanges.Name = "btnSelectNoneIPRanges";
            btnSelectNoneIPRanges.Size = new Size(88, 23);
            btnSelectNoneIPRanges.TabIndex = 2;
            btnSelectNoneIPRanges.Text = "Select None";
            btnSelectNoneIPRanges.UseVisualStyleBackColor = true;
            btnSelectNoneIPRanges.Click += btnSelectNoneIPRanges_Click;
            // 
            // btnSelectAllIPRanges
            // 
            btnSelectAllIPRanges.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnSelectAllIPRanges.Location = new Point(728, 9);
            btnSelectAllIPRanges.Name = "btnSelectAllIPRanges";
            btnSelectAllIPRanges.Size = new Size(88, 23);
            btnSelectAllIPRanges.TabIndex = 1;
            btnSelectAllIPRanges.Text = "Select All";
            btnSelectAllIPRanges.UseVisualStyleBackColor = true;
            btnSelectAllIPRanges.Click += btnSelectAllIPRanges_Click;
            // 
            // listCFIPList
            // 
            listCFIPList.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            listCFIPList.CheckBoxes = true;
            listCFIPList.Columns.AddRange(new ColumnHeader[] { headIPRange, headTotalIPs });
            listCFIPList.Location = new Point(0, 38);
            listCFIPList.Name = "listCFIPList";
            listCFIPList.Size = new Size(822, 210);
            listCFIPList.TabIndex = 0;
            listCFIPList.UseCompatibleStateImageBehavior = false;
            listCFIPList.View = View.Details;
            listCFIPList.ColumnClick += listCFIPList_ColumnClick;
            listCFIPList.ItemChecked += listCFIPList_ItemChecked;
            // 
            // headIPRange
            // 
            headIPRange.Text = "IP Range";
            headIPRange.Width = 120;
            // 
            // headTotalIPs
            // 
            headTotalIPs.Text = "Total IPs";
            headTotalIPs.Width = 90;
            // 
            // tabPageResults
            // 
            tabPageResults.Controls.Add(lblPrevListTotalIPs);
            tabPageResults.Controls.Add(lblPrevResults);
            tabPageResults.Controls.Add(btnResultsActions);
            tabPageResults.Controls.Add(comboResults);
            tabPageResults.Controls.Add(btnScanInPrevResults);
            tabPageResults.Controls.Add(listResults);
            tabPageResults.Location = new Point(4, 24);
            tabPageResults.Name = "tabPageResults";
            tabPageResults.Padding = new Padding(3);
            tabPageResults.Size = new Size(822, 214);
            tabPageResults.TabIndex = 0;
            tabPageResults.Text = "Scan Results";
            tabPageResults.UseVisualStyleBackColor = true;
            // 
            // lblPrevListTotalIPs
            // 
            lblPrevListTotalIPs.Location = new Point(343, 14);
            lblPrevListTotalIPs.Name = "lblPrevListTotalIPs";
            lblPrevListTotalIPs.Size = new Size(53, 19);
            lblPrevListTotalIPs.TabIndex = 9;
            lblPrevListTotalIPs.Text = "0 IPs";
            lblPrevListTotalIPs.TextAlign = ContentAlignment.TopCenter;
            // 
            // lblPrevResults
            // 
            lblPrevResults.AutoSize = true;
            lblPrevResults.Location = new Point(6, 13);
            lblPrevResults.Name = "lblPrevResults";
            lblPrevResults.Size = new Size(119, 15);
            lblPrevResults.TabIndex = 7;
            lblPrevResults.Text = "Previous scan results:";
            // 
            // btnResultsActions
            // 
            btnResultsActions.Location = new Point(538, 11);
            btnResultsActions.Name = "btnResultsActions";
            btnResultsActions.Size = new Size(131, 23);
            btnResultsActions.TabIndex = 8;
            btnResultsActions.Text = "Actions";
            btnResultsActions.UseVisualStyleBackColor = true;
            btnResultsActions.MouseClick += btnResultsActions_MouseClick;
            // 
            // mnuMain
            // 
            mnuMain.Items.AddRange(new ToolStripItem[] { fileToolStripMenuItem, toolsToolStripMenuItem });
            mnuMain.Location = new Point(0, 0);
            mnuMain.Name = "mnuMain";
            mnuMain.Size = new Size(854, 24);
            mnuMain.TabIndex = 8;
            mnuMain.Text = "menuStrip1";
            // 
            // fileToolStripMenuItem
            // 
            fileToolStripMenuItem.DropDownItems.AddRange(new ToolStripItem[] { loadCustomIPRangesToolStripMenuItem, toolStripMenuItem1, importScanResultsToolStripMenuItem, exportScanResultsToolStripMenuItem, toolStripMenuItem2, exitToolStripMenuItem });
            fileToolStripMenuItem.Name = "fileToolStripMenuItem";
            fileToolStripMenuItem.Size = new Size(37, 20);
            fileToolStripMenuItem.Text = "File";
            // 
            // loadCustomIPRangesToolStripMenuItem
            // 
            loadCustomIPRangesToolStripMenuItem.Name = "loadCustomIPRangesToolStripMenuItem";
            loadCustomIPRangesToolStripMenuItem.Size = new Size(194, 22);
            loadCustomIPRangesToolStripMenuItem.Text = "Load custom IP ranges";
            loadCustomIPRangesToolStripMenuItem.Click += loadCustomIPRangesToolStripMenuItem_Click;
            // 
            // toolStripMenuItem1
            // 
            toolStripMenuItem1.Name = "toolStripMenuItem1";
            toolStripMenuItem1.Size = new Size(191, 6);
            // 
            // importScanResultsToolStripMenuItem
            // 
            importScanResultsToolStripMenuItem.Name = "importScanResultsToolStripMenuItem";
            importScanResultsToolStripMenuItem.Size = new Size(194, 22);
            importScanResultsToolStripMenuItem.Text = "Import scan results";
            importScanResultsToolStripMenuItem.Click += importScanResultsToolStripMenuItem_Click;
            // 
            // exportScanResultsToolStripMenuItem
            // 
            exportScanResultsToolStripMenuItem.Name = "exportScanResultsToolStripMenuItem";
            exportScanResultsToolStripMenuItem.Size = new Size(194, 22);
            exportScanResultsToolStripMenuItem.Text = "Export scan results";
            exportScanResultsToolStripMenuItem.Click += exportScanResultsToolStripMenuItem_Click;
            // 
            // toolStripMenuItem2
            // 
            toolStripMenuItem2.Name = "toolStripMenuItem2";
            toolStripMenuItem2.Size = new Size(191, 6);
            // 
            // exitToolStripMenuItem
            // 
            exitToolStripMenuItem.Name = "exitToolStripMenuItem";
            exitToolStripMenuItem.Size = new Size(194, 22);
            exitToolStripMenuItem.Text = "Exit";
            exitToolStripMenuItem.Click += exitToolStripMenuItem_Click;
            // 
            // toolsToolStripMenuItem
            // 
            toolsToolStripMenuItem.DropDownItems.AddRange(new ToolStripItem[] { scanASingleIPAddressToolStripMenuItem, addCustomV2rayConfigToolStripMenuItem, downloadTimeoutToolStripMenuItem, checkForUpdateToolStripMenuItem });
            toolsToolStripMenuItem.Name = "toolsToolStripMenuItem";
            toolsToolStripMenuItem.Size = new Size(46, 20);
            toolsToolStripMenuItem.Text = "Tools";
            // 
            // scanASingleIPAddressToolStripMenuItem
            // 
            scanASingleIPAddressToolStripMenuItem.Name = "scanASingleIPAddressToolStripMenuItem";
            scanASingleIPAddressToolStripMenuItem.Size = new Size(216, 22);
            scanASingleIPAddressToolStripMenuItem.Text = "Test a single IP address";
            scanASingleIPAddressToolStripMenuItem.Click += scanASingleIPAddressToolStripMenuItem_Click;
            // 
            // addCustomV2rayConfigToolStripMenuItem
            // 
            addCustomV2rayConfigToolStripMenuItem.Name = "addCustomV2rayConfigToolStripMenuItem";
            addCustomV2rayConfigToolStripMenuItem.Size = new Size(216, 22);
            addCustomV2rayConfigToolStripMenuItem.Text = "Add custom v2ray config...";
            addCustomV2rayConfigToolStripMenuItem.Click += addCustomV2rayConfigToolStripMenuItem_Click;
            // 
            // downloadTimeoutToolStripMenuItem
            // 
            downloadTimeoutToolStripMenuItem.DropDownItems.AddRange(new ToolStripItem[] { comboDownloadTimeout });
            downloadTimeoutToolStripMenuItem.Name = "downloadTimeoutToolStripMenuItem";
            downloadTimeoutToolStripMenuItem.Size = new Size(216, 22);
            downloadTimeoutToolStripMenuItem.Text = "Download timeout";
            // 
            // comboDownloadTimeout
            // 
            comboDownloadTimeout.DropDownStyle = ComboBoxStyle.DropDownList;
            comboDownloadTimeout.FlatStyle = FlatStyle.System;
            comboDownloadTimeout.Items.AddRange(new object[] { "2 Seconds", "4 Seconds", "6 Seconds", "10 Seconds" });
            comboDownloadTimeout.Name = "comboDownloadTimeout";
            comboDownloadTimeout.Size = new Size(121, 23);
            comboDownloadTimeout.SelectedIndexChanged += comboDownloadTimeout_SelectedIndexChanged;
            // 
            // checkForUpdateToolStripMenuItem
            // 
            checkForUpdateToolStripMenuItem.Name = "checkForUpdateToolStripMenuItem";
            checkForUpdateToolStripMenuItem.Size = new Size(216, 22);
            checkForUpdateToolStripMenuItem.Text = "Check for updates";
            checkForUpdateToolStripMenuItem.Click += checkForUpdateToolStripMenuItem_Click;
            // 
            // openFileDialog1
            // 
            openFileDialog1.FileName = "openFileDialog1";
            openFileDialog1.RestoreDirectory = true;
            // 
            // saveFileDialog1
            // 
            saveFileDialog1.DefaultExt = "txt";
            saveFileDialog1.RestoreDirectory = true;
            // 
            // mnuResultsActions
            // 
            mnuResultsActions.Items.AddRange(new ToolStripItem[] { exportResultsToolStripMenuItem, importResultsToolStripMenuItem, deleteResultsToolStripMenuItem });
            mnuResultsActions.Name = "mnuResultsActions";
            mnuResultsActions.Size = new Size(148, 70);
            mnuResultsActions.Text = "Actions";
            // 
            // exportResultsToolStripMenuItem
            // 
            exportResultsToolStripMenuItem.Name = "exportResultsToolStripMenuItem";
            exportResultsToolStripMenuItem.Size = new Size(147, 22);
            exportResultsToolStripMenuItem.Text = "Export results";
            exportResultsToolStripMenuItem.Click += exportResultsToolStripMenuItem_Click;
            // 
            // importResultsToolStripMenuItem
            // 
            importResultsToolStripMenuItem.Name = "importResultsToolStripMenuItem";
            importResultsToolStripMenuItem.Size = new Size(147, 22);
            importResultsToolStripMenuItem.Text = "Import results";
            importResultsToolStripMenuItem.Click += importResultsToolStripMenuItem_Click;
            // 
            // deleteResultsToolStripMenuItem
            // 
            deleteResultsToolStripMenuItem.Name = "deleteResultsToolStripMenuItem";
            deleteResultsToolStripMenuItem.Size = new Size(147, 22);
            deleteResultsToolStripMenuItem.Text = "Delete results";
            deleteResultsToolStripMenuItem.Click += deleteResultsToolStripMenuItem_Click;
            // 
            // toolStripBottom
            // 
            toolStripBottom.Dock = DockStyle.Bottom;
            toolStripBottom.Items.AddRange(new ToolStripItem[] { btnFrontingErrors, toolStripSeparator3, btnDownloadErrors });
            toolStripBottom.Location = new Point(0, 574);
            toolStripBottom.Name = "toolStripBottom";
            toolStripBottom.Size = new Size(854, 25);
            toolStripBottom.TabIndex = 9;
            toolStripBottom.Text = "toolStrip2";
            // 
            // btnFrontingErrors
            // 
            btnFrontingErrors.BackColor = SystemColors.Control;
            btnFrontingErrors.DisplayStyle = ToolStripItemDisplayStyle.Text;
            btnFrontingErrors.DropDownItems.AddRange(new ToolStripItem[] { mnuCopyFrontingErrors });
            btnFrontingErrors.ForeColor = SystemColors.ControlText;
            btnFrontingErrors.Image = (Image)resources.GetObject("btnFrontingErrors.Image");
            btnFrontingErrors.ImageTransparentColor = Color.Magenta;
            btnFrontingErrors.Name = "btnFrontingErrors";
            btnFrontingErrors.Size = new Size(123, 22);
            btnFrontingErrors.Text = "Fronting Errors: 0%";
            btnFrontingErrors.ButtonClick += btnFrontingErrors_ButtonClick;
            // 
            // mnuCopyFrontingErrors
            // 
            mnuCopyFrontingErrors.Name = "mnuCopyFrontingErrors";
            mnuCopyFrontingErrors.Size = new Size(181, 22);
            mnuCopyFrontingErrors.Text = "Copy fronting errors";
            mnuCopyFrontingErrors.Click += mnuCopyFrontingErrors_Click;
            // 
            // toolStripSeparator3
            // 
            toolStripSeparator3.Name = "toolStripSeparator3";
            toolStripSeparator3.Size = new Size(6, 25);
            // 
            // btnDownloadErrors
            // 
            btnDownloadErrors.BackColor = SystemColors.Control;
            btnDownloadErrors.DisplayStyle = ToolStripItemDisplayStyle.Text;
            btnDownloadErrors.DropDownItems.AddRange(new ToolStripItem[] { mnuCopyDownloadErrors });
            btnDownloadErrors.ForeColor = SystemColors.ControlText;
            btnDownloadErrors.Image = (Image)resources.GetObject("btnDownloadErrors.Image");
            btnDownloadErrors.ImageTransparentColor = Color.Magenta;
            btnDownloadErrors.Name = "btnDownloadErrors";
            btnDownloadErrors.Size = new Size(132, 22);
            btnDownloadErrors.Text = "Download Errors: 0%";
            btnDownloadErrors.ButtonClick += btnDownloadErrors_ButtonClick;
            // 
            // mnuCopyDownloadErrors
            // 
            mnuCopyDownloadErrors.Name = "mnuCopyDownloadErrors";
            mnuCopyDownloadErrors.Size = new Size(191, 22);
            mnuCopyDownloadErrors.Text = "Copy download errors";
            mnuCopyDownloadErrors.Click += mnuCopyDownloadErrors_Click;
            // 
            // frmMain
            // 
            AutoScaleDimensions = new SizeF(7F, 15F);
            AutoScaleMode = AutoScaleMode.Font;
            ClientSize = new Size(854, 599);
            Controls.Add(toolStripBottom);
            Controls.Add(mnuMain);
            Controls.Add(splitContainer1);
            Controls.Add(groupBox1);
            Icon = (Icon)resources.GetObject("$this.Icon");
            MinimumSize = new Size(870, 480);
            Name = "frmMain";
            Text = "Cloudflare Scan";
            FormClosing += frmMain_FormClosing;
            groupBox1.ResumeLayout(false);
            groupBox1.PerformLayout();
            toolStrip1.ResumeLayout(false);
            toolStrip1.PerformLayout();
            mnuListView.ResumeLayout(false);
            splitContainer1.Panel1.ResumeLayout(false);
            splitContainer1.Panel2.ResumeLayout(false);
            splitContainer1.Panel2.PerformLayout();
            ((System.ComponentModel.ISupportInitialize)splitContainer1).EndInit();
            splitContainer1.ResumeLayout(false);
            tabControl1.ResumeLayout(false);
            tabPageCFRanges.ResumeLayout(false);
            tabPageCFRanges.PerformLayout();
            tabPageResults.ResumeLayout(false);
            tabPageResults.PerformLayout();
            mnuMain.ResumeLayout(false);
            mnuMain.PerformLayout();
            mnuResultsActions.ResumeLayout(false);
            toolStripBottom.ResumeLayout(false);
            toolStripBottom.PerformLayout();
            ResumeLayout(false);
            PerformLayout();
        }

        #endregion
        private TextBox txtLog;
        private System.Windows.Forms.Timer timerBase;
        private Label lblLastIPRange;
        private System.Windows.Forms.Timer timerProgress;
        private Label labelLastIPChecked;
        private GroupBox groupBox1;
        private ToolTip toolTip1;
        private ListView listResults;
        private ColumnHeader hdrDelay;
        private ColumnHeader hdrIP;
        private ComboBox comboResults;
        private Label lblTotalWorkingIPs;
        private Button btnScanInPrevResults;
        private SplitContainer splitContainer1;
        private Label lblPrevResults;
        private Button btnResultsActions;
        private TextBox txtFastestIP;
        private Button btnCopyFastestIP;
        private ContextMenuStrip mnuListView;
        private ToolStripMenuItem mnuListViewCopyIP;
        private ToolStripMenuItem mnuListViewTestThisIPAddress;
        private TabControl tabControl1;
        private TabPage tabPageResults;
        private TabPage tabPageCFRanges;
        private ListView listCFIPList;
        private ColumnHeader headIPRange;
        private ColumnHeader headTotalIPs;
        private Button btnSelectNoneIPRanges;
        private Button btnSelectAllIPRanges;
        private Label lblCFIPListStatus;
        private Label lblPrevListTotalIPs;
        private LinkLabel linkGithub;
        private MenuStrip mnuMain;
        private ToolStripMenuItem fileToolStripMenuItem;
        private ToolStripMenuItem exitToolStripMenuItem;
        private ToolStripMenuItem toolsToolStripMenuItem;
        private ToolStripMenuItem scanASingleIPAddressToolStripMenuItem;
        private Label lblDebugMode;
        private Button btnLoadIPRanges;
        private OpenFileDialog openFileDialog1;
        private ToolStripMenuItem loadCustomIPRangesToolStripMenuItem;
        private SaveFileDialog saveFileDialog1;
        private ToolStripMenuItem exportScanResultsToolStripMenuItem;
        private ContextMenuStrip mnuResultsActions;
        private ToolStripMenuItem exportResultsToolStripMenuItem;
        private ToolStripMenuItem importResultsToolStripMenuItem;
        private ToolStripMenuItem deleteResultsToolStripMenuItem;
        private ToolStripSeparator toolStripMenuItem1;
        private ToolStripMenuItem importScanResultsToolStripMenuItem;
        private ToolStripSeparator toolStripMenuItem2;
        private ToolStrip toolStrip1;
        private ToolStripComboBox comboConcurrent;
        private ToolStripProgressBar prgOveral;
        private ToolStripLabel lblConcurrent;
        private ToolStripComboBox comboTargetSpeed;
        private ToolStripLabel lblTargetSpeed;
        private ToolStripProgressBar prgCurRange;
        private ToolStripLabel toolStripLabel1;
        private ToolStripLabel toolStripLabel2;
        private ToolStripSeparator toolStripSeparator2;
        private ToolStripSeparator toolStripSeparator1;
        private ToolStripSplitButton btnStart;
        private ToolStripMenuItem mnuPauseScan;
        private ToolStripButton btnSkipCurRange;
        private ToolStripMenuItem checkForUpdateToolStripMenuItem;
        private ToolStripComboBox comboConfigs;
        private ToolStripLabel toolStripLabel3;
        private ToolStripMenuItem addCustomV2rayConfigToolStripMenuItem;
        private ToolStrip toolStripBottom;
        private ToolStripSplitButton btnFrontingErrors;
        private ToolStripSplitButton btnDownloadErrors;
        private ToolStripSeparator toolStripSeparator3;
        private ToolStripMenuItem mnuCopyDownloadErrors;
        private ToolStripMenuItem mnuCopyFrontingErrors;
        private ToolStripMenuItem downloadTimeoutToolStripMenuItem;
        private ToolStripComboBox comboDownloadTimeout;
    }
}