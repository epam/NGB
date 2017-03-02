# Requirements

**[Oracle JDK 8](https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html)** or **[Open JDK 8](http://openjdk.java.net/install/)**
**[Node.js >= 6.9.5](https://nodejs.org/en/download/package-manager/)** 

# How to build NGB desktop application

Obtain the source code from github:
```
$ git clone https://github.com/epam/NGB.git
$ cd NGB
```

Build NGB standalone JAR file, optimized for desktop usage

```
$ ./gradlew clean buildDesktopJar
$ cd desktop
```

NGB Desktop application uses Electron framework. To be able to develop with it, you need to install **Electron** globally

```
$ npm install
$ npm install electron -g
```

To run NGB application using Electron, type the following:

```
$ electron .
```

# How to package NGB desktop application's distribution

**Attention**: packaging NGB desktop application includes packaging it for all supported platforms. Packaging application's 
Windows version using non-Windows system requires **32bit Wine** to be installed. For additional information, see 
   **[electron-packager](https://github.com/electron-userland/electron-packager#building-windows-apps-from-non-windows-platforms/)** docs.

To pack a NGB desktop application distribution, that can be shipped to other workstations, without Electron or Node.js installed,
use the following command:

```
$ npm run build
```

This will pack NGB desktop application for all supported platforms (64bit Windows 7+, Linux and OS X) in **dist** directory:
```
$ ls ./dist

ngb-win32-x64
ngb-linux-x64
ngb-darwin-x64
```

To run packaged NGB desktop application, use executable file from those directories, e.g.

```
$ ./dist/ngb-linux-x64/ngb
```



