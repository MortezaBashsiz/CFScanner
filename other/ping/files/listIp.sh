#!/bin/bash  -
#===============================================================================
#
#          FILE: listIp.sh
#
#         USAGE: ./listIp.sh
#
#   DESCRIPTION: 
#
#       OPTIONS: ---
#  REQUIREMENTS: ---
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: Morteza Bashsiz (mb), morteza.bashsiz@gmail.com
#  ORGANIZATION: Linux
#       CREATED: 01/21/2023 08:41:38 PM
#      REVISION:  ---
#===============================================================================

set -o nounset                                  # Treat unset variables as an error

#IRC : Irancel
#MCI : Hamrah Aval
#RTL: Rightel
#SHT: Shatel
#ZTL: Zitel
#AST: Asiatek
#MBT: Mobinnet
#MKB: Mokhaberat


thisHour=$(date +"%b-%d-%Y-%H")
lastHour=$(date +"%b-%d-%Y-%H" -d '1 hour ago')
now=$(date +"%b-%d-%Y %H:%M:%S")
for file in $(find /var/log/cfapi -type f -iname "*$thisHour*.log" -o -iname "$lastHour")
do 
	provider=$(sort -n "$file" | head -n 1 | sort -n | awk -F ',' '{print $2}' | uniq)
	newIp=$(sort -n "$file" | head -n 1 | sort -n | awk -F ',' '{print $3}' | uniq)
	oldLine=$(grep "$provider" /var/www/html/best.cf.iran)
	oldIp=$(echo "$oldLine" | awk '{ print $2 }')
	oldHour=$(echo "$oldLine" | awk '{ print $3 }')
	if [[ "$newIp" != "$oldIp" ]]
	then
		result="$provider $newIp $now UTC"
		sed -i "s/.*$provider.*/$result/" /var/www/html/best.cf.iran
	else
		if [[ "$oldHour" != "$thisHour" ]] || [[ "$oldHour" != "$lastHour"  ]]
		then
			result="$provider $newIp \t $now UTC"
			sed -i "s/.*$provider.*/$result/" /var/www/html/best.cf.iran
		fi
	fi
done
