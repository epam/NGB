#!/bin/bash -e

#This script just downloads demo data from ngb.opensource.epam.com and register it

REFERENCES=~/ngb_files/references
DATA_FOLDER=~/ngb_files/data_files

while getopts "r:f:h" opt
do
  case $opt in
        r) REFERENCES=$OPTARG;;
        f) DATA_FOLDER=$OPTARG;;
        h) echo "-f - the folder with test files (default is ~/ngb_files/data_files)"
           echo "-r  - the folder to save references (default is  ~/ngb_files/references)" 
	   echo "-h - this help"
           exit 0 ;;
        *) echo "-h will help you, please look it up"
           exit 1;;
  esac
done

if [ ! -d $REFERENCES ]; then
  echo "The directory does not exist, so $REFERENCES folder will be created"
  mkdir -p $REFERENCES
fi

if [ ! -d $DATA_FOLDER ]; then
  echo "The directory does not exist, so $DATA_FOLDER folder will be created"
  mkdir -p $DATA_FOLDER
fi

echo "References will be downloaded to folder $REFERENCES"
echo "Demo data will be downloaded to folder $DATA_FOLDER"

#Downloading data
cd $REFERENCES

# Download grch38
wget --quiet --no-clobber http://ngb.opensource.epam.com/distr/data/genome/grch38/Homo_sapiens.GRCh38.gtf.gz  && \
wget --quiet --no-clobber http://ngb.opensource.epam.com/distr/data/genome/grch38/Homo_sapiens.GRCh38.fa.gz && \
wget --quiet --no-clobber http://ngb.opensource.epam.com/distr/data/genome/grch38/Homo_sapiens.GRCh38.domains.bed && \
gzip -d Homo_sapiens.GRCh38.fa.gz
gzip -d Homo_sapiens.GRCh38.gtf.gz

# Download grch37
wget --quiet --no-clobber http://ngb.opensource.epam.com/distr/data/genome/grch37/Homo_sapiens.GRCh37.gtf.gz  && \
wget --quiet --no-clobber http://ngb.opensource.epam.com/distr/data/genome/grch37/Homo_sapiens.GRCh37.fa.gz && \
gzip -d Homo_sapiens.GRCh37.fa.gz
gzip -d Homo_sapiens.GRCh38.gtf.gz

# Download dm6
wget --quiet --no-clobber http://ngb.opensource.epam.com/distr/data/genome/dm6/dmel-all-r6.06.sorted.gtf.gz && \
wget --quiet --no-clobber http://ngb.opensource.epam.com/distr/data/genome/dm6/dmel-all-chromosome-r6.06.fasta.gz && \
gzip -d dmel-all-chromosome-r6.06.fasta.gz
gzip -d dmel-all-r6.06.sorted.gtf.gz

# Download grcm38
wget --quiet  --no-clobber http://ngb.opensource.epam.com/distr/data/genome/mm/Mus_musculus.GRCm38.sorted.gtf.gz && \
wget --quiet  --no-clobber http://ngb.opensource.epam.com/distr/data/genome/mm/Mus_musculus.GRCm38.fa.gz && \
gzip -d Mus_musculus.GRCm38.fa.gz
gzip -d Mus_musculus.GRCm38.sorted.gtf.gz

# Download demo data
cd $DATA_FOLDER
wget --quiet  --no-clobber http://ngb.opensource.epam.com/distr/data/demo/ngb_demo_data.tar.gz && \
tar -zxvf ngb_demo_data.tar.gz

echo "Registering GRCh38 reference"
ngb reg_ref $REFERENCES/Homo_sapiens.GRCh38.fasta --name GRCh38 && \
ngb reg_file GRCh38 $REFERENCES/Homo_sapiens.GRCh38.gtf --name GRCh38_Genes && \
ngb ag GRCh38 GRCh38_Genes && \
ngb reg_file GRCh38 $REFERENCES/Homo_sapiens.GRCh38.domains.bed --name GRCh38_Domains && \
ngb an GRCh38 GRCh38_Domains && \
ngb rs Human GRCh38 && ngb add_spec "GRCh38" "GRCh38" && \
echo "GRCh38 reference and annotation were successfully registered" ||
echo "Something with GRCh38 reference has gone wrong, please check paths and filenames"

#Registering D.melanogaster reference
ngb reg_ref $REFERENCES/dmel-all-chromosome-r6.06.fasta --species DM6 --name DM6 && \
ngb reg_file DM6 $REFERENCES/dmel-all-r6.06.sorted.gtf --name DM6_Genes && \
ngb ag DM6 DM6_Genes && \
ngb rs Drosophila DM6 && ngb add_spec "DM6" "DM6" && \
echo "GRCh38 reference and annotation were successfully registered" ||
echo "Something with DM6 reference has gone wrong, please check paths and filenames"

