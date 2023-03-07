#!/bin/bash  -
#===============================================================================
#
#          FILE: listIpBest.sh
#
#         USAGE: ./listIpBest.sh
#
#   DESCRIPTION: 
#
#       OPTIONS: ---
#  REQUIREMENTS: ---
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: Morteza Bashsiz (mb), morteza.bashsiz@gmail.com
#  ORGANIZATION: Linux
#       CREATED: 01/25/2023 10:44:40 AM
#      REVISION:  ---
#===============================================================================

set -o nounset                                  # Treat unset variables as an error

# Function fncGetBests
# get best IPs
function fncGetBests {
	today=$(date +"%b-%d")
	yesterday=$(date +"%b-%d" -d "1 day ago")
	# shellcheck disable=SC2044
	for file in $(find /var/log/cfapi -type f -iname "*$today*.log" -o -iname "*$yesterday*.log")
	do 
		newIpList=$(sort -n "$file" | head -n 5 | sort -n | awk -F ',' '{print $3}' | uniq)
		for ip in ${newIpList[@]}
		do
			zones=$(rgrep --no-filename "$ip$" /var/log/cfapi/ | sort -n | awk -F ',' '{ print $2 }' | sort | uniq)
			echo -e "$ip\t\t in : " ${zones}

		done
	done
}
# End of fncGetBests


fncGetBests | awk '{ print length, $0 }' | sort -n -s -r | cut -d" " -f2- | uniq 

