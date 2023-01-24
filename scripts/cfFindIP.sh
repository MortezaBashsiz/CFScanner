#!/bin/bash 
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

# Check if 'parallel', 'nmap' and 'bc' packages are installed
# If they are not, install them
if [[ "$(uname)" == "Linux" ]]; then
    command -v parallel >/dev/null 2>&1 || { echo >&2 "I require 'parallel' but it's not installed. Installing now..."; apt-get install -y parallel; }
    command -v nmap >/dev/null 2>&1 || { echo >&2 "I require 'nmap' but it's not installed. Installing now..."; apt-get install -y nmap; }
    command -v bc >/dev/null 2>&1 || { echo >&2 "I require 'bc' but it's not installed. Installing now..."; apt-get install -y bc; }
elif [[ "$(uname)" == "Darwin" ]]; then
    command -v parallel >/dev/null 2>&1 || { echo >&2 "I require 'parallel' but it's not installed. Installing now..."; brew install parallel; }
    command -v nmap >/dev/null 2>&1 || { echo >&2 "I require 'nmap' but it's not installed. Installing now..."; brew install nmap; }
    command -v bc >/dev/null 2>&1 || { echo >&2 "I require 'bc' but it's not installed. Installing now..."; brew install bc; }
    command -v gtimeout >/dev/null 2>&1 || { echo >&2 "I require 'gtimeout' but it's not installed. Installing now..."; brew install coreutils; }
fi

threads="$1"

cloudFlareIpList=$(curl -s -XGET https://www.cloudflare.com/ips-v4)
now=$(date +"%Y%m%d-%H%M%S")
scriptDir=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
resultDir="$scriptDir"
resultFile="$resultDir/$now-result.txt"


# Function fncCheckIP
# Check IP Address
function fncCheckIP {
    local ip resultFile timeoutCommand
    ip="$1"
    resultFile="$2"
    if [[ "$(uname)" == "Darwin" ]]; then
        timeoutCommand="gtimeout"
    else
        timeoutCommand="timeout"
    fi
    if $timeoutCommand 1 nc -z "$ip" 443; then
        timeMil=$($timeoutCommand 2 curl -s -w '%{time_total}\n' --resolve scan.sudoer.net:443:"$ip" https://scan.sudoer.net/data.100K --output /dev/null | xargs -I {} echo "{} * 1000 /1" | bc )
        ping=$(ping -c 1 -W 1 "$ip" | tail -1| awk '{print $4}' | cut -d '/' -f 2)
        if [[ "$timeMil" && "$ping" ]]
        then
            echo "OK $ip Latency: $timeMil PingTime: $ping"
            echo "$ip $timeMil $ping" >> "$resultFile"
        fi
    else
        echo "FAILED $ip"
    fi
}
# End of Function fncCheckIP
export -f fncCheckIP

echo "" > "$resultFile"

for subNet in ${cloudFlareIpList}
do
    ipList=$(nmap -sL -n "$subNet" | awk '/Nmap scan report/{print $NF}')
    parallel -j "$threads" fncCheckIP ::: "$ipList" ::: "$resultFile"
done

sort -n -k3 "$resultFile" -o "$resultFile"

#sort based on the best overall ping time and minimum latency
sort -n -k2,3 "$resultFile" -o "$resultFile"

