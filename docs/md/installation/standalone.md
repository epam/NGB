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
  
## Get Source Code

Obtain NGB source code from GitHub:
```
$ git clone https://github.com/epam/NGB.git
$ cd NGB

```

## Build JAR file

Build NGB standalone jar, using build.sh script
```
$ ./build.sh --jar

```

You can find **catgenome.jar** archive in the **dist/** folder

## Run JAR file

Run **catgenome.jar**

```
$ java -jar catgenome.jar

```

NGB will be avalable at http://localhost:8080/catgenome

### Configuring NGB instance

By default NGB will run on port 8080 and locate all the data (files and database) in the runtime folder
To customize the configuration the following  options are available

#### Configure data storage

You can provide an external file **catgenome.properties** to specify data location. Available properties:
 * **files.base.directory.path=/opt/catgenome/contents** path for storing NGB files (Fasta, BAM, VCF, etc.)
 * **database.driver.class=org.h2.Driver** driver for NGB database, default database is H2
 * **database.jdbc.url=jdbc:h2:file:/opt/catgenome/H2/catgenome** path to NGB database location
 * **database.username=catgenome** user for NGB database
 * **database.password=** password for NGB database, may be empty
 * **database.max.pool.size=25** NGB database connection pool configuration
 * **database.initial.pool.size=5** NGB database connection pool configuration
 
You should put **catgenome.properties** in **config** folder in the runtime folder or provide path
 to folder with properties file from command line:
 
 ```
 $ java -jar catgenome.jar --conf=/folder/with/properties
 
 ```
 
#### Configure Embedded Tomcat

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
