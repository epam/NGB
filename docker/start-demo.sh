#!/bin/bash

NGB_VERSION="$1"
if [ -z "$NGB_VERSION" ]; then
    echo "NGB version is not speficied"
    exit 1
fi

mkdir -p /opt/ngb/reference/grch38 && \
    mkdir -p /opt/ngb/reference/grch37 && \
    mkdir -p /opt/ngb/reference/dm6 && \
    mkdir -p /opt/ngb/reference/mm

cd /opt/ngb/reference/grch38 && \
    wget --quiet https://ngb-oss-builds.s3.amazonaws.com/public/data/genome/grch38/Homo_sapiens.GRCh38.gtf.gz  && \
    wget --quiet https://ngb-oss-builds.s3.amazonaws.com/public/data/genome/grch38/Homo_sapiens.GRCh38.fa.gz && \
    wget --quiet https://ngb-oss-builds.s3.amazonaws.com/public/data/genome/grch38/Homo_sapiens.GRCh38.domains.bed && \
    gzip -d Homo_sapiens.GRCh38.fa.gz

cd /opt/ngb/reference/grch37 && \
    wget --quiet https://ngb-oss-builds.s3.amazonaws.com/public/data/genome/grch37/Homo_sapiens.GRCh37.gtf.gz  && \
    wget --quiet https://ngb-oss-builds.s3.amazonaws.com/public/data/genome/grch37/Homo_sapiens.GRCh37.fa.gz && \
    gzip -d Homo_sapiens.GRCh37.fa.gz

cd /opt/ngb/reference/dm6 && \
    wget --quiet https://ngb-oss-builds.s3.amazonaws.com/public/data/genome/dm6/dmel-all-r6.06.sorted.gtf.gz && \
    wget --quiet https://ngb-oss-builds.s3.amazonaws.com/public/data/genome/dm6/dmel-all-chromosome-r6.06.fasta.gz && \
    gzip -d dmel-all-chromosome-r6.06.fasta.gz

cd /opt/ngb/reference/mm && \
    wget --quiet https://ngb-oss-builds.s3.amazonaws.com/public/data/genome/mm/Mus_musculus.GRCm38.sorted.gtf.gz && \
    wget --quiet https://ngb-oss-builds.s3.amazonaws.com/public/data/genome/mm/Mus_musculus.GRCm38.fa.gz && \
    gzip -d Mus_musculus.GRCm38.fa.gz

cd /opt/ngb && \
    wget --quiet https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data.tar.gz && \
    tar -zxvf ngb_demo_data.tar.gz && \
    rm -f ngb_demo_data.tar.gz

cd /opt/ngb && \
    wget --quiet https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb-demo-index-cache/2.6.0/ngb-demo-index-cache.tar.gz && \
    tar -zxvf ngb-demo-index-cache.tar.gz && \
    rm -f ngb-demo-index-cache.tar.gz

docker run -d -p 8080:8080  -v /opt/ngb/reference:/reference \
                            -v /opt/ngb/contents:/opt/ngb/contents \
                            -v /opt/ngb/H2:/opt/ngb/H2 \
                            -v /opt/ngb/ngb_demo_data:/opt/data/ngb_demo_data \
                            lifescience/ngb:$NGB_VERSION
