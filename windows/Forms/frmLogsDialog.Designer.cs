namespace WinCFScan.Forms
{
    partial class frmLogsDialog
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
            btnOK = new Button();
            btnCopy = new Button();
            txtLogs = new TextBox();
            linkLabelHowTo = new LinkLabel();
            SuspendLayout();
            // 
            // btnOK
            // 
            btnOK.Anchor = AnchorStyles.Bottom | AnchorStyles.Right;
            btnOK.Location = new Point(637, 362);
            btnOK.Name = "btnOK";
            btnOK.Size = new Size(75, 23);
            btnOK.TabIndex = 0;
            btnOK.Text = "OK";
            btnOK.UseVisualStyleBackColor = true;
            btnOK.Click += btnOK_Click;
            // 
            // btnCopy
            // 
            btnCopy.Anchor = AnchorStyles.Bottom | AnchorStyles.Right;
            btnCopy.Location = new Point(556, 362);
            btnCopy.Name = "btnCopy";
            btnCopy.Size = new Size(75, 23);
            btnCopy.TabIndex = 1;
            btnCopy.Text = "Copy Logs";
            btnCopy.UseVisualStyleBackColor = true;
            btnCopy.Click += btnCopy_Click;
            // 
            // txtLogs
            // 
            txtLogs.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            txtLogs.BackColor = Color.FromArgb(21, 23, 24);
            txtLogs.Font = new Font("Consolas", 9F, FontStyle.Regular, GraphicsUnit.Point);
            txtLogs.ForeColor = Color.PaleTurquoise;
            txtLogs.Location = new Point(0, 0);
            txtLogs.Multiline = true;
            txtLogs.Name = "txtLogs";
            txtLogs.ReadOnly = true;
            txtLogs.ScrollBars = ScrollBars.Both;
            txtLogs.Size = new Size(723, 347);
            txtLogs.TabIndex = 2;
            // 
            // linkLabelHowTo
            // 
            linkLabelHowTo.AutoSize = true;
            linkLabelHowTo.Location = new Point(14, 364);
            linkLabelHowTo.Name = "linkLabelHowTo";
            linkLabelHowTo.Size = new Size(158, 15);
            linkLabelHowTo.TabIndex = 3;
            linkLabelHowTo.TabStop = true;
            linkLabelHowTo.Text = "Read about diagnose results.";
            linkLabelHowTo.LinkClicked += linkLabelHowTo_LinkClicked;
            // 
            // frmLogsDialog
            // 
            AutoScaleDimensions = new SizeF(7F, 15F);
            AutoScaleMode = AutoScaleMode.Font;
            ClientSize = new Size(724, 396);
            Controls.Add(linkLabelHowTo);
            Controls.Add(txtLogs);
            Controls.Add(btnCopy);
            Controls.Add(btnOK);
            Name = "frmLogsDialog";
            Text = "Logs";
            ResumeLayout(false);
            PerformLayout();
        }

        #endregion

        private Button btnOK;
        private Button btnCopy;
        private TextBox txtLogs;
        private LinkLabel linkLabelHowTo;
    }
}