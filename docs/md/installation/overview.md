# NGB installation

There are three options to install NGB

* **[Run a standalone jar file](standalone.md)** — needs a Java 21 runtime on the host
* **[Run a JRE-bundled archive](standalone.md#running-the-jre-bundled-distribution)** — the same
  server with an Eclipse Temurin 21 runtime inside it, for a host with no Java
* **[Use Docker image](docker.md)**

These options are described in details in the subsequent sections.

If you are upgrading an existing NGB instance to 3.0.0, read
[Before you upgrade](../release-notes/3.0.0/3.0.0.md#before-you-upgrade) first: the database and the
search indexes both need a one-time conversion that NGB will not do on its own.

## Distributions

NGB distributions are available at the following locations:

* Jar (`catgenome.jar`, `catgenome-h2.jar`, `catgenome-psql.jar`), CLI (`ngb-cli.tar.gz`) and
  documentation (`ngb-docs.tar.gz`)
    * [https://github.com/epam/NGB/releases](https://github.com/epam/NGB/releases)
    * every build of a release branch:
      [browse them](https://ngb-oss-builds.s3.amazonaws.com/web/index.html?prefix=public/builds/release/),
      laid out as
      `public/builds/<branch>/<version>/<artifact>-<version>.<ext>`
    * every build of `develop`:
      [browse them](https://ngb-oss-builds.s3.amazonaws.com/web/index.html?prefix=public/builds/develop/)
* JRE-bundled archives (`ngb-server-linux.tgz`, `ngb-server-windows.zip`) — built by the release
  workflow and attached to its run, or from source with
  `./gradlew -p server/catgenome bundleLinux bundleWindows`
* Docker image
    * [https://hub.docker.com/r/lifescience/ngb](https://hub.docker.com/r/lifescience/ngb/)

## General requirements

The following minimal requirements should be met for the server host machine

* Server hardware requirements
    * CPU: 2 cores
    * RAM: 4Gb
    * HDD: 20 Gb free space
* Server software requirements
    * A **Java 21** runtime, unless you use the docker image or a JRE-bundled archive, both of
      which carry their own. See
      [Environment requirements](standalone.md#environment-requirements).
    * 64-bit Linux with glibc 2.17 or newer — RedHat / CentOS >= 7, Ubuntu >= 16.04. CentOS and
      RedHat 6 will no longer work at all: their glibc is 2.12 and no Java 21 runtime will load
      on them. The published archives and image are x86_64; `-PbundleArch=aarch64` builds an
      aarch64 bundle from source.
    * Windows is supported through `ngb-server-windows.zip` and through a plain
      `java -jar catgenome.jar`.
* Client web-browser requirements
    * Chrome (>= 56)
    * Firefox (>= 51)
    * Safari (>= 9)
    * EDGE (>= 25)


