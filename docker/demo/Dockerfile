FROM ngb:latest

    # Create reference folders
RUN mkdir -p /reference/grch38 && \
    mkdir -p /reference/grch37 && \
    mkdir -p /reference/dm6 && \
    mkdir -p /reference/mm && \

    # Download grch38
    cd /reference/grch38 && \
    wget --quiet http://ngb.opensource.epam.com/distr/data/genome/grch38/Homo_sapiens.GRCh38.gtf.gz  && \
    wget --quiet http://ngb.opensource.epam.com/distr/data/genome/grch38/Homo_sapiens.GRCh38.fa.gz && \
    wget --quiet http://ngb.opensource.epam.com/distr/data/genome/grch38/Homo_sapiens.GRCh38.domains.bed && \
    gzip -d Homo_sapiens.GRCh38.fa.gz && \

    # Download grch37
    cd /reference/grch37 && \
    wget --quiet http://ngb.opensource.epam.com/distr/data/genome/grch37/Homo_sapiens.GRCh37.gtf.gz  && \
    wget --quiet http://ngb.opensource.epam.com/distr/data/genome/grch37/Homo_sapiens.GRCh37.fa.gz && \
    gzip -d Homo_sapiens.GRCh37.fa.gz && \

    # Download dm6
    cd /reference/dm6 && \
    wget --quiet http://ngb.opensource.epam.com/distr/data/genome/dm6/dmel-all-r6.06.sorted.gtf.gz && \
    wget --quiet http://ngb.opensource.epam.com/distr/data/genome/dm6/dmel-all-chromosome-r6.06.fasta.gz && \
    gzip -d dmel-all-chromosome-r6.06.fasta.gz && \

    # Download grcm38
    cd /reference/mm && \
    wget --quiet http://ngb.opensource.epam.com/distr/data/genome/mm/Mus_musculus.GRCm38.sorted.gtf.gz && \
    wget --quiet http://ngb.opensource.epam.com/distr/data/genome/mm/Mus_musculus.GRCm38.fa.gz && \
    gzip -d Mus_musculus.GRCm38.fa.gz && \

    # Download demo data
    cd / && \
    wget --quiet http://ngb.opensource.epam.com/distr/data/demo/ngb_demo_data.tar.gz && \
    tar -zxvf ngb_demo_data.tar.gz && \
    rm ngb_demo_data.tar.gz && \

    # Download cached NGB index
    cd $NGB_HOME && \
    wget --quiet http://ngb.opensource.epam.com/distr/data/demo/ngb-demo-index-cache.tar.gz && \
    tar -zxvf ngb-demo-index-cache.tar.gz && \
    rm ngb-demo-index-cache.tar.gz



EXPOSE 8080
CMD cd $NGB_HOME && java -Xmx2G -jar catgenome.jar
