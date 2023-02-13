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
#      REVISION:  thehxdev, Ali-Frh, nomadzzz, armgham 
#===============================================================================

set -o nounset                                  # Treat unset variables as an error

# Check if 'parallel', 'timeout', 'nmap' and 'bc' packages are installed
# If they are not,exit the script
osVersion="Linux"
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

parallelVersion=$(parallel --version | head -n1 | grep -Ewo '[0-9]{8}')


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

export GREEN='\033[0;32m'
export BLUE='\033[0;34m'
export RED='\033[0;31m'
export ORANGE='\033[0;33m'
export YELLOW='\033[1;33m'
export NC='\033[0m'

frontDomain="fronting.sudoer.net"
scanDomain="scan.sudoer.net"
downloadFile="data.100k"

threads="$1"
config="$2"
speed="$3"

speedList=(25 50 100 150 200 250 500)
declare -a downloadFileArr
downloadFileArr["25"]="data.50k"
downloadFileArr["50"]="data.100k"
downloadFileArr["100"]="data.200k"
downloadFileArr["150"]="data.300k"
downloadFileArr["200"]="data.400k"
downloadFileArr["250"]="data.500k"
downloadFileArr["500"]="data.1000k"

if [[ "${speedList[*]}" =~ $speed ]]
then
	downloadFile="${downloadFileArr[${speed}]}"
else
	echo "Speed $speed is not valid, choose be one of (25 50 100 150 200 250 500)"
	exit 0
fi

