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
  mkdir -p $REFERENCES && echo "$REFERENCES folder created successfully"
fi

if [ ! -d $DATA_FOLDER ]; then
  echo "The directory does not exist, so $DATA_FOLDER folder will be created"
  mkdir -p $DATA_FOLDER && echo "$DATA_FOLDER folder created successfully"
fi

echo "References will be downloaded to folder $REFERENCES"
echo "Demo data will be downloaded to folder $DATA_FOLDER"

#Downloading data
cd $REFERENCES

# Download grch38 fasta and annotation

if [ ! -e Homo_sapiens.GRCh38.fa ]; then
  echo "Downloading GRCh38 reference"
  wget --quiet --no-clobber http://ngb.opensource.epam.com/distr/data/genome/grch38/Homo_sapiens.GRCh38.fa.gz
  gunzip Homo_sapiens.GRCh38.fa.gz && echo "Successfully downloaded and gunzipped"
else
  echo "GRCh38 fasta has already been downloaded"
fi
echo

if [ ! -e Homo_sapiens.GRCh38.gtf ] && [ ! -e Homo_sapiens.GRCh38.domains.bed ]; then
  echo "Downloadiing annotation files for GRCh38"
  wget --quiet --no-clobber http://ngb.opensource.epam.com/distr/data/genome/grch38/Homo_sapiens.GRCh38.gtf.gz
  gunzip Homo_sapiens.GRCh38.gtf.gz && echo "Successfully downloaded and gunzipped"
  wget --quiet --no-clobber http://ngb.opensource.epam.com/distr/data/genome/grch38/Homo_sapiens.GRCh38.domains.bed
else
  echo "Annotation files have been already downloaded"
fi
echo

# Download grch37 fasta and annotation
if [ ! -e Homo_sapiens.GRCh37.fa ]; then
  echo "Downloading GRCh37 reference"
  wget --quiet --no-clobber http://ngb.opensource.epam.com/distr/data/genome/grch37/Homo_sapiens.GRCh37.fa.gz
  gunzip Homo_sapiens.GRCh37.fa.gz && echo "Successfully downloaded and gunzipped"
else
  echo "GRCh37 fasta has already been downloaded"
fi
echo

if [ ! -e Homo_sapiens.GRCh37.gtf.gz ]; then
  echo "Downloading GRCh37 annotation files"
  wget --quiet --no-clobber http://ngb.opensource.epam.com/distr/data/genome/grch37/Homo_sapiens.GRCh37.gtf.gz
  gunzip Homo_sapiens.GRCh37.gtf.gz && echo "Successfully downloaded and gunzipped"
else
  echo "GRCh37 annotation file has already been downloaded"
fi
echo

# Download grcm38
if [ ! -e Mus_musculus.GRCm38.fa ]; then
  echo "Downloading GRCm38 reference file"
  wget --quiet --no-clobber http://ngb.opensource.epam.com/distr/data/genome/mm/Mus_musculus.GRCm38.fa.gz
  gunzip Mus_musculus.GRCm38.fa.gz && echo "Successfully downloaded and gunzipped"
else
 echo "GRCm38 fasta has already been downloaded"
fi
echo

if [ ! -e Mus_musculus.GRCm38.sorted.gtf ]; then
  echo "Downloading GRCm38 annotation file"
  wget --quiet --no-clobber http://ngb.opensource.epam.com/distr/data/genome/mm/Mus_musculus.GRCm38.sorted.gtf.gz
  gunzip Mus_musculus.GRCm38.sorted.gtf.gz && echo "Successfully downloaded and gunzipped"
else
 echo "GRCm38 annotation has already been downloaded"
fi
echo

# Download dm6
if [ ! -e dmel-all-chromosome-r6.06.fasta ]; then
  echo "Downloading reference fasta for Drosophila"
  wget --quiet --no-clobber http://ngb.opensource.epam.com/distr/data/genome/dm6/dmel-all-chromosome-r6.06.fasta.gz
  gunzip dmel-all-chromosome-r6.06.fasta.gz && echo "Successfully downloaded and gunzipped"
else
 echo "Drosophila reference has already been downloaded"
fi
echo

if [ ! -e dmel-all-r6.06.sorted.gtf ]; then
  echo "Downloading annotation file for Drosophila"
  wget --quiet --no-clobber http://ngb.opensource.epam.com/distr/data/genome/dm6/dmel-all-r6.06.sorted.gtf.gz
  gunzip dmel-all-r6.06.sorted.gtf.gz && echo "Successfully downloaded and gunzipped"
