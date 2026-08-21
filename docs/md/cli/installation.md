# CLI installation

CLI needs a **Java 17 or newer** runtime on the machine it runs on; unlike the server, it does not
require Java 21 and does not need `--enable-native-access=ALL-UNNAMED`. It talks to the server over
the REST API, so it does not have to run on the server host at all.

There are two options to install NGB CLI, depending on NGB Server installation scenario:

- NGB Docker image installation
- NGB Manual installation

## NGB Docker image installation

CLI is already installed inside a Docker image and is added to $PATH variable.

To access CLI, console should be attached to a running docker container and ngb command could be used immediately.

For details on running docker image and attaching to a container - see description at [https://hub.docker.com/r/lifescience/ngb](https://hub.docker.com/r/lifescience/ngb).

## NGB Manual installation

Download `ngb-cli.tar.gz` from one of the
[distribution locations](../installation/overview.md#distributions) and unpack it (**Note**: replace
the values for CLI_HOME and NGB_CLI_URL, if needed):

```bash
# Create a directory for ngb-cli
$ CLI_HOME=/opt/ngb-cli

# One of the release assets at https://github.com/epam/NGB/releases, or a versioned build:
$ NGB_CLI_URL=https://ngb-oss-builds.s3.amazonaws.com/public/builds/release/2.7.1/2.7.1.4384/ngb-cli-2.7.1.4384.tar.gz

$ mkdir -p $CLI_HOME && cd $CLI_HOME

# Download and unpack ngb-cli
$ wget -O ngb-cli.tar.gz "$NGB_CLI_URL" && \
    tar -zxvf ngb-cli.tar.gz && \
    rm ngb-cli.tar.gz

# Write ngb-cli location to $PATH
$ export PATH="$CLI_HOME/ngb-cli/bin:$PATH"
```

There is no `latest` alias: every archive carries its version in its name. Build one from source
with `./gradlew buildCli`, which leaves `dist/ngb-cli.tar.gz`.
