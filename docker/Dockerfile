FROM debian
WORKDIR /CFSCANNER
RUN apt update
RUN apt install -y git bc curl parallel
RUN git clone https://github.com/MortezaBashsiz/CFScanner.git
RUN mkdir -p CFScanner/result
RUN mkdir -p CFScanner/config
