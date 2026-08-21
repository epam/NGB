# NGB server + CLI, no data. Published as lifescience/ngb:<version>.
#
# The build context is this directory, and two artifacts have to be copied into it first:
#
#   catgenome.jar   the Spring Boot executable jar   (./gradlew buildJar)
#   ngb-cli.tar.gz  the CLI distribution             (./gradlew buildCli)
#
# `./gradlew buildDocker` does the copying and the build; see ../README.md to do it by hand.
#
# The base image carries tar, gzip, curl and wget, so nothing is installed here at all. It used to
# be ubuntu:16.04 + `apt-get install wget openjdk-8-jre nginx`; the nginx was installed, never
# configured and never started in any commit since 2021 (see ../README.md on reverse proxies).
FROM eclipse-temurin:21-jre-jammy

ENV INSTALL_DIR=/opt
ENV NGB_HOME=$INSTALL_DIR/ngb
ENV CLI_HOME=$INSTALL_DIR/ngb-cli/bin
ENV NGS_DATA_DIR=/ngs
ENV PATH=$PATH:$CLI_HOME

# --enable-native-access=ALL-UNNAMED is not optional noise: Lucene 9's MMapDirectory calls
# java.lang.foreign, and without it every start prints three "WARNING: A restricted method in
# java.lang.foreign.Linker has been called" lines. The manifest attribute that would remove the need
# for a flag (Enable-Native-Access) is JDK 24+. Override the whole variable to change the heap.
ENV NGB_JAVA_OPTS="-Xmx2G --enable-native-access=ALL-UNNAMED"

# NGB server binaries
RUN mkdir -p ${NGB_HOME}
COPY catgenome.jar ${NGB_HOME}/

# NGB Command Line Interface
COPY ngb-cli.tar.gz ${INSTALL_DIR}/
RUN cd ${INSTALL_DIR} && \
    tar -zxf ngb-cli.tar.gz && \
    rm ngb-cli.tar.gz

# Configures "Open from NGB server" and the registerable path root at start time, from
# NGS_DATA_DIR - see the file for why that is not done here.
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# The H2 database and the parsed-file cache are written relative to the working directory, so
# ${NGB_HOME}/H2 and ${NGB_HOME}/contents are what an operator mounts to keep them.
WORKDIR ${NGB_HOME}

EXPOSE 8080
ENTRYPOINT ["/entrypoint.sh"]
# `exec` so that java is PID 1 and stops on SIGTERM rather than being killed after the timeout.
CMD ["sh", "-c", "exec java ${NGB_JAVA_OPTS} -jar catgenome.jar"]
