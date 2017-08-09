# Prerequisites
* Operating system: Microsoft Windows 8.1, 10
* Downolad and install [Java](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* Download and install [maven](http://maven.apache.org/install.html)
* Download and install [Google Chrome](https://www.google.com/chrome/browser/desktop/index.html)
* Download and install [Mozila Firefox](https://www.mozilla.org/ru/firefox/new/)

# Running tests
*Make sure that commands are ran from current user, not administrator*

Get sources
```
git clone https://github.com/vpolyakov88/ngb-qa.git
cd ngb-qa
```

Run tests on Saucelabs:

Chrome 57.0 tests on http://ngb.opensource.epam.com/catgenome on Windows 10, screen resolution 1280x1024
```
mvn clean test -P chrome_opensource -e
```
Firefox 52.0 tests on http://ngb.opensource.epam.com/catgenome on Windows 10, screen resolution 1280x1024
```
mvn clean test -P firefox_opensource -e
```
IE 11.103 tests on http://ngb.opensource.epam.com/catgenome on Windows 10, screen resolution 1280x1024
```
mvn clean test -P ie_opensource -e
```
EDGE 14.14393 tests on http://ngb.opensource.epam.com/catgenome availible only for Wndows 10, screen resolution 1280x1024
```
mvn clean test -P edge_opensource -e
```
Safari 10.0 tests on http://ngb.opensource.epam.com/catgenome on macOS 10.12, screen resolution 1280x1024
```
mvn clean test -P safari_opensource -e
```

Additional parameters:

-Ddomain=${NGB_address}

-Ddriver=[chrome|firefox|ie|edge|safari]

-Drun.type=[local|remote]

-Dos=${opearating system with version}

-Dbrowser.version=${browser_version_number}

-Dscreen.resolution=${INTxINT}

Example:

```
mvn clean test -P chrome_opensource -Dos="Widows 8.1" -Dbrowser.version=50.0
```
Run tests on Windows 8.1, Chrome version 50.0

Note:

use https://wiki.saucelabs.com/display/DOCS/Platform+Configurator#/ for validating version of OS, browser etc.
