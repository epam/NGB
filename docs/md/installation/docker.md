# Docker installation
## Image location
Docker image is available at [DockerHub Lifesciences repository](https://hub.docker.com/r/lifescience/ngb/)

## Versions
There are two version of NGB in the repository:

* **ngb:latest** - a "core" version - contains image of NGB without any data in it, only binaries
* **ngb:latest-demo** - a "demo" version - contains demo data set, which does not require any data registration, you need only to run an image

## Running demo version
Warning: a **demo** version could take up to 2Gb of the disk space (FASTA sequence, genes annotations, BAM, VCFs)
For a **demo** version run the following command
```
$ docker run -p 8080:8080 -d --name ngbcore lifescience/ngb:latest-demo
```  
You can go to [http://localhost:8080/catgenome](http://localhost:8080/catgenome) or [http://ip-of-the-host:8080/catgenome](http://ip-of-the-host:8080/catgenome) in a browser and view demo datasets (Sample 1 and Sample 2), which contain Structural Variations

## Running core image
For a **core** version replace <YOUR_NGS_DATA_FOLDER> placeholder with a real path to a folder with NGS data, and then run command
```
$ docker run -p 8080:8080 -d --name ngbcore -v <YOUR_NGS_DATA_FOLDER>:/ngs lifescience/ngb
```
This will create and start the container in a background mode and map port 8080 of the container to port 8080 of the host, then mount **<YOUR_NGS_DATA_FOLDER>** of the host to **/ngs** folder of the container and at last - make container accessible by name **ngbcore**

You can go to [http://localhost:8080/catgenome](http://localhost:8080/catgenome) or [http://ip-of-the-host:8080/catgenome](http://ip-of-the-host:8080/catgenome) in a browser (Chrome) and verify that server started successfully (you should see empty list of datasets)

## Registering data
To register your own data you should attach to a running container
```
$ docker exec -it ngbcore /bin/bash
```
This will put you inside a container's console and make **ngb** command available
First of all you should register reference (genome data), using a mounted folder **/ngs**. NGB accepts FASTA files for reference sequence
```
# ngb reg_ref /ngs/<PATH_TO_FASTA> -n my_genome -t
```

According to FASTA size you should wait several minutes.

To make NGS data available via NGB, you should create a **DATASET**, that is used to group linked files
You can register files and then add them to a dataset

Register file
```
# ngb reg_file my_genome /ngs/<PATH_TO_FILE> -n my_file1 -t
```

*Note that you should provide reference name (my_genome in this case), also **-n** (name) key is optional, if it is not specified - original file name will be used*

Create dataset and add file(s) to it
```
# ngb reg_dataset my_genome my_sample my_file1
```

Or you can create dataset and register files simultaneously
```
# ngb reg_dataset my_genome my_sample /ngs/<PATH_TO_FILE> /ngs/<PATH_TO_FILE2>
```

*Note that when registering a dataset, you should specify a genome name, to which files correspond*

After all you can leave container's console using
```
# exit
```

NGB container will continue running in a background.
When datasets are created - you can immediately browse NGS data.

## Demo data description
**ngb:latest-demo** container is built to show some basic features of NGB. It uses mostly shrinked data to minimize a container size

### Points of interest

* SV_Sample1 dataset: [ALK-EML4 fusion](http://localhost:8080/catgenome#/1/2/29224570/29224993?rewrite=Off&tracks=%5B%7B%22bioDataItemId%22%3A1%2C%22height%22%3A20%2C%22hidden%22%3Afalse%2C%22state%22%3A%7B%7D%7D%2C%7B%22bioDataItemId%22%3A3%2C%22hidden%22%3Afalse%2C%22height%22%3A100%2C%22state%22%3A%7B%22geneTranscript%22%3A%22collapsed%22%7D%7D%2C%7B%22bioDataItemId%22%3A8%2C%22hidden%22%3Afalse%2C%22height%22%3A47%2C%22state%22%3A%7B%22variantsView%22%3A%22Collapsed%22%7D%7D%2C%7B%22bioDataItemId%22%3A11%2C%22hidden%22%3Afalse%2C%22height%22%3A414%2C%22state%22%3A%7B%22arrows%22%3Atrue%2C%22colorMode%22%3A%22pairOrientation%22%2C%22coverage%22%3Atrue%2C%22diffBase%22%3Atrue%2C%22groupMode%22%3A%22default%22%2C%22ins_del%22%3Atrue%2C%22mismatches%22%3Atrue%2C%22readsViewMode%22%3A1%2C%22shadeByQuality%22%3Afalse%2C%22softClip%22%3Atrue%2C%22spliceJunctions%22%3Afalse%2C%22viewAsPairs%22%3Afalse%7D%7D%2C%7B%22bioDataItemId%22%3A9%2C%22hidden%22%3Atrue%7D%5D)
    
* SV_Sample2 dataset: [ROS1-SLC34A2 fusion](http://localhost:8080/catgenome#/2/6/117336964/117337328?rewrite=Off&tracks=%5B%7B%22bioDataItemId%22%3A1%2C%22height%22%3A20%2C%22hidden%22%3Afalse%7D%2C%7B%22bioDataItemId%22%3A3%2C%22height%22%3A100%2C%22hidden%22%3Afalse%7D%2C%7B%22bioDataItemId%22%3A13%2C%22height%22%3A70%2C%22hidden%22%3Afalse%7D%2C%7B%22bioDataItemId%22%3A16%2C%22height%22%3A437%2C%22hidden%22%3Afalse%2C%22state%22%3A%7B%22arrows%22%3Atrue%2C%22colorMode%22%3A%22insertSize%22%2C%22coverage%22%3Atrue%2C%22diffBase%22%3Atrue%2C%22groupMode%22%3A%22chromosomeOfMate%22%2C%22ins_del%22%3Atrue%2C%22mismatches%22%3Atrue%2C%22readsViewMode%22%3A0%2C%22shadeByQuality%22%3Afalse%2C%22softClip%22%3Atrue%2C%22spliceJunctions%22%3Afalse%2C%22viewAsPairs%22%3Afalse%7D%7D%2C%7B%22bioDataItemId%22%3A14%2C%22hidden%22%3Atrue%7D%5D)

* FGFR3-TACC-Fusion-Sample dataset: [FGFR3-TACC3 fusion](http://localhost:8080/catgenome#/5/4/1727714/1729323?rewrite=Off&tracks=%5B%7B%22height%22%3A20%2C%22hidden%22%3Afalse%2C%22bioDataItemId%22%3A1%7D%2C%7B%22height%22%3A57%2C%22hidden%22%3Afalse%2C%22bioDataItemId%22%3A3%7D%2C%7B%22height%22%3A70%2C%22hidden%22%3Afalse%2C%22bioDataItemId%22%3A24%7D%2C%7B%22height%22%3A422%2C%22hidden%22%3Afalse%2C%22bioDataItemId%22%3A26%2C%22state%22%3A%7B%22arrows%22%3Atrue%2C%22colorMode%22%3A%22pairOrientation%22%2C%22coverage%22%3Atrue%2C%22diffBase%22%3Atrue%2C%22groupMode%22%3A%22default%22%2C%22ins_del%22%3Atrue%2C%22mismatches%22%3Atrue%2C%22readsViewMode%22%3A1%2C%22shadeByQuality%22%3Afalse%2C%22softClip%22%3Atrue%2C%22spliceJunctions%22%3Afalse%2C%22viewAsPairs%22%3Afalse%7D%7D%5D)

* PIK3CA-E545K-Sample dataset: [E545K SNV](http://localhost:8080/catgenome#/3/3/179218270/179218335?rewrite=Off&tracks=%5B%7B%22bioDataItemId%22%3A1%2C%22height%22%3A20%2C%22hidden%22%3Afalse%2C%22state%22%3A%7B%7D%7D%2C%7B%22bioDataItemId%22%3A3%2C%22height%22%3A74%2C%22hidden%22%3Afalse%2C%22state%22%3A%7B%22geneTranscript%22%3A%22expanded%22%7D%7D%2C%7B%22bioDataItemId%22%3A40%2C%22height%22%3A87%2C%22hidden%22%3Afalse%2C%22state%22%3A%7B%22variantsView%22%3A%22Collapsed%22%7D%7D%2C%7B%22bioDataItemId%22%3A20%2C%22height%22%3A435%2C%22hidden%22%3Afalse%2C%22state%22%3A%7B%22arrows%22%3Atrue%2C%22colorMode%22%3A%22noColor%22%2C%22coverage%22%3Atrue%2C%22diffBase%22%3Atrue%2C%22groupMode%22%3A%22default%22%2C%22ins_del%22%3Atrue%2C%22mismatches%22%3Atrue%2C%22readsViewMode%22%3A%221%22%2C%22shadeByQuality%22%3Afalse%2C%22softClip%22%3Atrue%2C%22spliceJunctions%22%3Afalse%2C%22viewAsPairs%22%3Afalse%7D%7D%5D)

* Fruitfly dataset: [LIMK 1 SNV-INDELS](http://localhost:8080/catgenome#/6/X/12588807/12591202?rewrite=Off&tracks=%5B%7B%22height%22%3A20%2C%22hidden%22%3Afalse%2C%22bioDataItemId%22%3A4%2C%22state%22%3A%7B%7D%7D%2C%7B%22height%22%3A195%2C%22hidden%22%3Afalse%2C%22bioDataItemId%22%3A6%2C%22state%22%3A%7B%22geneTranscript%22%3A%22expanded%22%7D%7D%2C%7B%22height%22%3A115%2C%22hidden%22%3Afalse%2C%22bioDataItemId%22%3A32%2C%22state%22%3A%7B%22variantsView%22%3A%22Expanded%22%7D%7D%2C%7B%22height%22%3A284%2C%22hidden%22%3Afalse%2C%22bioDataItemId%22%3A34%2C%22state%22%3A%7B%22arrows%22%3Atrue%2C%22colorMode%22%3A%22noColor%22%2C%22coverage%22%3Atrue%2C%22diffBase%22%3Atrue%2C%22groupMode%22%3A%22default%22%2C%22ins_del%22%3Atrue%2C%22mismatches%22%3Atrue%2C%22readsViewMode%22%3A%221%22%2C%22shadeByQuality%22%3Afalse%2C%22softClip%22%3Atrue%2C%22spliceJunctions%22%3Afalse%2C%22viewAsPairs%22%3Afalse%7D%7D%2C%7B%22bioDataItemId%22%3A36%2C%22hidden%22%3Atrue%7D%2C%7B%22bioDataItemId%22%3A38%2C%22hidden%22%3Atrue%7D%2C%7B%22bioDataItemId%22%3A28%2C%22hidden%22%3Atrue%7D%2C%7B%22bioDataItemId%22%3A30%2C%22hidden%22%3Atrue%7D%2C%7B%22bioDataItemId%22%3A1%2C%22hidden%22%3Atrue%7D%2C%7B%22bioDataItemId%22%3A3%2C%22hidden%22%3Atrue%7D%2C%7B%22bioDataItemId%22%3A40%2C%22hidden%22%3Atrue%7D%2C%7B%22bioDataItemId%22%3A20%2C%22hidden%22%3Atrue%7D%5D)