# Requirements

* **[Docker engine](https://docs.docker.com/engine/installation/)**
* **JDK 21** — [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=21) or any other
  build. This is what the build needs; the images themselves carry their own JRE, so nothing has to
  be installed on the machine that *runs* them.

# How to build NGB docker image

Obtain the source code from github:
```
$ git clone https://github.com/epam/NGB.git
$ cd NGB
```

## The short way

```
$ ./gradlew buildDocker        # builds the jar and the CLI, then `ngb:latest`
$ ./gradlew buildDockerDemo    # the above, then `ngb:latest-demo` on top of it
```

## By hand

### Build NGB server jar

```
$ ./gradlew buildJar
$ cp dist/catgenome.jar docker/core/catgenome.jar
```

### Build NGB Command Line Interface tarball

```
$ ./gradlew buildCli
$ cp dist/ngb-cli.tar.gz docker/core/ngb-cli.tar.gz
```

### Build docker image

```
$ cd docker/core
$ docker build -t ngb:latest .
$ rm catgenome.jar ngb-cli.tar.gz
```

# How to run NGB docker image

To run an image, that was built in a previous section, run the following command

```
docker run -p 8080:8080 -d --name ngbcore ngb:latest
```

This command performs the following operations
* Create and start the container in a background mode
* Map port 8080 of the container to port 8080 of the host 
* Make container accessible by name `ngbcore`

Verify that NGB is up and running: navigate with your web-browser (**Chrome** prefered) to [http://localhost:8080/catgenome](http://localhost:8080/catgenome)

## Keeping state, and pointing the server at data

The H2 database and the parsed-file cache are written relative to the working directory, which is
`/opt/ngb`. `NGS_DATA_DIR` (default `/ngs`) is both the root of the "Open from NGB server" file
browser and the only tree under which local files can be registered — a path outside it is refused
with *Parameter path doesn't fall into 'ngs.data.root.path'*. So mount data there:

```
docker run -p 8080:8080 -d --name ngbcore \
    -v /opt/ngb/H2:/opt/ngb/H2 \
    -v /opt/ngb/contents:/opt/ngb/contents \
    -v /my/ngs:/ngs \
    ngb:latest
```

or move the root, which the entrypoint writes into `catgenome.properties` at every start:

```
docker run ... -e NGS_DATA_DIR=/data -v /my/ngs:/data ngb:latest
```

Mounting your own `/opt/ngb/config/catgenome.properties` (or the whole `/opt/ngb/config`) takes
precedence over both — the entrypoint leaves an existing file alone.

`NGB_JAVA_OPTS` holds the JVM flags — override the whole variable to change the heap, and keep
`--enable-native-access=ALL-UNNAMED` when you do, or every start prints three warnings about
restricted `java.lang.foreign` calls from Lucene's `MMapDirectory`:

```
docker run ... -e NGB_JAVA_OPTS="-Xmx8G --enable-native-access=ALL-UNNAMED" ngb:latest
```

## TLS, authentication and other front-end concerns

The image serves plain HTTP on 8080 and contains no web server of its own. Anything in front of
the application — TLS termination, a hostname, HTTP authentication, rate limiting — belongs in a
separate reverse-proxy container (nginx, Caddy, Traefik) alongside this one, or in the ingress of
whatever runs it. Up to NGB 3.0.0 the image did `apt-get install nginx`, but never configured or
started it; that package is gone rather than the capability.

NGB's own JWT and SAML SSO authentication is a server setting rather than a proxy one — see
"Configuring NGB instance" in [installation/standalone.md](../docs/md/installation/standalone.md),
and mount the resulting `catgenome.properties` over `/opt/ngb/config/catgenome.properties`.

# Latest stable and demo docker images

Latest stable NGB version is available at [DockerHub Repository](https://hub.docker.com/r/lifescience/ngb/)

There are two version of NGB in this repository:
* **ngb:latest** - a "core" version - contains image of NGB without any data in it, only binaries
* **ngb:latest-demo** - a "demo" version - contains demo data set, which does not require any data registration, you need only to run an image

Details on usage of these images are available in [DockerHub Readme](https://hub.docker.com/r/lifescience/ngb/)

## Building the demo image with less in it

`docker/demo/Dockerfile` downloads four references and the demo data set — about 3.4 GB
compressed, 13 GB unpacked — so a full build takes as long as the download does. Three build
arguments narrow it, which is what makes the image practical to test:

```
$ cd docker/demo
$ docker build -t ngb:latest-demo \
      --build-arg REFERENCES=dm6 --build-arg DEMO_DATA=false .
```

`REFERENCES` is a space-separated subset of `grch38 grch37 dm6 mm`, `DEMO_DATA=false` skips the
data set, and `DATA_URL` (default
`https://ngb-oss-builds.s3.amazonaws.com/public/data`) repoints both at a mirror.

Everything lands under `/ngs` — `/ngs/reference/<ref>/` and `/ngs/ngb_demo_data/` — for the reason
in the previous section. The image ships the data, not the registrations: the `ngb reg_ref` /
`reg_file` / `reg_dataset` commands at the bottom of `start-demo.sh` are what turns it into the
demo, and they can be run inside a container of this image with `docker exec` because the paths are
the same either way.

`start-demo.sh <version>` is the other way round: it downloads the same data onto the host and
mounts it into the *core* image, then registers it with the CLI. That script is what produces the
public demo instance.
