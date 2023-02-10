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
#       CREATED: 01/24/2023 07:36:57 PM
#      REVISION:  1 by Nomad
#===============================================================================

set -o nounset                                  # Treat unset variables as an error

osVersion="Linux"
# Check if 'parallel', 'timeout', 'nmap' and 'bc' packages are installed
# If they are not,exit the script
if [[ "$(uname)" == "Linux" ]]; then
		osVersion="Linux"
    command -v parallel >/dev/null 2>&1 || { echo >&2 "I require 'parallel' but it's not installed. Please install it and try again."; exit 1; }
    command -v nmap >/dev/null 2>&1 || { echo >&2 "I require 'nmap' but it's not installed. Please install it and try again."; exit 1; }
    command -v bc >/dev/null 2>&1 || { echo >&2 "I require 'bc' but it's not installed. Please install it and try again."; exit 1; }
		command -v timeout >/dev/null 2>&1 || { echo >&2 "I require 'timeout' but it's not installed. Please install it and try again."; exit 1; }

elif [[ "$(uname)" == "Darwin" ]];then
		osVersion="Mac"
    command -v parallel >/dev/null 2>&1 || { echo >&2 "I require 'parallel' but it's not installed. Please install it and try again."; exit 1; }
    command -v nmap >/dev/null 2>&1 || { echo >&2 "I require 'nmap' but it's not installed. Please install it and try again."; exit 1; }
    command -v bc >/dev/null 2>&1 || { echo >&2 "I require 'bc' but it's not installed. Please install it and try again."; exit 1; }
    command -v gtimeout >/dev/null 2>&1 || { echo >&2 "I require 'gtimeout' but it's not installed. Please install it and try again."; exit 1; }
fi

threads="$1"
config="$2"

cloudFlareASNList=( AS209242 )
cloudFlareOkList=(31 45 66 80 89 103 104 108 141 147 154 159 168 170 185 188 191 192 193 194 195 199 203 205 212)
now=$(date +"%Y%m%d-%H%M%S")
scriptDir=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
resultDir="$scriptDir/../result"
resultFile="$resultDir/$now-result.cf"
configDir="$scriptDir/../config"

configId="NULL"
configHost="NULL"
configPort="NULL"
configPath="NULL"
configServerName="NULL"

barCharDone="="
barCharTodo=" "
barSplitter='>'
barPercentageScale=2
progressBar=""

# Check if config file exists
if [[ -f "$config" ]]
then
	echo "reading config ..."
	configId=$(grep "^id" "$config" | awk -F ":" '{ print $2 }' | sed "s/ //g")	
	configHost=$(grep "^Host" "$config" | awk -F ":" '{ print $2 }' | sed "s/ //g")	
	configPort=$(grep "^Port" "$config" | awk -F ":" '{ print $2 }' | sed "s/ //g")	
	configPath=$(grep "^path" "$config" | awk -F ":" '{ print $2 }' | sed "s/ //g")	
	configServerName=$(grep "^serverName" "$config" | awk -F ":" '{ print $2 }' | sed "s/ //g")	
	if ! [[ "$configId" ]] || ! [[ $configHost ]] || ! [[ $configPath ]] || ! [[ $configServerName ]]
	then
		echo "config is not correct"
		exit 1
	fi
else
	echo "config file does not exist $config"
	exit 1
fi

#check if expected output folder exists and create if it's not availbe
if [ ! -d "$resultDir" ]; then
    mkdir -p "$resultDir"
fi
if [ ! -d "$configDir" ]; then
    mkdir -p "$configDir"
fi