# Check if config file exists
if [[ -f "$config" ]]
then
	echo "reading config ..."
	configId=$(grep "^id" "$config" | awk -F ":" '{ print $2 }' | sed "s/ //g")	
	configHost=$(grep "^Host" "$config" | awk -F ":" '{ print $2 }' | sed "s/ //g")	
	configPort=$(grep "^Port" "$config" | awk -F ":" '{ print $2 }' | sed "s/ //g")	
	configPath=$(grep "^path" "$config" | awk -F ":" '{ print $2 }' | sed "s/ //g" | sed 's/\//\\\//g')	
	configServerName=$(grep "^serverName" "$config" | awk -F ":" '{ print $2 }' | sed "s/ //g")	
	if ! [[ "$configId" ]] || ! [[ $configHost ]] || ! [[ $configPort ]] || ! [[ $configPath ]] || ! [[ $configServerName ]]
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
	ipList="${1}"
	resultFile="${3}"
	scriptDir="${4}"
	configId="${5}"
	configHost="${6}"
	configPort="${7}"
	configPath="${8}"
	configServerName="${9}"
	frontDomain="${10}"
	scanDomain="${11}"
	downloadFile="${12}"
	osVersion="${13}"
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
				domainFronting=$($timeoutCommand 2 curl -s -w "%{http_code}\n" --tlsv1.2 -servername "$frontDomain" -H "Host: $frontDomain" --resolve "$frontDomain":443:"$ip" https://"$frontDomain" -o /dev/null | grep '200')
				if [[ "$domainFronting" == "200" ]]
				then
					ipConfigFile="$configDir/config.json.$ip"
					cp "$scriptDir"/config.json.temp "$ipConfigFile"
					ipO1=$(echo "$ip" | awk -F '.' '{print $1}')
					ipO2=$(echo "$ip" | awk -F '.' '{print $2}')
					ipO3=$(echo "$ip" | awk -F '.' '{print $3}')
					ipO4=$(echo "$ip" | awk -F '.' '{print $4}')
					port=$((ipO1 + ipO2 + ipO3 + ipO4))
					if [[ "$osVersion" == "Mac" ]]
					then
						sed -i "" "s/IP.IP.IP.IP/$ip/g" "$ipConfigFile"
						sed -i "" "s/PORTPORT/3$port/g" "$ipConfigFile"
						sed -i "" "s/IDID/$configId/g" "$ipConfigFile"
						sed -i "" "s/HOSTHOST/$configHost/g" "$ipConfigFile"
						sed -i "" "s/CFPORTCFPORT/$configPort/g" "$ipConfigFile"
						sed -i "" "s/ENDPOINTENDPOINT/$configPath/g" "$ipConfigFile"
						sed -i "" "s/RANDOMHOST/$configServerName/g" "$ipConfigFile"
					elif [[ "$osVersion" == "Linux" ]]
					then
						sed -i "s/IP.IP.IP.IP/$ip/g" "$ipConfigFile"
						sed -i "s/PORTPORT/3$port/g" "$ipConfigFile"
						sed -i "s/IDID/$configId/g" "$ipConfigFile"
						sed -i "s/HOSTHOST/$configHost/g" "$ipConfigFile"
						sed -i "s/CFPORTCFPORT/$configPort/g" "$ipConfigFile"
						sed -i "s/ENDPOINTENDPOINT/$configPath/g" "$ipConfigFile"
						sed -i "s/RANDOMHOST/$configServerName/g" "$ipConfigFile"
					fi
					# shellcheck disable=SC2009
					pid=$(ps aux | grep config.json."$ip" | grep -v grep | awk '{ print $2 }')
					if [[ "$pid" ]]
					then
						kill -9 "$pid"
					fi
					nohup "$scriptDir"/"$v2rayCommand" -c "$ipConfigFile" > /dev/null &
					sleep 2
					timeMil=$($timeoutCommand 2 curl -x "socks5://127.0.0.1:3$port" -s -w "TIME: %{time_total}\n" https://"$scanDomain"/"$downloadFile" --output /dev/null | grep "TIME" | tail -n 1 | awk '{print $2}' | xargs -I {} echo "{} * 1000 /1" | bc )
					# shellcheck disable=SC2009
					pid=$(ps aux | grep config.json."$ip" | grep -v grep | awk '{ print $2 }')
					if [[ "$pid" ]]
					then
						kill -9 "$pid" > /dev/null 2>&1
					fi
					if [[ "$timeMil" ]] && [[ "$timeMil" != 0 ]]
					then
						echo -e "${GREEN}OK${NC} $ip ${BLUE}ResponseTime $timeMil${NC}" 
						echo "$timeMil $ip" >> "$resultFile"
					else
						echo -e "${YELLOW}FAILED${NC} $ip"
					fi
				else
					echo -e "${YELLOW}FAILED${NC} $ip"
				fi
			else
				echo -e "${YELLOW}FAILED${NC} $ip"
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
    fncShowProgress "$passedIpsCount" "$ipListLength"
		firstOctet=$(echo "$subNet" | awk -F "." '{ print $1 }')
		if [[ "${cloudFlareOkList[*]}" =~ $firstOctet ]]
		then
			killall v2ray > /dev/null 2>&1
			ipList=$(nmap -sL -n "$subNet" | awk '/Nmap scan report/{print $NF}')
      tput cuu1; tput ed # rewrites Parallel's bar
      if [[ $parallelVersion -gt "20220515" ]];
      then
        parallel --ll --bar -j "$threads" fncCheckSubnet ::: "$ipList" ::: "$progressBar" ::: "$resultFile" ::: "$scriptDir" ::: "$configId" ::: "$configHost" ::: "$configPort" ::: "$configPath" ::: "$configServerName" ::: "$frontDomain" ::: "$scanDomain" ::: "$downloadFile" ::: "$osVersion"
      else
        echo -e "${RED}$progressBar${NC}"
        parallel -j "$threads" fncCheckSubnet ::: "$ipList" ::: "$progressBar" ::: "$resultFile" ::: "$scriptDir" ::: "$configId" ::: "$configHost" ::: "$configPort" ::: "$configPath" ::: "$configServerName" ::: "$frontDomain" ::: "$scanDomain" ::: "$downloadFile" ::: "$osVersion"
      fi
			killall v2ray > /dev/null 2>&1
		fi
    passedIpsCount=$(( passedIpsCount+1 ))
	done
done

sort -n -k1 -t, "$resultFile" -o "$resultFile"
