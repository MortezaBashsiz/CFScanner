using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace WinCFScan.Forms
{
    public partial class frmLogsDialog : Form
    {
        
        public frmLogsDialog()
        {
            InitializeComponent();
        }

        private void btnOK_Click(object sender, EventArgs e)
        {
            this.Close();
        }

        private void btnCopy_Click(object sender, EventArgs e)
        {
            setClipboard(txtLogs.Text);
        }

        public string LogText
        {
            get { return txtLogs.Text; }
            set { setTextLog(value); }
        }

        delegate void SetTextCallback(string log, bool sendToScreenReaderToo = false);
        public void setTextLog(string log, bool sendToScreenReaderToo = false)
        {
            try
            {
                if (this.txtLogs.InvokeRequired)
                {
                    SetTextCallback d = new SetTextCallback(setTextLog);
                    this.Invoke(d, new object[] { log, sendToScreenReaderToo });
                }
                else
                {
                    txtLogs.AppendText(log + Environment.NewLine);
                    //if (sendToScreenReaderToo)
                        //sendScreenReaderMsg(log);
                }
            }
            catch (Exception ex)
            {
            }
        }

        private bool setClipboard(string text)
        {
            try
            {
                Clipboard.SetText(text);
                return true;
            }
            catch (Exception ex) { }

            return false;
        }
    }
}
