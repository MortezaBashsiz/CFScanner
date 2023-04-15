package logger

import (
	"CFScanner/utils"
	"fmt"
	"os"
	"strings"
	"time"
)

type LogStatus string

var (
	red    = utils.Colors.FAIL
	green  = utils.Colors.OKGREEN
	yellow = utils.Colors.WARNING
	reset  = utils.Colors.ENDC
)

func (m *ScannerManage) String() string {
	builder := &strings.Builder{}
	builder.WriteString(time.Now().Format("2006/01/02 15:04:05"))
	builder.WriteString(" ")

	switch m.Status {
	case FailStatus:
		builder.WriteString(red)
	case ErrorStatus:
		builder.WriteString(red)
	case InfoStatus:
		builder.WriteString(yellow)
	case OKStatus:
		builder.WriteString(green)
	}

	builder.WriteString(string(m.Status))
	builder.WriteString(reset)
	builder.WriteByte(' ')
	builder.WriteString(m.IP)
	builder.WriteByte(' ')
	builder.WriteString(reset)

	if status := string(m.Status); len(status) > 0 {
		switch m.Status {
		case FailStatus:
			builder.WriteString(yellow)
		case OKStatus:
			builder.WriteString(green)
		}
		builder.WriteString(" ")
		builder.WriteString(fmt.Sprintf("%v", m.Message))
		builder.WriteString(reset)
	}

	if err := m.Cause; err != "" {
		builder.WriteString(red)
		builder.WriteString(" ")
		builder.WriteString(m.Cause)
		builder.WriteString(reset)
	}

	return builder.String()
}

func (m *ScannerManage) Print() {
	fmt.Fprintln(os.Stdout, m.String())
}
