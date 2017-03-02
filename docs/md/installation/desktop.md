# Running NGB as desktop application

A standalone distribution is available for NGB, which runs it as a standalone desktop application, without the need to run browser

## Environment requirements

Verify that your system meets or exceeds the following hardware/software requirements
* Hardware requirements
    * x64 operating system: Windows 7+, Linux or OS X
    * CPU: 2 cores
    * RAM: 4Gb
    * HDD: 20 Gb free space
    * **[Oracle JDK 8](https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html)** or **[Open JDK 8](http://openjdk.java.net/install/)**
    * GIT
  
## Get Source Code

Obtain NGB source code from GitHub:
```
$ git clone https://github.com/epam/NGB.git
$ cd NGB

```

## Build application

Build NGB desktop application, using Gradle build script
```
$ ./gradlew buildDesktop

```

You can find **ngb-win32-x64**, **ngb-linux-x64** and **ngb-darwin-x64** folders in the **dist/** folder

## Run desktop application

#### Windows
Go to **ngb-win32-x64** directory and run **ngb.exe** file
 
#### Linux
Run the following command from terminal:
```
$ dist/ngb-linux-x64/ngb

```

Or simply go to **ngb-linux-x64** directory and run **ngb** file

#### OS X

Run **ngb** file from **ngb-darwin-x64/ngb.app/MacOS/**