#Registering GRC37, GRCm38 references
ngb reg_ref $REFERENCES/Homo_sapiens.GRCh37.fasta --species GRCh37 --name GRCh38 && \
ngb reg_file GRCh37 $REFERENCES/Homo_sapiens.GRCh37.gtf --name GRCh37_Genes && \
ngb ag GRCh37 GRCh37_Genes && \
ngb rs Human GRCh37 && ngb add_spec "GRCh37" "GRCh37" && \
echo "Success with GRCh37" || echo "GRCh37 fail"

ngb reg_ref $REFERENCES/Mus_musculus.GRCm38.fa --name GRCm38 --species GRCm38 && \
ngb reg_file GRCm38 $REFERENCES/Mus_musculus.GRCm38.sorted.gtf --name GRCm38_Genes && \
ngb ag GRCm38 GRCm38_Genes && \
ngb rs Human GRCm38 && ngb add_spec "GRCm38" "GRCm38" && \
echo "Success with GRCm38" || echo "GRCm38 fail"

#Now let's register datasets and separate files
cd $DATA_FOLDER/ngb_demo_data
ngb rd GRCh38 SV_Sample1
ngb add SV_Sample1 sample_1-lumpy.vcf
ngb add SV_Sample1 sv_sample_1.bw
ngb add SV_Sample1 sv_sample_1.bam
ngb reg_file GRCh38 sample_2-lumpy.vcf
ngb reg_file GRCh38 sv_sample_2.bw
ngb reg_file GRCh38 sv_sample_2.bam

ngb rd GRCh38 SV_Sample2
ngb add SV_Sample2 sample_2-lumpy.vcf
ngb add SV_Sample2 sv_sample_2.bw
ngb add SV_Sample2 sv_sample_2.bam

ngb reg_file GRCh38 PIK3CA-E545K.vcf
ngb reg_file GRCh38 PIK3CA-E545K.bam
ngb reg_file GRCh38 PIK3CA-E545K.cram?PIK3CA-E545K.cram.crai

ngb rd GRCh38 PIK3CA-E545K-Sample
ngb add PIK3CA-E545K-Sample PIK3CA-E545K.vcf
ngb add PIK3CA-E545K-Sample PIK3CA-E545K.bam
ngb add PIK3CA-E545K-Sample PIK3CA-E545K.cram
ngb reg_file GRCh38 brain_th.bam?brain_th.bam.bai

ngb rd GRCh38 RNASeq-chr22-SpliceJunctions
ngb add RNASeq-chr22-SpliceJunctions brain_th.bam

ngb reg_file GRCh38 FGFR3-TACC-Fusion.vcf
ngb reg_file GRCh38 FGFR3-TACC-Fusion.bam
ngb rd GRCh38 FGFR3-TACC-Fusion-Sample
ngb add FGFR3-TACC-Fusion-Sample FGFR3-TACC-Fusion.vcf
ngb add FGFR3-TACC-Fusion-Sample FGFR3-TACC-Fusion.bam

ngb reg_file DM6 agnX1.09-28.trim.dm606.realign.vcf
ngb reg_file DM6 agnX1.09-28.trim.dm606.realign.bam?agnX1.09-28.trim.dm606.realign.bai
ngb reg_file DM6 CantonS.09-28.trim.dm606.realign.vcf
ngb reg_file DM6 CantonS.09-28.trim.dm606.realign.bam?CantonS.09-28.trim.dm606.realign.bai
ngb reg_file DM6 agnts3.09-28.trim.dm606.realign.vcf
ngb reg_file DM6 agnts3.09-28.trim.dm606.realign.bam?agnts3.09-28.trim.dm606.realign.bai

ngb rd DM6 Fruitfly
ngb add Fruitfly agnX1.09-28.trim.dm606.realign.vcf
ngb add Fruitfly agnX1.09-28.trim.dm606.realign.bam
ngb add Fruitfly CantonS.09-28.trim.dm606.realign.vcf
ngb add Fruitfly CantonS.09-28.trim.dm606.realign.bam

#Remove downloaded raw archives with genomes
rm Homo_sapiens.GRCh38.fa.gz Homo_sapiens.GRCh37.fa.gz
rm dmel-all-chromosome-r6.06.fasta.gz dmel-all-r6.06.sorted.gtf.gz
rm Mus_musculus.GRCm38.fa.gz  Mus_musculus.GRCm38.sorted.gtf.gz

#Remove demo data archive
rm ngb_demo_data.tar.gz

exit 0
