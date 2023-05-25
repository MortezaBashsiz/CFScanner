FROM debian
WORKDIR /CFSCANNER
RUN apt-get update
RUN apt-get install -y git bc curl parallel jq
RUN git clone https://github.com/MortezaBashsiz/CFScanner.git
RUN mkdir -p CFScanner/result
RUN mkdir -p CFScanner/config
