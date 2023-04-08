package utils

type COLORS struct {
	OKBLUE  string
	OKGREEN string
	WARNING string
	FAIL    string
	ENDC    string
}

var Colors = COLORS{
	OKBLUE:  "\033[94m",
	OKGREEN: "\033[92m",
	WARNING: "\033[93m",
	FAIL:    "\033[91m",
	ENDC:    "\033[0m",
}
