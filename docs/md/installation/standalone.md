# Running NGB from standalone Jar

A standalone Jar distribution is available for NGB, which includes embedded Tomcat server

## Environment requirements

Verify that your system meets or exceeds the following hardware/software requirements

* Server hardware requirements
    * CPU: 2 cores
    * RAM: 4Gb
    * HDD: 20 Gb free space
    * **[Oracle JDK 8](https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html)** or **[Open JDK 8](http://openjdk.java.net/install/)**
    * GIT

* Client web-browser requirements
    * Chrome (>= 56)
    * Firefox (>= 51)
    * Safari (>= 9)
    * EDGE (>= 25)
  
## Get Source Code

Obtain NGB source code from GitHub:
```
$ git clone https://github.com/epam/NGB.git
$ cd NGB
```

## Build JAR file

Build NGB standalone jar, using Gradle build script
```
$ ./gradlew buildJar
```

You can find **catgenome.jar** archive in the **dist/** folder

## Run JAR file

Run **catgenome.jar**

*Note:* data files, used by NGB instance, will be located in a `current` directory (current directory of a console)

```
# Data files will be located in the same folder as a catgenome.jar
$ java -jar catgenome.jar

# Data files will be located in /home/user folder
$ pwd
/home/user
$ java -jar NGB/dist/catgenome.jar
```

NGB will be avalable at http://localhost:8080/catgenome

## Configuring NGB instance

By default NGB will run on port 8080 and locate all the data (files and database) in the runtime folder
To customize the configuration the following options are available

### Configure data storage

You can provide an external file **catgenome.properties** to specify data location. Available properties:

* **files.base.directory.path=/opt/catgenome/contents** path for storing NGB files (Fasta, BAM, VCF, etc.)
* **database.driver.class=org.h2.Driver** driver for NGB database, default database is H2
* **database.jdbc.url=jdbc:h2:file:/opt/catgenome/H2/catgenome** path to NGB database location
* **database.username=catgenome** user for NGB database
* **database.password=** password for NGB database, may be empty
* **database.max.pool.size=25** NGB database connection pool configuration
* **database.initial.pool.size=5** NGB database connection pool configuration

If you want to enable browsing NGS files directly from server's file system, add the following properties:

* **file.browsing.allowed=true** - enables file browsing from file system
* **ngs.data.root.path=/opt/catgenome** - sets root of allowed to browsing file system part to /opt/catgenome. 

If this property is not set, root will be set to the root of file system.

If you want to configure default options for tracks visualization on a client side, add the following properties:
* **config.path=/opt/catgenome/configs** path to a directory that contains `json` configuration files for NGB client

Properties for configuring full-text search settings:
* **search.indexer.buffer.size** Lucene indexer buffer size in MB, larger size of buffer increases speed of file
 indexing and search performance 
* **lucene.index.max.size.grouping** Sets maximum Lucene index size in bytes to perform groping requests

You should put **catgenome.properties** in **config** folder in the runtime folder or provide path to folder with properties file from command line:
 
```
$ java -jar catgenome.jar --conf=/folder/with/properties
```
 
### Configure Embedded Tomcat

NGB uses Spring Boot so it supports a full stack of Spring Boot Application properties.
These properties may be specified by the command line:

```
# Run NGB on 9999 port 
$ java -jar catgenome.jar --server.port=9999
# Disable traffic compression
$ java -jar catgenome.jar --server.compression.enabled=false
```
 
or in **application.properties** file in **config** folder in the runtime folder:
* server.port=9999
* server.compression.enabled=false

See the full list of available options in the [Spring Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/common-application-properties.html)