# Function fncShowProgress
# Progress bar maker function (based on https://www.baeldung.com/linux/command-line-progress-bar)
function fncShowProgress {
  current="$1"
  total="$2"

  barSize="$(($(tput cols)-70))" # 70 cols for description characters
  spaces=20

  # calculate the progress in percentage 
  percent=$(bc <<< "scale=$barPercentageScale; 100 * $current / $total" )
  # The number of done and todo characters
  done=$(bc <<< "scale=0; $barSize * $percent / 100" )
  todo=$(bc <<< "scale=0; $barSize - $done")
  # build the done and todo sub-bars
  doneSubBar=$(printf "%${done}s" | tr " " "${barCharDone}")
  todoSubBar=$(printf "%${todo}s" | tr " " "${barCharTodo} - 1") # 1 for barSplitter
  spacesSubBar=$(printf "%${todo}s" | tr " " " ")

  # output the bar
  progressBar="| Progress bar of main IPs: [${doneSubBar}${barSplitter}${todoSubBar}] ${percent}%${spacesSubBar}" # Some end space for pretty formatting
}
# End of Function showProgress
# Function fncCheckSubnet
# Check Subnet
function fncCheckSubnet {
	local ipList scriptDir resultFile timeoutCommand domainFronting
	ipList="$1"
	resultFile="$3"
	scriptDir="$4"
	configId="$5"
	configHost="$6"
	configPort="$7"
	configPath="$8"
	configServerName="$9"
	osVersion="${10}"
	v2rayCommand="v2ray"
	configDir="$scriptDir/../config"
	# set proper command for linux
	if command -v timeout >/dev/null 2>&1; 
	then
	    timeoutCommand="timeout"
	else
		# set proper command for mac
		if command -v gtimeout >/dev/null 2>&1; 
		then
		    timeoutCommand="gtimeout"
		else
		    echo >&2 "I require 'timeout' command but it's not installed. Please install 'timeout' or an alternative command like 'gtimeout' and try again."
		    exit 1
		fi
	fi
	# set proper command for v2ray
	if [[ "$osVersion" == "Linux" ]]
	then
		v2rayCommand="v2ray"
	elif [[ "$osVersion" == "Mac"  ]]
	then
		v2rayCommand="v2ray-mac"
	else
		echo "OS not supported only Linux or Mac"
		exit 1
	fi
	for ip in ${ipList}
		do
			if $timeoutCommand 1 bash -c "</dev/tcp/$ip/443" > /dev/null 2>&1;
			then
				domainFronting=$($timeoutCommand 2 curl -s -w "%{http_code}\n" --tlsv1.2 -servername fronting.sudoer.net -H "Host: fronting.sudoer.net" --resolve fronting.sudoer.net:443:"$ip" https://fronting.sudoer.net -o /dev/null | grep '200')
				if [[ "$domainFronting" == "200" ]]
				then
					ipConfigFile="$configDir/config.json.$ip"
					cp "$scriptDir"/config.json.temp "$ipConfigFile"
					sed -i "s/IP.IP.IP.IP/$ip/g" "$ipConfigFile"
					ipO1=$(echo "$ip" | awk -F '.' '{print $1}')
					ipO2=$(echo "$ip" | awk -F '.' '{print $2}')
					ipO3=$(echo "$ip" | awk -F '.' '{print $3}')
					ipO4=$(echo "$ip" | awk -F '.' '{print $4}')
					port=$((ipO1 + ipO2 + ipO3 + ipO4))
					sed -i "s/PORTPORT/3$port/g" "$ipConfigFile"
					sed -i "s/IDID/$configId/g" "$ipConfigFile"
					sed -i "s/HOSTHOST/$configHost/g" "$ipConfigFile"
					sed -i "s/CFPORTCFPORT/$configPort/g" "$ipConfigFile"
					sed -i "s/ENDPOINTENDPOINT/$configPath/g" "$ipConfigFile"
					sed -i "s/RANDOMHOST/$configServerName/g" "$ipConfigFile"
					# shellcheck disable=SC2009
					pid=$(ps aux | grep config.json."$ip" | grep -v grep | awk '{ print $2 }')
					if [[ "$pid" ]]
					then
						kill -9 "$pid"
					fi
					nohup "$scriptDir"/"$v2rayCommand" -c "$ipConfigFile" > /dev/null &
					sleep 2
					timeMil=$($timeoutCommand 2 curl -x "socks5://127.0.0.1:3$port" -s -w "TIME: %{time_total}\n" https://scan.sudoer.net | grep "TIME" | tail -n 1 | awk '{print $2}' | xargs -I {} echo "{} * 1000 /1" | bc )
					# shellcheck disable=SC2009
					pid=$(ps aux | grep config.json."$ip" | grep -v grep | awk '{ print $2 }')
					if [[ "$pid" ]]
					then
						kill -9 "$pid" > /dev/null 2>&1
					fi
					if [[ "$timeMil" ]] && [[ "$timeMil" != 0 ]]
					then
						echo "OK $ip ResponseTime $timeMil" 
						echo "$timeMil $ip" >> "$resultFile"
					else
						echo "FAILED $ip"
					fi
				else
					echo "FAILED $ip"
				fi
			else
				echo "FAILED $ip"
			fi
	done
}
# End of Function fncCheckSubnet
export -f fncCheckSubnet

echo "" > "$resultFile"

for asn in "${cloudFlareASNList[@]}"
do
	urlResult=$(curl -I -L -s https://asnlookup.com/asn/"$asn" | grep "^HTTP" | grep 200 | awk '{ print $2 }')
	if [[ "$urlResult" == "200" ]]
	then
		cloudFlareIpList=$(curl -s https://asnlookup.com/asn/"$asn"/ | grep "^<li><a href=\"/cidr/.*0/" | awk -F "cidr/" '{print $2}' | awk -F "\">" '{print $1}' | grep -E -v     "^8\.|^1\.")
	else
		echo "could not get url curl -s https://asnlookup.com/asn/$asn/"
		echo "will use local file"
		cloudFlareIpList=$(cat "$scriptDir"/cf.local.iplist)
	fi
  ipListLength=$(echo "$cloudFlareIpList" | wc -l)
  passedIpsCount=0
	for subNet in ${cloudFlareIpList}
	do
    fncShowProgress $passedIpsCount $ipListLength
		firstOctet=$(echo "$subNet" | awk -F "." '{ print $1 }')
		if [[ "${cloudFlareOkList[*]}" =~ $firstOctet ]]
		then
			killall v2ray > /dev/null 2>&1
			ipList=$(nmap -sL -n "$subNet" | awk '/Nmap scan report/{print $NF}')
      tput cuu1; tput ed # rewrites Parallel's bar
      parallel --ll --bar -j "$threads" fncCheckSubnet ::: "$ipList" ::: "$progressBar" ::: "$resultFile" ::: "$scriptDir" ::: "$configId" ::: "$configHost" ::: "$configPort" ::: "$configPath" ::: "$configServerName" ::: "$osVersion"
			killall v2ray > /dev/null 2>&1
		fi
    let passedIpsCount++
	done
done

sort -n -k1 -t, "$resultFile" -o "$resultFile"
