FROM ubuntu:16.04

ENV NGB_HOME /opt/ngb/
ENV CLI_HOME /opt/ngb-cli/
ENV PATH $PATH:$JAVA_HOME/bin:$CATALINA_HOME/bin:$CATALINA_HOME/scripts:$NGB_HOME/ngb-cli/bin

#Install OpenJDK 8
RUN apt-get -y update && \
	apt-get -y install wget default-jdk

#Install NGB server binaries
RUN mkdir ${NGB_HOME}
COPY catgenome.jar ${NGB_HOME}

#Install NGB CLI 
COPY ngb-cli.tar.gz ${NGB_HOME}
RUN cd ${NGB_HOME} && \ 
		tar -xvf ngb-cli.tar && \
		rm ngb-cli.tar && \
		cd ${NGB_HOME}/ngb-cli/bin && \
		chmod +x ngb
	
WORKDIR $NGB_HOME/ngb-cli/bin
EXPOSE 8080
CMD $CATALINA_HOME/bin/catalina.sh run
