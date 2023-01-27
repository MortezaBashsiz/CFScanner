#!/bin/bash  -
#===============================================================================
#
#          FILE: cfFindIP.sh
#
#         USAGE: ./cfFindIP.sh [ThreadCount]
#
#   DESCRIPTION: Scan all 1.5 Mil CloudFlare IP addresses
#
#       OPTIONS: ---
#  REQUIREMENTS: ThreadCount (integer Number which defines the parallel processes count)
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: Morteza Bashsiz (mb), morteza.bashsiz@gmail.com
#  ORGANIZATION: Linux
#       CREATED: 01/20/2023 07:36:57 PM
#      REVISION:  ---
#===============================================================================

set -o nounset                                  # Treat unset variables as an error

threads="$1"

cloudFlareIpList=$(curl -s -XGET https://www.cloudflare.com/ips-v4)
now=$(date +"%Y%m%d-%H%M%S")
scriptDir=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
resultDir="$scriptDir/../result"
resultFile="$resultDir/$now-result.cf"

# Function fncCheckSubnet
# Check Subnet
function fncCheckSubnet {
	local ipList resultFile 
	ipList="$1"
	resultFile="$2"
	for ip in ${ipList}
	do
		if timeout 1 bash -c "</dev/tcp/$ip/443" > /dev/null 2>&1;
		then
			timeMil=$($timeoutCommand 2 curl_chrome101 -s -w "TIME: %{time_total}" --tlsv1.2 -servername scan.sudoer.net -H 'Host: scan.sudoer.net' --resolve scan.sudoer.net:443:$ip | grep "TIME" | tail -n 1 | awk '{print $2}' | xargs -I {} echo "{} * 1000 /1" | bc )
			if [[ "$timeMil" ]]
			then
				echo "OK $ip"
				echo "$timeMil $ip" >> "$resultFile"
			fi
		else
			echo "FAILED $ip"
		fi
	done
}
# End of Function fncCheckSubnet
export -f fncCheckSubnet

echo "" > "$resultFile"

for subNet in ${cloudFlareIpList}
do
	ipList=$(nmap -sL -n "$subNet" | awk '/Nmap scan report/{print $NF}')
	parallel -j "$threads" fncCheckSubnet ::: "$ipList" ::: "$resultFile"
done

