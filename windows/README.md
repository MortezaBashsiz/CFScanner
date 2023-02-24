# Cloudflare IP scan on Windows
![screen](https://user-images.githubusercontent.com/126115050/220948247-711c972c-0b86-4131-82c1-437e461daa6e.png)

## About
This tool help you to scan all IP ranges of Cloudflare to find clean and working ones.

Inspired by [Morteza CFScanner](https://github.com/MortezaBashsiz/CFScanner)

## Features
* Scan all Cloudflare IP ranges and find fastest IP addresses.
* Save scan results and ability to scan in previous results.
* Scan a single IP address.
* Fast and concurrent scan.
* Selectable IP ranges and ability to skip current IP range.
* User friendly and easy to use.

## Requirements
To run this app you need to have `.NET Desktop Runtime 6` installed on your Windows which is normally installed on newer versions of Windows.
However if you don't already have it installed on your pc then you can download it from here:

Look for **.NET Desktop Runtime 6** in download page:
```
https://dotnet.microsoft.com/en-us/download/dotnet/6.0
```

Or use this direct links:

For 64 bit Windows:
```
https://dotnet.microsoft.com/en-us/download/dotnet/thank-you/runtime-desktop-6.0.14-windows-x64-installer
```
For 32 bit Windows:
```
https://dotnet.microsoft.com/en-us/download/dotnet/thank-you/runtime-desktop-6.0.14-windows-x86-installer
```

## How to use
Just download latest release from `release` section and extract zip file to disk, then run `WinCFScan.exe`.

## Other notes
* In `Scan Results` tab you can right click on an IP and **Copy** IP address and also you can click on `Test this IP address` menu to test just one IP address.

![right](https://user-images.githubusercontent.com/126115050/220962263-429eda22-2987-441c-81e2-9c448bbb026e.png)

* Here also you can load saved previous results and scan only on those IPs by clicking on `Scan in results` button.
* You can set number of concurrent scan process to speed up your scan but be careful about using large values. Set it base on your CPU threads and also your connection speed. High number of concurrent processes might lead to scan timeout and loose some of working IPs.

## Build and compile
Only advanced users:

If you want to build and compile it yourself then download source code to your pc and open `WinCFScan.sln` file in `Visual Studio 2022`.
Then you can build it from Build menu. After that you **must** copy content of `assets` folder into executable folder which usually is something like `bin/Debug/net6.0-windows`.

# Disclaimer
This app is provided as is, and we make no warranties or guarantees about its performance or suitability for your specific needs. Use at your own risk.


Have fun and give us feedback.
