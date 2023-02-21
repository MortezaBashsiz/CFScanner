#!/bin/bash  -
#===============================================================================
#
#          FILE: cfFindIPFromList.sh
#
#         USAGE: ./cfFindIPFromList.sh [ThreadCount]
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
#      REVISION:
#===============================================================================

# Function fncCheckIP
# Check Subnet
function fncCheckIP {
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
	v2rayCommand="${14}"
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
	for ip in ${ipList}
		do
			if $timeoutCommand 1 bash -c "</dev/tcp/$ip/443" > /dev/null 2>&1;
			then
				domainFronting=$($timeoutCommand 1 curl -k -s -w "%{http_code}\n" --tlsv1.2 -H "Host: $frontDomain" --resolve "$frontDomain":443:"$ip" https://"$frontDomain" -o /dev/null | grep '200')
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
						kill -9 "$pid" > /dev/null 2>&1
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
# End of Function fncCheckIP
export -f fncCheckIP

# Function fncCheckDpnd
# Check for dipendencies
function fncCheckDpnd {
	osVersion="Linux"
	if [[ "$(uname)" == "Linux" ]]; then
			osVersion="Linux"
	    command -v parallel >/dev/null 2>&1 || { echo >&2 "I require 'parallel' but it's not installed. Please install it and try again."; exit 1; }
	    command -v bc >/dev/null 2>&1 || { echo >&2 "I require 'bc' but it's not installed. Please install it and try again."; exit 1; }
			command -v timeout >/dev/null 2>&1 || { echo >&2 "I require 'timeout' but it's not installed. Please install it and try again."; exit 1; }
	
	elif [[ "$(uname)" == "Darwin" ]];then
			osVersion="Mac"
	    command -v parallel >/dev/null 2>&1 || { echo >&2 "I require 'parallel' but it's not installed. Please install it and try again."; exit 1; }
	    command -v bc >/dev/null 2>&1 || { echo >&2 "I require 'bc' but it's not installed. Please install it and try again."; exit 1; }
	    command -v gtimeout >/dev/null 2>&1 || { echo >&2 "I require 'gtimeout' but it's not installed. Please install it and try again."; exit 1; }
	fi
	echo "$osVersion"
}
# End of Function fncCheckDpnd

# Function fncValidateConfig
# Install packages on destination host
function fncValidateConfig {
	local config
	config="$1"
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
}
# End of Function fncValidateConfig

# Function fncCreateDir
# creates needed directory
function fncCreateDir {
	local dirPath
	dirPath="${1}"
	if [ ! -d "$dirPath" ]; then
		mkdir -p "$dirPath"
	fi
}
# End of Function fncCreateDir

# Function fncCheckSpeed
# validates speed return proper downloadFile
function fncCheckSpeed {
	local downloadFile speedList speed
	speed="${1}"
	downloadFile="NULL"
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
	fi
	echo "$downloadFile"
}
# End of Function fncCheckSpeed

# Function fncMainCFFind
# main Function
function fncMainCFFind {
	local threads progressBar resultFile scriptDir configId configHost configPort configPath configServerName frontDomain scanDomain speed  downloadFile osVersion parallelVersion IPFile
	threads="${1}"
	progressBar="${2}"
	resultFile="${3}"
	scriptDir="${4}"
	configId="${5}"
	configHost="${6}"
	configPort="${7}"
	configPath="${8}"
	configServerName="${9}"
	frontDomain="${10}"
	scanDomain="${11}"
	speed="${12}"
	osVersion="${13}"
	IPFile="${14}"

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
	
	echo "updating config.real"
	configRealUrlResult=$(curl -I -L -s http://bot.sudoer.net/config.real | grep "^HTTP" | grep 200 | awk '{ print $2 }')
	if [[ "$configRealUrlResult" == "200" ]]
	then
		curl -s http://bot.sudoer.net/config.real -o "$scriptDir"/config.real
		echo "config.real updated with http://bot.sudoer.net/config.real"
		echo ""
		fncValidateConfig "$config"
	else
		echo ""
		echo "url http://bot.sudoer.net/config.real is not reachable"
		echo "make sure that you have the updated config.real"
		echo ""
	fi

	parallelVersion=$(parallel --version | head -n1 | grep -Ewo '[0-9]{8}')

	downloadFile="$(fncCheckSpeed "$speed")"
	if [[ "$downloadFile" == "NULL" ]]
	then
		echo "Speed $speed is not valid, choose be one of (25 50 100 150 200 250 500)"
		exit 0
	fi
	cfIPList=$(cat "$IPFile")
	killall v2ray > /dev/null 2>&1
	tput cuu1; tput ed # rewrites Parallel's bar
	if [[ $parallelVersion -gt "20220515" ]];
	then
	  parallel --ll --bar -j "$threads" fncCheckIP ::: "$cfIPList" ::: "$progressBar" ::: "$resultFile" ::: "$scriptDir" ::: "$configId" ::: "$configHost" ::: "$configPort" ::: "$configPath" ::: "$configServerName" ::: "$frontDomain" ::: "$scanDomain" ::: "$downloadFile" ::: "$osVersion" ::: "$v2rayCommand"
	else
	  echo -e "${RED}$progressBar${NC}"
	  parallel -j "$threads" fncCheckIP ::: "$cfIPList" ::: "$progressBar" ::: "$resultFile" ::: "$scriptDir" ::: "$configId" ::: "$configHost" ::: "$configPort" ::: "$configPath" ::: "$configServerName" ::: "$frontDomain" ::: "$scanDomain" ::: "$downloadFile" ::: "$osVersion" ::: "$v2rayCommand"
	fi
	killall v2ray > /dev/null 2>&1
	sort -n -k1 -t, "$resultFile" -o "$resultFile"
}
# End of Function fncMainCFFind

threads="$1"
config="$2"
speed="$3"
IPFile="NULL"

if [[ "$4" ]]
then
	IPFile="$4"
	if ! [[ -f "$IPFile" ]]
	then
		echo "file does not exists: $IPFile"
		exit 1
	fi
fi

frontDomain="fronting.sudoer.net"
scanDomain="scan.sudoer.net"
downloadFile="data.100k"

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

progressBar=""

export GREEN='\033[0;32m'
export BLUE='\033[0;34m'
export RED='\033[0;31m'
export ORANGE='\033[0;33m'
export YELLOW='\033[1;33m'
export NC='\033[0m'

fncCreateDir "${resultDir}"
fncCreateDir "${configDir}"
echo "" > "$resultFile"

osVersion="$(fncCheckDpnd)"
fncMainCFFind	"$threads" "$progressBar" "$resultFile" "$scriptDir" "$configId" "$configHost" "$configPort" "$configPath" "$configServerName" "$frontDomain" "$scanDomain" "$speed" "$osVersion" "$IPFile"
