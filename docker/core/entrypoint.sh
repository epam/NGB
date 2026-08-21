#!/bin/sh
#
# Writes $NGB_HOME/config/catgenome.properties from the environment before starting the server,
# unless the operator already supplied one by mounting a file (or a whole config directory) over it.
#
# This used to be a build-time RUN, which made `docker run -e NGS_DATA_DIR=/data` a silent no-op:
# ngs.data.root.path was fixed at /ngs in the image. That property is not only the root of the
# "Open from NGB server" file browser - since UrlValidatorService (Apr 2025) it is also checked
# against the path of every locally registered file - so a data volume mounted anywhere else got
# "Parameter path doesn't fall into 'ngs.data.root.path'" from `ngb reg_ref` and there was no way
# to change it short of rebuilding the image.

set -e

CONFIG="$NGB_HOME/config/catgenome.properties"

if [ -f "$CONFIG" ]; then
    echo "entrypoint: using the supplied $CONFIG"
else
    mkdir -p "$NGB_HOME/config"
    {
        echo "file.browsing.allowed=true"
        echo "ngs.data.root.path=$NGS_DATA_DIR"
    } > "$CONFIG"
    echo "entrypoint: wrote $CONFIG (ngs.data.root.path=$NGS_DATA_DIR)"
fi

mkdir -p "$NGS_DATA_DIR"

exec "$@"
