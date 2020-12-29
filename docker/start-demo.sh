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

docker rm -f ngb
docker run -d -p 8080:8080  -v /opt/ngb/reference:/reference \
                            -v /opt/ngb/contents:/opt/ngb/contents \
                            -v /opt/ngb/H2:/opt/ngb/H2 \
                            -v /opt/ngb/ngb_demo_data:/opt/data/ngb_demo_data \
                            --name ngb \
                            lifescience/ngb:$NGB_VERSION

# Register ref
#    GRCh38
ngb reg_ref /reference/grch38/Homo_sapiens.GRCh38.fa --name GRCh38
ngb reg_file GRCh38 /reference/grch38/Homo_sapiens.GRCh38.gtf.gz --name GRCh38_Genes
ngb add_genes GRCh38 GRCh38_Genes
ngb reg_file GRCh38 /reference/grch38/Homo_sapiens.GRCh38.domains.bed --name GRCh38_Domains
ngb add_ann GRCh38 GRCh38_Domains

#    DM6
ngb reg_ref /reference/dm6/dmel-all-chromosome-r6.06.fasta --name DM6
ngb reg_file DM6 /reference/dm6/dmel-all-r6.06.sorted.gtf.gz --name DM6_Genes
ngb add_genes DM6 DM6_Genes

# Register datasets
#    SV_Sample1
ngb reg_dataset GRCh38 SV_Sample1
ngb reg_file GRCh38 /opt/data/ngb_demo_data/sample_1-lumpy.vcf
ngb add_dataset SV_Sample1 sample_1-lumpy.vcf
ngb reg_file GRCh38 /opt/data/ngb_demo_data/sv_sample_1.bam?/opt/data/ngb_demo_data/sv_sample_1.bam.bai
ngb add_dataset SV_Sample1 sv_sample_1.bam
ngb reg_file GRCh38 /opt/data/ngb_demo_data/sv_sample_1.bw
ngb add_dataset SV_Sample1 sv_sample_1.bw

#    SV_Sample2
ngb reg_dataset GRCh38 SV_Sample2
ngb reg_file GRCh38 /opt/data/ngb_demo_data/sample_2-lumpy.vcf
ngb add_dataset SV_Sample2 sample_2-lumpy.vcf
ngb reg_file GRCh38 /opt/data/ngb_demo_data/sv_sample_2.bam?/opt/data/ngb_demo_data/sv_sample_2.bam.bai
ngb add_dataset SV_Sample2 sv_sample_2.bam
ngb reg_file GRCh38 /opt/data/ngb_demo_data/sv_sample_2.bw
ngb add_dataset SV_Sample2 sv_sample_2.bw

#    PIK3CA-E545K-Sample
ngb reg_dataset GRCh38 PIK3CA-E545K-Sample
ngb reg_file GRCh38 /opt/data/ngb_demo_data/PIK3CA-E545K.bam?/opt/data/ngb_demo_data/PIK3CA-E545K.bam.bai
ngb add_dataset PIK3CA-E545K-Sample PIK3CA-E545K.bam
ngb reg_file GRCh38 /opt/data/ngb_demo_data/PIK3CA-E545K.cram?/opt/data/ngb_demo_data/PIK3CA-E545K.cram.crai
ngb add_dataset PIK3CA-E545K-Sample PIK3CA-E545K.cram

#    RNASeq-chr22-SpliceJunctions
ngb reg_dataset GRCh38 RNASeq-chr22-SpliceJunctions
ngb reg_file GRCh38 /opt/data/ngb_demo_data/brain_th.bam?/opt/data/ngb_demo_data/brain_th.bam.bai
ngb add_dataset RNASeq-chr22-SpliceJunctions brain_th.bam


#    FGFR3-TACC-Fusion-Sample
ngb reg_dataset GRCh38 FGFR3-TACC-Fusion-Sample
ngb reg_file GRCh38 /opt/data/ngb_demo_data/FGFR3-TACC-Fusion.bam?/opt/data/ngb_demo_data/FGFR3-TACC-Fusion.bam.bai
ngb add_dataset FGFR3-TACC-Fusion-Sample FGFR3-TACC-Fusion.bam
ngb reg_file GRCh38 /opt/data/ngb_demo_data/FGFR3-TACC-Fusion.vcf
ngb add_dataset FGFR3-TACC-Fusion-Sample FGFR3-TACC-Fusion.vcf

#    Fruitfly
ngb reg_dataset DM6 Fruitfly
ngb reg_file DM6 /opt/data/ngb_demo_data/agnts3.09-28.trim.dm606.realign.bam?/opt/data/ngb_demo_data/agnts3.09-28.trim.dm606.realign.bai
ngb add_dataset Fruitfly agnts3.09-28.trim.dm606.realign.bam
ngb reg_file DM6 /opt/data/ngb_demo_data/agnX1.09-28.trim.dm606.realign.bam?/opt/data/ngb_demo_data/agnX1.09-28.trim.dm606.realign.bai
ngb add_dataset Fruitfly agnX1.09-28.trim.dm606.realign.bam
ngb reg_file DM6 /opt/data/ngb_demo_data/CantonS.09-28.trim.dm606.realign.bam?/opt/data/ngb_demo_data/CantonS.09-28.trim.dm606.realign.bai
ngb add_dataset Fruitfly CantonS.09-28.trim.dm606.realign.bam
ngb reg_file DM6 /opt/data/ngb_demo_data/agnts3.09-28.trim.dm606.realign.vcf
ngb add_dataset Fruitfly agnts3.09-28.trim.dm606.realign.vcf
ngb reg_file DM6 /opt/data/ngb_demo_data/agnX1.09-28.trim.dm606.realign.vcf
ngb add_dataset Fruitfly agnX1.09-28.trim.dm606.realign.vcf
ngb reg_file DM6 /opt/data/ngb_demo_data/CantonS.09-28.trim.dm606.realign.vcf
ngb add_dataset Fruitfly CantonS.09-28.trim.dm606.realign.vcf
