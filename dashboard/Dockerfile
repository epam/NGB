# Adds User stats report to prebuilt NGB image
FROM ngb:latest
# FROM ubuntu:16.04

ARG ANACONDA_VERSION="3-latest"
ENV ANACONDA_HOME=/opt/local/anaconda
ENV PATH="$PATH:$ANACONDA_HOME/bin"

ENV DASHBOARD_HOME=/opt/dashboard
ADD dashboard/ "$DASHBOARD_HOME/dashboard/"
ADD parser/ "$DASHBOARD_HOME/parser/"
ADD requirements.txt "$DASHBOARD_HOME/"
ADD start.sh "$DASHBOARD_HOME/"

RUN mkdir -p "$ANACONDA_HOME" && \
    wget -q "https://repo.anaconda.com/miniconda/Miniconda${ANACONDA_VERSION}-Linux-x86_64.sh" -O /tmp/Anaconda_Install.sh --no-check-certificate && \
    bash /tmp/Anaconda_Install.sh -f -b -p "$ANACONDA_HOME" && \
    rm -f /tmp/Anaconda_Install.sh && \
    conda init bash && \
    . $ANACONDA_HOME/etc/profile.d/conda.sh && \
    conda create -y -n dashboard python==3.10.14 && \
    conda activate dashboard && \
    pip3 install -r "$DASHBOARD_HOME/requirements.txt"

# Set up parser
ENV NGB_LOG_FOLDER=/opt/ngb/logs/
ENV NGB_STATS_FILE="$DASHBOARD_HOME/stats/ngb-user-stats.parquet"
ENV NGS_LOG_SYNC_TIMESTAMP="$DASHBOARD_HOME/stats/sync.txt"
ENV CRON_FILE="$DASHBOARD_HOME/parser.sh"

RUN apt-get install -y cron && \
    echo "0 1 * * * . $ANACONDA_HOME/etc/profile.d/conda.sh && conda activate dashboard && cd $DASHBOARD_HOME/parser && python3 log_parser.py >> $DASHBOARD_HOME/parser.log 2>&1\n" >> "$CRON_FILE" && \
    chmod 0644 "$CRON_FILE" && \
    crontab "$CRON_FILE"


