FROM ubuntu:16.04

ENV INSTALL_DIR /opt/
ENV NGB_HOME $INSTALL_DIR/ngb/
ENV CLI_HOME $INSTALL_DIR/ngb-cli/bin/
ENV NGS_DATA_DIR /ngs/
ENV PATH $PATH:$CLI_HOME

#Install OpenJDK 8
RUN apt-get -y update && \
    apt-get -y install wget openjdk-8-jre nginx

#Install NGB server binaries
RUN mkdir ${NGB_HOME}
COPY catgenome.jar ${NGB_HOME}

#Install NGB CLI
COPY ngb-cli.tar.gz ${INSTALL_DIR}
RUN cd ${INSTALL_DIR} && \
    tar -zxvf ngb-cli.tar.gz && \
    rm ngb-cli.tar.gz


# Configure "Open from NGB server"
RUN mkdir $NGS_DATA_DIR && \
	cd $NGB_HOME && \
	mkdir config && \
	cd config && \
	echo "file.browsing.allowed=true" >> catgenome.properties && \
    echo "ngs.data.root.path=${NGS_DATA_DIR}" >> catgenome.properties


EXPOSE 8080
CMD cd $NGB_HOME && java -Xmx2G -jar catgenome.jar
