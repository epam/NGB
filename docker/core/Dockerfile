FROM ubuntu:16.04

ENV CATALINA_HOME /opt/tomcat
ENV NGB_HOME /opt/catgenome
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64/jre
ENV JAVA_OPTS "-Xms256m -Xmx2048m -XX:MaxPermSize=768m -XX:ReservedCodeCacheSize=225m"
ENV PATH $PATH:$JAVA_HOME/bin:$CATALINA_HOME/bin:$CATALINA_HOME/scripts:$NGB_HOME/ngb-cli/bin

#Install OpenJDK 8
RUN apt-get -y update && \
	apt-get -y install wget default-jdk

#Install tomcat 8
RUN wget -q http://apache-mirror.rbc.ru/pub/apache/tomcat/tomcat-8/v8.0.42/bin/apache-tomcat-8.0.42.tar.gz && \
        tar -xvf apache-tomcat-8.0.42.tar.gz && \
        rm apache-tomcat*.tar.gz && \
        mv apache-tomcat* ${CATALINA_HOME}

#Install NGB binaries
COPY catgenome.war ${CATALINA_HOME}/webapps/catgenome/
RUN	cd ${CATALINA_HOME}/webapps/catgenome && \
		jar xvf catgenome.war

#Install NGB CLI 
COPY ngb-cli.tar ${NGB_HOME}/
RUN cd ${NGB_HOME} && \ 
		tar -xvf ngb-cli.tar && \
		rm ngb-cli.tar && \
		cd ${NGB_HOME}/ngb-cli/bin && \
		chmod +x ngb

#Write NGB configuration
#--Add catgenome config location
RUN echo "CATGENOME_CONF_DIR=${CATALINA_HOME}/conf/catgenome/" >> $CATALINA_HOME/conf/catalina.properties

#--Add gzip to tomcat config
RUN	sed -i 's|Connector port="8080"|Connector port="8080" compression="on" compressionMinSize="2048" compressableMimeType="text/html,text/xml,application/json,application/javascript"|g' ${CATALINA_HOME}/conf/server.xml

#--Add catgenome config
RUN mkdir $CATALINA_HOME/conf/catgenome && \
	echo "files.base.directory.path=/opt/catgenome/contents" >> $CATALINA_HOME/conf/catgenome/catgenome.properties && \
	echo "database.max.pool.size=25" >> $CATALINA_HOME/conf/catgenome/catgenome.properties && \
	echo "database.username=catgenome" >> $CATALINA_HOME/conf/catgenome/catgenome.properties && \
	echo "database.password=" >> $CATALINA_HOME/conf/catgenome/catgenome.properties && \
	echo "database.initial.pool.size=5" >> $CATALINA_HOME/conf/catgenome/catgenome.properties && \
	echo "database.driver.class=org.h2.Driver" >> $CATALINA_HOME/conf/catgenome/catgenome.properties && \
	echo "database.jdbc.url=jdbc:h2:file:/opt/catgenome/H2/catgenome" >> $CATALINA_HOME/conf/catgenome/catgenome.properties
	
WORKDIR $NGB_HOME/ngb-cli/bin
EXPOSE 8080
CMD $CATALINA_HOME/bin/catalina.sh run
