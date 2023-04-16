import requests
import re
from datetime import datetime
import pytz
from pathlib import Path
import subprocess
import os
import time

path = Path(__file__).resolve().parent
url = 'https://asnlookup.com/asn/'
agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/109.0"
ips = list()
asns = ['AS13335', 'AS209242']
# correctIp = ['23', '31', '45', '66', '80', '89', '103', '104', '108', '141',
#              '147', '154', '159', '168','162', '170', '172', '173', '185', '188', '191',
#              '192', '193', '194', '195', '198', '199', '203', '205', '212', ]
wrongIp = ['1', '8' ]


def substring_after(s, delim):
    return s.partition(delim)[2]


def substring_before(s, delim):
    return s.partition(delim)[0]


def substring_between(s, before, after):
    return substring_before(substring_after(s, before), after)


def getPageContent(url):
    pathss = str(path) + os.sep
    main = pathss + "cfchallenger.py"
    result = subprocess.getoutput(f'python {main} {url}'.format(main=main, url=url))
    return result


deli_before = """<th scope="row">IPv4 CIDRs</th>
<td>
<ul class="grid-list">
"""
deli_after = """</ul>
</td>
</tr>
<tr>
<th scope="row">IPv6 CIDRs</th>"""


def extract_ips(text):
    raw = substring_between(text, deli_before, deli_after)
    temp_list = list()
    regex = r"\">(.+?)<"
    matches = re.finditer(regex, raw, re.MULTILINE)
    for matchNum, match in enumerate(matches, start=1):
        for groupNum in range(0, len(match.groups())):
            groupNum = groupNum + 1
            temp_list.append(match.group(groupNum))
    return temp_list


def filter_ips(ips):
    def filterFun(ip):
        tarter = ip.split(".")[0]
        return False if wrongIp.count(tarter) else True

    return filter(filterFun, ips)


print("<====/ started at  " + str(datetime.now()) + " \====>")

for asn in asns:
    url_asn = url + asn + "/"
    response = str(getPageContent(url_asn))
    successful = True if "IPv4 CIDRs" in response else False
    print("{url} fetched with status {status}".format(url=url_asn, status="success" if successful else "failed"))
    time.sleep(5)
    ips.extend(extract_ips(response))

# get another addresses
additionUrl = "https://www.cloudflare.com/ips-v4"
thirdReq = requests.get(additionUrl)
print("{url} fetched with status {status}".format(url=additionUrl,
                                                  status="success" if thirdReq.status_code == 200 else "failed"))
ips.extend(thirdReq.text.splitlines())

teh_tz = pytz.timezone('Iran')
datetime_Th = datetime.now(teh_tz)

#disable filter
finalList = list(set(filter_ips(ips)))
count = len(finalList)
time = datetime_Th
output = '\n'.join(finalList)

if len(finalList) < 20:
    print("Error , Prevent Update !")
    exit()

path = os.path.dirname(os.path.realpath(__file__)) + os.path.sep
repoPath = path + "CFScanner" + os.path.sep
outputPath = repoPath + "config" + os.path.sep + "cf.local.iplist"
with open(path + "token.secret", "r") as file: githubToken = file.read()
repoUrl = "https://{token}@github.com/MortezaBashsiz/CFScanner".format(token=githubToken)

print("start Git repository update ...")
result = subprocess.getoutput("git clone " + repoUrl + " " + repoPath)
result = subprocess.getoutput(f"git -C {repoPath} fetch origin ip-update:ip-update")
result = subprocess.getoutput(f"git -C {repoPath} pull -f origin ip-update:ip-update")
result = subprocess.getoutput(f"git -C {repoPath} checkout ip-update")

if os.path.exists(outputPath):
    os.remove(outputPath)

f = open(outputPath, "a")
f.write(output)
f.close()

description = "Updated at {time} by {count} items".format(time=time, count=count)
result = subprocess.getoutput(f"git -C {repoPath} add .")
result = subprocess.getoutput(f"git -C {repoPath} commit -m \"{description}\"")
result = subprocess.getoutput(f"git -C {repoPath} push origin ip-update")
print("Git repository updated.")

print("<====\ Ended at  " + str(datetime.now()) + " /====>")
