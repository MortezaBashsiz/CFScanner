#!/bin/bash  -
#===============================================================================
#
#          FILE: cfScanner.sh
#
#         USAGE: ./cfScanner.sh [Argumets]
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
#      REVISION: nomadzzz, armgham, beh-rouz, amini8  
#===============================================================================

# Function fncLongIntToStr
# converts IP in long integer format to a string 
fncLongIntToStr() {
    local IFS=. num quad ip e
    num=$1
    for e in 3 2 1
    do
        (( quad = 256 ** e))
        (( ip[3-e] = num / quad ))
        (( num = num % quad ))
    done
    ip[3]=$num
    echo "${ip[*]}"
}
# End of Function fncLongIntToStr

# Function fncIpToLongInt
# converts IP to long integer 
fncIpToLongInt() {
    local IFS=. ip num e
		# shellcheck disable=SC2206
    ip=($1)
    for e in 3 2 1
    do
        (( num += ip[3-e] * 256 ** e ))
    done
    (( num += ip[3] ))
    echo $num
}
# End of Function fncIpToLongInt


# Function fncSubnetToIP
# converts subnet to IP list
fncSubnetToIP() {
	# shellcheck disable=SC2206
  local network=(${1//\// })
	# shellcheck disable=SC2206
  local iparr=(${network[0]//./ })
  local mask=32
  [[ $((${#network[@]})) -gt 1 ]] && mask=${network[1]}

  local maskarr
	# shellcheck disable=SC2206
  if [[ ${mask} = '\.' ]]; then  # already mask format like 255.255.255.0
    maskarr=(${mask//./ })
  else                           # assume CIDR like /24, convert to mask
    if [[ $((mask)) -lt 8 ]]; then
      maskarr=($((256-2**(8-mask))) 0 0 0)
    elif  [[ $((mask)) -lt 16 ]]; then
      maskarr=(255 $((256-2**(16-mask))) 0 0)
    elif  [[ $((mask)) -lt 24 ]]; then
      maskarr=(255 255 $((256-2**(24-mask))) 0)
    elif [[ $((mask)) -lt 32 ]]; then
      maskarr=(255 255 255 $((256-2**(32-mask))))
    elif [[ ${mask} == 32 ]]; then
      maskarr=(255 255 255 255)
    fi
  fi

  # correct wrong subnet masks (e.g. 240.192.255.0 to 255.255.255.0)
  [[ ${maskarr[2]} == 255 ]] && maskarr[1]=255
  [[ ${maskarr[1]} == 255 ]] && maskarr[0]=255

  # generate list of ip addresses
  local bytes=(0 0 0 0)
  for i in $(seq 0 $((255-maskarr[0]))); do
    bytes[0]="$(( i+(iparr[0] & maskarr[0]) ))"
    for j in $(seq 0 $((255-maskarr[1]))); do
      bytes[1]="$(( j+(iparr[1] & maskarr[1]) ))"
      for k in $(seq 0 $((255-maskarr[2]))); do
        bytes[2]="$(( k+(iparr[2] & maskarr[2]) ))"
        for l in $(seq 1 $((255-maskarr[3]))); do
          bytes[3]="$(( l+(iparr[3] & maskarr[3]) ))"
          printf "%d.%d.%d.%d\n" "${bytes[@]}"
        done
      done
    done
  done
}
# End of Function fncSubnetToIP

# Function fncShowProgress
# Progress bar maker function (based on https://www.baeldung.com/linux/command-line-progress-bar)
function fncShowProgress {
	barCharDone="="
	barCharTodo=" "
	barSplitter='>'
	barPercentageScale=2
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

# Function fncCheckIPList
# Check Subnet
function fncCheckIPList {
	local ipList scriptDir resultFile timeoutCommand domainFronting
	ipList="${1}"
	resultFile="${3}"
	scriptDir="${4}"
	configId="${5}"
	configHost="${6}"
	configPort="${7}"
	configPath="${8}"
	configServerName="${9}"
	fileSize="${10}"
	osVersion="${11}"
	v2rayCommand="${12}"
	tryCount="${13}"
	downloadOrUpload="${14}"
	uploadFile="$scriptDir/../files/upload_file"
	echo "$uploadFile" >> /tmp/adas
	binDir="$scriptDir/../bin"
	configDir="$scriptDir/../config"
	configPath=$(echo "$configPath" | sed 's/\//\\\//g')
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
				domainFronting=$($timeoutCommand 1 curl -k -s -w "%{http_code}\n" --tlsv1.2 -H "Host: speed.cloudflare.com" --resolve "speed.cloudflare.com:443:$ip" "https://speed.cloudflare.com/__down?bytes=1000" -o /dev/null)
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
					avgTime=0
					avgStr=""
					timeMil=0
					nohup "$binDir"/"$v2rayCommand" -c "$ipConfigFile" > /dev/null &
					sleep 2
					for i in $(seq 1 "$tryCount");
					do
						if [[ "$downloadOrUpload" == "DOWN" ]]
						then
							timeMil=$($timeoutCommand 2 curl -x "socks5://127.0.0.1:3$port" -s -w "TIME: %{time_total}\n" "https://speed.cloudflare.com/__down?bytes=$fileSize" --output /dev/null | grep "TIME" | tail -n 1 | awk '{print $2}' | xargs -I {} echo "{} * 1000 /1" | bc )
						elif [[ "$downloadOrUpload" == "UP" ]]
						then
							timeMil=$($timeoutCommand 2 curl -x "socks5://127.0.0.1:3$port" -s -w "TIME: %{time_total}\n" -X POST --data "@$uploadFile" https://speed.cloudflare.com/__up --output /dev/null | grep "TIME" | tail -n 1 | awk '{print $2}' | xargs -I {} echo "{} * 1000 /1" | bc )
						fi
						avgTime=$(( avgTime+timeMil ))
						avgStr="$avgStr $timeMil"
					done
					realTime=$(( avgTime/tryCount ))
					# shellcheck disable=SC2009
					pid=$(ps aux | grep config.json."$ip" | grep -v grep | awk '{ print $2 }')
					if [[ "$pid" ]]
					then
						kill -9 "$pid" > /dev/null 2>&1
					fi
					if [[ "$realTime" ]] && [[ "$realTime" != 0 ]]
					then
						echo -e "${GREEN}OK${NC} $ip ${BLUE}AverageTime $realTime  StringTime$avgStr${NC}" 
						echo "$realTime StringTime $avgStr  IP $ip" >> "$resultFile"
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
# End of Function fncCheckIPList
export -f fncCheckIPList

# Function fncCheckDpnd
# Check for dipendencies
function fncCheckDpnd {
	osVersion="Linux"
	if [[ "$(uname)" == "Linux" ]]; then
			osVersion="Linux"
	    command -v jq >/dev/null 2>&1 || { echo >&2 "I require 'jq' but it's not installed. Please install it and try again."; exit 1; }
	    command -v parallel >/dev/null 2>&1 || { echo >&2 "I require 'parallel' but it's not installed. Please install it and try again."; exit 1; }
	    command -v bc >/dev/null 2>&1 || { echo >&2 "I require 'bc' but it's not installed. Please install it and try again."; exit 1; }
			command -v timeout >/dev/null 2>&1 || { echo >&2 "I require 'timeout' but it's not installed. Please install it and try again."; exit 1; }
	
	elif [[ "$(uname)" == "Darwin" ]];then
			osVersion="Mac"
	    command -v jq >/dev/null 2>&1 || { echo >&2 "I require 'jq' but it's not installed. Please install it and try again."; exit 1; }
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
		configId=$(jq --raw-output .id "$config")	
		configHost=$(jq --raw-output .host "$config")	
		configPort=$(jq --raw-output .port "$config")	
		configPath=$(jq --raw-output .path "$config")	
		configServerName=$(jq --raw-output .serverName "$config")	
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

# Function fncMainCFFindSubnet
# main Function for Subnet
function fncMainCFFindSubnet {
	local threads progressBar resultFile scriptDir configId configHost configPort configPath configServerName fileSize osVersion parallelVersion subnetsFile cloudFlareASNList breakedSubnets network netmask downloadOrUpload
	threads="${1}"
	progressBar="${2}"
	resultFile="${3}"
	scriptDir="${4}"
	configId="${5}"
	configHost="${6}"
	configPort="${7}"
	configPath="${8}"
	configServerName="${9}"
	fileSize="${10}"
	osVersion="${11}"
	subnetsFile="${12}"
	tryCount="${13}"
	downloadOrUpload="${14}"

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
	
	cloudFlareASNList=( AS13335 AS209242 )
	
	parallelVersion=$(parallel --version | head -n1 | grep -Ewo '[0-9]{8}')

	echo "" > "$scriptDir/subnets.list"
	if [[ "$subnetsFile" == "NULL" ]]	
	then
		for asn in "${cloudFlareASNList[@]}"
		do
			urlResult=$(curl -I -L -s https://asnlookup.com/asn/"$asn" | grep "^HTTP" | grep 200 | awk '{ print $2 }')
			if [[ "$urlResult" == "200" ]]
			then
				cfSubnetList=$(curl -s https://asnlookup.com/asn/"$asn"/ | grep "^<li><a href=\"/cidr/.*0/" | awk -F "cidr/" '{print $2}' | awk -F "\">" '{print $1}' | grep -E -v     "^8\.|^1\.")
				echo "$cfSubnetList" >> "$scriptDir/subnets.list"
			else
				echo "could not get url curl -s https://asnlookup.com/asn/$asn/"
				echo "will use local file"
				cat "$scriptDir/cf.local.iplist" > "$scriptDir/subnets.list"
			fi
		done
		cfSubnetList=$(cat "$scriptDir/subnets.list")
	else
		cfSubnetList=$(cat "$subnetsFile")
	fi
	
	ipListLength="0"
	for subNet in ${cfSubnetList}
	do
		breakedSubnets=
		maxSubnet=24
		network=${subNet%/*}
		netmask=${subNet#*/}
		if [[ ${netmask} -ge ${maxSubnet} ]]
		then
		  breakedSubnets="${breakedSubnets} ${network}/${netmask}"
		else
		  for i in $(seq 0 $(( $(( 2 ** (maxSubnet - netmask) )) - 1 )) )
		  do
		    breakedSubnets="${breakedSubnets} $( fncLongIntToStr $(( $( fncIpToLongInt "${network}" ) + $(( 2 ** ( 32 - maxSubnet ) * i )) )) )/${maxSubnet}"
		  done
		fi
		breakedSubnets=$(echo "${breakedSubnets}"|tr ' ' '\n')
		for breakedSubnet in ${breakedSubnets}
		do
			ipListLength=$(( ipListLength+1 ))
		done
	done

	passedIpsCount=0
	for subNet in ${cfSubnetList}
	do
		breakedSubnets=
		maxSubnet=24
		network=${subNet%/*}
		netmask=${subNet#*/}
		if [[ ${netmask} -ge ${maxSubnet} ]]
		then
		  breakedSubnets="${breakedSubnets} ${network}/${netmask}"
		else
		  for i in $(seq 0 $(( $(( 2 ** (maxSubnet - netmask) )) - 1 )) )
		  do
		    breakedSubnets="${breakedSubnets} $( fncLongIntToStr $(( $( fncIpToLongInt "${network}" ) + $(( 2 ** ( 32 - maxSubnet ) * i )) )) )/${maxSubnet}"
		  done
		fi
		breakedSubnets=$(echo "${breakedSubnets}"|tr ' ' '\n')
		for breakedSubnet in ${breakedSubnets}
		do
			fncShowProgress "$passedIpsCount" "$ipListLength"
			killall v2ray > /dev/null 2>&1
			ipList=$(fncSubnetToIP "$breakedSubnet")
	  	tput cuu1; tput ed # rewrites Parallel's bar
	  	if [[ $parallelVersion -gt "20220515" ]];
	  	then
	  	  parallel --ll --bar -j "$threads" fncCheckIPList ::: "$ipList" ::: "$progressBar" ::: "$resultFile" ::: "$scriptDir" ::: "$configId" ::: "$configHost" ::: "$configPort" ::: "$configPath" ::: "$configServerName" ::: "$fileSize" ::: "$osVersion" ::: "$v2rayCommand" ::: "$tryCount" ::: "$downloadOrUpload"
	  	else
	  	  echo -e "${RED}$progressBar${NC}"
	  	  parallel -j "$threads" fncCheckIPList ::: "$ipList" ::: "$progressBar" ::: "$resultFile" ::: "$scriptDir" ::: "$configId" ::: "$configHost" ::: "$configPort" ::: "$configPath" ::: "$configServerName" ::: "$fileSize" ::: "$osVersion" ::: "$v2rayCommand" ::: "$tryCount" ::: "$downloadOrUpload"
	  	fi
			killall v2ray > /dev/null 2>&1
			passedIpsCount=$(( passedIpsCount+1 ))
		done
	done
	sort -n -k1 -t, "$resultFile" -o "$resultFile"
}
# End of Function fncMainCFFindSubnet

# Function fncMainCFFindIP
# main Function for IP
function fncMainCFFindIP {
	local threads progressBar resultFile scriptDir configId configHost configPort configPath configServerName fileSize osVersion parallelVersion IPFile downloadOrUpload
	threads="${1}"
	progressBar="${2}"
	resultFile="${3}"
	scriptDir="${4}"
	configId="${5}"
	configHost="${6}"
	configPort="${7}"
	configPath="${8}"
	configServerName="${9}"
	fileSize="${10}"
	osVersion="${11}"
	IPFile="${12}"
	tryCount="${13}"
	downloadOrUpload="${14}"

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

	parallelVersion=$(parallel --version | head -n1 | grep -Ewo '[0-9]{8}')

	cfIPList=$(cat "$IPFile")
	killall v2ray > /dev/null 2>&1
	tput cuu1; tput ed # rewrites Parallel's bar
	if [[ $parallelVersion -gt "20220515" ]];
	then
	  parallel --ll --bar -j "$threads" fncCheckIPList ::: "$cfIPList" ::: "$progressBar" ::: "$resultFile" ::: "$scriptDir" ::: "$configId" ::: "$configHost" ::: "$configPort" ::: "$configPath" ::: "$configServerName" ::: "$fileSize" ::: "$osVersion" ::: "$v2rayCommand" ::: "$tryCount" ::: "$downloadOrUpload"
	else
	  echo -e "${RED}$progressBar${NC}"
	  parallel -j "$threads" fncCheckIPList ::: "$cfIPList" ::: "$progressBar" ::: "$resultFile" ::: "$scriptDir" ::: "$configId" ::: "$configHost" ::: "$configPort" ::: "$configPath" ::: "$configServerName" ::: "$fileSize" ::: "$osVersion" ::: "$v2rayCommand" ::: "$tryCount" ::: "$downloadOrUpload"
	fi
	killall v2ray > /dev/null 2>&1
	sort -n -k1 -t, "$resultFile" -o "$resultFile"
}
# End of Function fncMainCFFindIP

clientConfigFile="https://raw.githubusercontent.com/MortezaBashsiz/CFScanner/main/bash/ClientConfig.json"
subnetIPFile="NULL"




usage()
{
  echo -e "Usage: cfScanner [ -m|--mode  SUBNET/IP ] 
                 [ -t|--test-type  DOWN/UP ]
		 [ -thr|--thread <int> ]
		 [ -try|--tryCount <int> ]
		 [ -c|--config <configfile> ]
		 [ -s|--speed <int> ] 
		 [ -f|--file <custome-ip-file> (if you chose IP mode)]\n"
  exit 2
}

parsedArguments=$(getopt -a -n  cfScanner -o m:t:thr:try:c:s:f: --long mode:,test-type:,thread:,tryCount:,config:,speed:,custom-subnet-file: -- "$@")
validArguments=$?
if [ "$VALID_ARGUMENTS" != "0" ]; then
  echo "error validate"
  exist 2
fi


eval set -- "$PARSED_ARGUMENTS"
while :
do
  case "$1" in
    -m | --mode)    subnetOrIP="$2" ; shift 2  ;;
	-t | --test-type)   downloadOrUpload="$2" ; shift 2  ;;
	-thr | --thread)    threads="$2"  ; shift 2  ;;
	-try | --tryCount)    tryCount="$2"  ; shift 2  ;;
	-c | --config) config="$2"  ; shift 2  ;;
	-s | --speed)    speed="$2" ; shift 2  ;;
	-f | --file)    subnetIPFile="$2"  ; shift 2  ;;
	-h | --help) usage;;

    --) shift; break ;;
    *) echo "Unexpected option: $1 - this should not happen."
       usage ;;
  esac
done







if [[ "$subnetIPFile" != "NULL" ]]
then

	if ! [[ -f "$subnetIPFile" ]]
	then
		echo "file does not exists: $subnetIPFile"
		exit 1
	fi
fi

now=$(date +"%Y%m%d-%H%M%S")
scriptDir=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
resultDir="$scriptDir/../result"
resultFile="$resultDir/$now-result.cf"
configDir="$scriptDir/../config"
filesDir="$scriptDir/../files"

uploadFile="$filesDir/upload_file"

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

echo "updating config.real"
if [[ "$config" == "config.real"  ]]
then
	configRealUrlResult=$(curl -I -L -s "$clientConfigFile" | grep "^HTTP" | grep 200 | awk '{ print $2 }')
	if [[ "$configRealUrlResult" == "200" ]]
	then
		curl -s "$clientConfigFile" -o "$scriptDir"/config.real
		echo "config.real updated with $clientConfigFile"
		echo ""
		config="$scriptDir/config.real"
		cat "$config"
	else
		echo ""
		echo "config file is not available $clientConfigFile"
		echo "use your own"
		echo ""	
		exit 1
	fi
else
	echo ""
	echo "using your own config $config"
	cat "$config"
	echo ""
fi

fileSize="$(( 2*speed ))000"

if [[ "$downloadOrUpload" == "DOWN" ]]
then
	echo "You are testing download"
elif [[ "$downloadOrUpload" == "UP" ]]
then
	echo "You are testing upload"
	echo "making upload file by size $fileSize bytes in $uploadFile"
	dd if=/dev/zero of="$uploadFile" bs=1B count="$fileSize" > /dev/null 2>&1
else
	echo "$downloadOrUpload is not correct choose one DOWN or UP"
	exit 1
fi

fncValidateConfig "$config"

if [[ "$subnetOrIP" == "SUBNET" ]]
then
	fncMainCFFindSubnet	"$threads" "$progressBar" "$resultFile" "$scriptDir" "$configId" "$configHost" "$configPort" "$configPath" "$configServerName" "$fileSize" "$osVersion" "$subnetIPFile" "$tryCount" "$downloadOrUpload"
elif [[ "$subnetOrIP" == "IP" ]]
then
	fncMainCFFindIP	"$threads" "$progressBar" "$resultFile" "$scriptDir" "$configId" "$configHost" "$configPort" "$configPath" "$configServerName" "$fileSize" "$osVersion" "$subnetIPFile" "$tryCount" "$downloadOrUpload"
else
	echo "$subnetOrIP is not correct choose one SUBNET or IP"
	exit 1
fi

