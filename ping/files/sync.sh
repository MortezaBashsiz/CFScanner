#!/bin/bash  -
#===============================================================================
#
#          FILE: sync.sh
#
#         USAGE: ./sync.sh
#
#   DESCRIPTION: 
#
#       OPTIONS: ---
#  REQUIREMENTS: ---
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: Morteza Bashsiz (mb), morteza.bashsiz@gmail.com
#  ORGANIZATION: Linux
#       CREATED: 03/05/2023 08:29:53 AM
#      REVISION:  ---
#===============================================================================

set -o nounset                                  # Treat unset variables as an error

split -l 2048 "$resultFile" "$resultDir/part-result"
ssh -q bot.sudoer.net "rm /var/www/html/part-result*"
scp "$resultDir"/part-result* bot.sudoer.net:/var/www/html/
ssh -q bot.sudoer.net 'find /var/www/html/ -type f -iname "part-result*" -printf "%f\n" > /var/www/html/part-all'

