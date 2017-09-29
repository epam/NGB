# Running NGB from standalone Jar

A standalone Jar distribution is available for NGB, which includes embedded Tomcat server

## Environment requirements

Verify that your system meets or exceeds the following hardware/software requirements

* Server requirementsЖ
    * CPU: 2 cores
    * RAM: 4Gb
    * HDD: 20 Gb of free space
    * **[Oracle JDK 8](https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html)** or **[Open JDK 8](http://openjdk.java.net/install/)**
    * GIT

* Client web-browser requirements:
    * Chrome 56 or later
    * Firefox 51 or later
    * Safari 9 or later
    * EDGE 25 or later
  
## Get Source Code

Download NGB source code from GitHub:
```
$ git clone https://github.com/epam/NGB.git
$ cd NGB
```

## Build JAR file

Build NGB standalone jar, using Gradle build script:
```
$ ./gradlew buildJar
```

You can find **catgenome.jar** archive in the **dist/** folder.

## Run JAR file

Run **catgenome.jar** archive:

*Note:* data files, used by NGB instance, locate in a `current` directory of the console:

```
# Data files will be located in the same folder as a catgenome.jar
$ java -jar catgenome.jar

# Data files will be located in /home/user folder
$ pwd
/home/user
$ java -jar NGB/dist/catgenome.jar
```

NGB will be available at http://localhost:8080/catgenome.

## Configuring NGB instance

By default NGB will run on port 8080 and keep NGB data files and error logs in the runtime folder.
To customize the configuration you can use the following available options:

### Data storage

You can create **catgenome.properties** file to specify the data location. The available properties:

* **files.base.directory.path=/opt/catgenome/contents** - path for storing NGB files
* **database.driver.class=org.h2.Driver** - NGB database driver, the default database is H2
* **database.jdbc.url=jdbc:h2:file:/opt/catgenome/H2/catgenome** - the path to NGB database location
* **database.username=catgenome** - NGB database user
* **database.password=** - NGB database user's password, may be empty
* **database.max.pool.size=25** - NGB database connection pool configuration
* **database.initial.pool.size=5** - NGB database connection pool configuration

To enable browsing of NGS files directly from the server's file system, set the following properties:

* **file.browsing.allowed=true** - enables file browsing from the server file system
* **ngs.data.root.path=/opt/catgenome** - sets /opt/catgenome as a root to allowed files browsing. 

If you do not specify this property, the application sets root to the root of file system.

To configure the default options for tracks visualization on a client side, set the following property:
* **config.path=/opt/catgenome/configs** - sets the path to the directory that contains `Json` configuration files for NGB client.

To specify max number of VcfIndexEntries keeping in memory during vcf loading, set the following property. For files, which produce more entries then the number, the application will spill  to disk extra entries (temp directory).
* **files.vcf.max.entries.in.memory=1000000** - 1000000 entries take about 3Gb in the heap

You should put **catgenome.properties** in the **config** folder in the runtime folder or provide a path to the folder with properties file from the command line:
 
```
$ java -jar catgenome.jar --conf=/folder/with/properties
```
 
### Configure Embedded Tomcat Server

NGB uses Spring Boot so it supports a full stack of Spring Boot Application properties.
You can set these properties in the command line:

```
# Run NGB on 9999 port 
$ java -jar catgenome.jar --server.port=9999
# Disable traffic compression
$ java -jar catgenome.jar --server.compression.enabled=false
```
 
or in the **application.properties** file in the **config** folder in the runtime directory:
* server.port=9999
* server.compression.enabled=false

See the full list of available options in the [Spring Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/common-application-properties.html).
