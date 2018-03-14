# CLI installation
There are two options two install NGB CLI, depending on NGB Server installation scenario:
* NGB Docker image installation
* NGB Manual installation

## NGB Docker image installation
CLI is already installed inside a Docker image and is added to $PATH variable.

To access CLI, console should be attached to a running docker container and ngb command could be used immediately.

For details on running docker image and attaching to a container - see description at https://hub.docker.com/r/lifescience/ngb

## NGB Manual installation
To manually install NGB CLI one can use the following script:

(replace the value for CLI_HOME variable, if needed)
```bash
# Create a directory for ngb-cli
$ CLI_HOME=/opt/catgenome

$ mkdir $CLI_HOME && cd $CLI_HOME

# Download and unpack ngb-cli
$ wget http://ngb.opensource.epam.com/distr/latest/ngb-cli-latest.tar.gz && \
    tar -zxvf ngb-cli-latest.tar.gz && \
    rm ngb-cli-latest.tar.gz

# Write ngb-cli location to $PATH
$ export PATH="$CLI_HOME/ngb-cli/bin:$PATH"
```