else
 echo "Drosophila annotation has already been downloaded"
fi
echo

#Download demo data
cd $DATA_FOLDER
if  [ ! -d ngb_demo_files ]; then
  echo "Downloading demo files"
  wget --quiet --no-clobber http://ngb.opensource.epam.com/distr/data/demo/ngb_demo_data.tar.gz && \
  tar -zxvf ngb_demo_data.tar.gz && echo "Successfully downloaded and gunzipped"
else
  echo "Demo files seem to be already downloaded"
fi
echo


#Now let's register references one by one

#Registering the GRCh38 reference
cd $REFERENCES
echo "Registering GRCh38 reference"
ngb reg_spec "GRCh38" "GRCh38"
ngb reg_ref Homo_sapiens.GRCh38.fa --name "GRCh38" --genes Homo_sapiens.GRCh38.gtf --species "GRCh38" && \
echo "GRCh38 reference and genes file were successfully registered" || \
echo "Something with GRCh38 reference has gone wrong, please check paths and filenames"
echo

#Registering the GRCh38.domains.bed file as the annotation file
ngb add_ann "GRCh38" Homo_sapiens.GRCh38.domains.bed && \
echo "Annotation was successfully added to GRCh38 reference"

#Registering the GRCh37 reference
echo "Registering Human GRCh37 reference"
ngb reg_spec "GRCh37" "GRCh37"
ngb reg_ref Homo_sapiens.GRCh37.fa --name "GRCh37" --genes Homo_sapiens.GRCh37.gtf --species "GRCh37" && \
echo "Human GRCh37 reference and annotation were successfully registered" || \
echo "Something with GRCh37 reference has gone wrong, please check paths and filenames"
echo

#Registering the GRCm38 reference
echo "Registering Mouse GRCm38 reference"
ngb reg_spec "Mouse" "GRCm38"
ngb reg_ref Mus_musculus.GRCm38.fa --name "Mouse" --genes Mus_musculus.GRCm38.sorted.gtf --species "GRCm38" && \
echo "Mouse reference and annotation were successfully registered" || \
echo "Something with Mouse reference has gone wrong, please check paths and filenames"
echo

#Registering D.melanogaster reference
echo "Registering D.melanogaster reference"
ngb reg_spec "Drosophila" "DM6"
ngb reg_ref dmel-all-chromosome-r6.06.fasta --name "Drosophila" --genes dmel-all-r6.06.sorted.gtf --species "DM6" && \
echo "DM6 reference and annotation were successfully registered" || \
echo "Something with DM6 reference has gone wrong, please check paths and filenames"
echo

#Now let's register datasets and separate files
cd $DATA_FOLDER/ngb_demo_data
echo "Registering files for Drosophila"
ngb reg_file Drosophila agnX1.09-28.trim.dm606.realign.vcf
ngb reg_file Drosophila agnX1.09-28.trim.dm606.realign.bam?agnX1.09-28.trim.dm606.realign.bai
ngb reg_file Drosophila CantonS.09-28.trim.dm606.realign.vcf
ngb reg_file Drosophila CantonS.09-28.trim.dm606.realign.bam?CantonS.09-28.trim.dm606.realign.bai
ngb reg_file Drosophila agnts3.09-28.trim.dm606.realign.vcf
ngb reg_file Drosophila agnts3.09-28.trim.dm606.realign.bam?agnts3.09-28.trim.dm606.realign.bai
echo

echo "Registering Fruitfly dataset"
ngb rd Drosophila Fruitfly
ngb add Fruitfly agnX1.09-28.trim.dm606.realign.vcf
ngb add Fruitfly agnX1.09-28.trim.dm606.realign.bam
ngb add Fruitfly CantonS.09-28.trim.dm606.realign.vcf
ngb add Fruitfly CantonS.09-28.trim.dm606.realign.bam
echo

echo "Registering data for GRCh38 reference"

ngb rd GRCh38 SV_Sample1
ngb add SV_Sample1 sample_1-lumpy.vcf
ngb add SV_Sample1 sv_sample_1.bw
ngb add SV_Sample1 sv_sample_1.bam

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

echo "All data have been registered successfully"

exit 0
