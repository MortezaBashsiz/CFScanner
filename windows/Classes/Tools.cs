using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using WinCFScan.Classes.Config;

namespace WinCFScan.Classes
{
    internal class Tools
    {

        public static void logStep(string message)
        {
            if(ConfigManager.Instance.enableDebug)
            {
                LogControl.Write(message, "debug.txt");
            }
        }

        // input dialog
        public static string ShowDialog(string text, string caption)
        {
            Form prompt = new Form()
            {
                Width = 280,
                Height = 160,
                FormBorderStyle = FormBorderStyle.FixedDialog,
                Text = caption,
                StartPosition = FormStartPosition.CenterScreen
            };
            Label textLabel = new Label() { Left = 30, Top = 20, Text = text, Width = 150 };
            TextBox textBox = new TextBox() { Left = 30, Top = 40, Width = 200 };
            Button confirmation = new Button() { Text = "Ok", Left = 130, Width = 100, Top = 80, DialogResult = DialogResult.OK };
            confirmation.Click += (sender, e) => { prompt.Close(); };
            prompt.Controls.Add(textBox);
            prompt.Controls.Add(confirmation);
            prompt.Controls.Add(textLabel);
            prompt.AcceptButton = confirmation;
            prompt.MaximizeBox = false;                

            return prompt.ShowDialog() == DialogResult.OK ? textBox.Text : "";
        }
    }
}
