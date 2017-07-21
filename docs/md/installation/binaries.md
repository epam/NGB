# Installing NGB from binaries
NGB is provided as java web-archive (war file) that could be deployed to a TOMCAT infrastructure

## Environment requirements

*Note:  To install the software you must have administrator privileges on the system.*

Verify that your system meets or exceeds the following hardware/software requirements

* Server hardware requirements
  * CPU: 2 cores
  * RAM: 4Gb
  * HDD: 20 Gb free space
* Server software requirements
  * Ubuntu >= 14.04
  * CentOS >= 6
  * RedHat >= 6
  * **[Oracle JDK 8](https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html)** or **[Open JDK 8](http://openjdk.java.net/install/)**
  * **[Tomcat 8](https://tomcat.apache.org/tomcat-8.0-doc/setup.html)**
* Client web-browser requirements
  * Chrome (>= 56)
  * Firefox (>= 51)
  * Safari (>= 9)
  * EDGE (>= 25)

*We'll refer to Tomcat installation directory as **$CATALINA_HOME**, assuming that NGB binary distribution 
is located at **/home/user/catgenome.war** and NGB data folder is **/opt/catgenome/**.*

Stop Tomcat, if it is running
```
sudo $CATALINA_HOME/bin/catalina.sh stop
```

Delete **$CATALINA_HOME/webapps/catgenome** folder and **$CATALINA_HOME/webapps/catgenome.war**, if you've previously installed NGB

```
#Delete catgenome folder
if [ -d $CATALINA_HOME/webapps/catgenome ]
    then
       rm -Rf $CATALINA_HOME/webapps/catgenome
fi  
if [ -f  $CATALINA_HOME/webapps/catgenome.war ]
   then
      rm -f  $CATALINA_HOME/webapps/catgenome.war
fi  
```

Copy **catgenome.war** to **$CATALINA_HOME/webapps/** folder
```
cp /home/user/catgenome.war $CATALINA_HOME/webapps/
```

Set path to NGB configuration file for Tomcat configuration file **$CATALINA_HOME/conf/catalina.properties**
```
mkdir $CATALINA_HOME/conf/catgenome/
echo "CATGENOME_CONF_DIR=${CATALINA_HOME}/conf/catgenome/" >> $CATALINA_HOME/conf/catalina.properties
```   
Create **catgenome.properies** file with NGB configuration in **$CATGENOME_CONF_DIR**. This file must include the following 
properties:
* **files.base.directory.path=/opt/catgenome/contents** path for storing NGB files (Fasta, BAM, VCF, etc.)
* **database.driver.class=org.h2.Driver** driver for NGB database, default database is H2
* **database.jdbc.url=jdbc:h2:file:/opt/catgenome/H2/catgenome** path to NGB database location
* **database.username=catgenome** user for NGB database
* **database.password=** password for NGB database, may be empty
* **database.max.pool.size=25** NGB database connection pool configuration
* **database.initial.pool.size=5** NGB database connection pool configuration
```
echo "files.base.directory.path=/opt/catgenome/contents" > $CATGENOME_CONF_DIR/catgenome.properties
echo "database.driver.class=org.h2.Driver" >> $CATGENOME_CONF_DIR/catgenome.properties
echo "database.jdbc.url=jdbc:h2:file:/opt/catgenome/H2/catgenome" >> $CATGENOME_CONF_DIR/catgenome.properties
echo "database.username=catgenome" >>$CATGENOME_CONF_DIR/catgenome.properties
echo "database.password=" >> $CATGENOME_CONF_DIR/catgenome.properties
echo "database.initial.pool.size=5" >> $CATGENOME_CONF_DIR/catgenome.properties
echo "database.max.pool.size=25" >> $CATGENOME_CONF_DIR/catgenome.properties
```   
If you want to enable browsing NGS files directly from server's file system, add the following properties:
* **file.browsing.allowed=true** - enables file browsing from file system
* **ngs.data.root.path=/opt/catgenome** - sets root of allowed to browsing file system part to /opt/catgenome. 
If this property is not set, root will be set to the root of file system.
    
```
echo "file.browsing.allowed=true" >> $CATGENOME_CONF_DIR/catgenome.properties
echo "ngs.data.root.path=/opt/catgenome" >> $CATGENOME_CONF_DIR/catgenome.properties
```

If you want to specify max number of VcfIndexEntries keeping in memory during vcf loading, add the following property. For files, which produce more entries then the number, extra entries will be spilled to disk (temp directory).
* **files.vcf.max.entries.in.memory=1000000** - 1000000 entries take about 3Gb in the heap

```
echo "files.vcf.max.entries.in.memory=1000000" >> $CATGENOME_CONF_DIR/catgenome.properties
```

Set Tomcat configuration in the file **$CATALINA_HOME/conf/server.xml** by adding the 
 following values to the **"Connector"** XML node
 
 `sed -i '/Connector port="8080"/,/redirectPort="8443" \`
 `  /c\<Connector port="8080" protocol="HTTP/1.1" connectionTimeout="20000" \`
 `  compression="on" compressionMinSize="2048" compressableMimeType="text/html,text/xml,application/json,application/javascript" \`
 `  redirectPort="8443"/>' $CATALINA_HOME/conf/server.xml`

Start Tomcat
```
sudo $CATALINA_HOME/bin/catalina.sh start
```
NGB will be available on port 8080 (if it was not changed): **[http://localhost:8080/catgenome/](http://localhost:8080/catgenome/)**

