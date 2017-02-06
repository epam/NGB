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

* SV_Sample1 dataset: [ALK-EML4 fusion](http://localhost:8080/catgenome#/GRCh38/2/29224570/29224993?rewrite=Off&tracks=%5B%7B%22b%22%3A1%2C%22p%22%3A1%2C%22h%22%3A20%2C%22s%22%3A%7B%7D%7D%2C%7B%22b%22%3A2%2C%22p%22%3A1%2C%22h%22%3A55%2C%22s%22%3A%7B%22g%22%3A%22collapsed%22%7D%7D%2C%7B%22b%22%3A7%2C%22p%22%3A1%2C%22h%22%3A42%2C%22s%22%3A%7B%22v%22%3A%22Collapsed%22%7D%7D%2C%7B%22b%22%3A10%2C%22p%22%3A1%2C%22h%22%3A462%2C%22s%22%3A%7B%22a%22%3Atrue%2C%22c%22%3A%22pairOrientation%22%2C%22c1%22%3Atrue%2C%22d%22%3Atrue%2C%22g1%22%3A%22default%22%2C%22i%22%3Atrue%2C%22m%22%3Atrue%2C%22r%22%3A1%2C%22s1%22%3Afalse%2C%22s2%22%3Atrue%2C%22s3%22%3Afalse%2C%22v1%22%3Afalse%7D%7D%5D)
    
* SV_Sample2 dataset: [ROS1-SLC34A2 fusion](http://localhost:8080/catgenome#/GRCh38/6/117336964/117337328?rewrite=Off&tracks=%5B%7B%22b%22%3A1%2C%22p%22%3A2%2C%22h%22%3A20%2C%22s%22%3A%7B%7D%7D%2C%7B%22b%22%3A2%2C%22p%22%3A2%2C%22h%22%3A122%2C%22s%22%3A%7B%22g%22%3A%22collapsed%22%7D%7D%2C%7B%22b%22%3A12%2C%22p%22%3A2%2C%22h%22%3A63%2C%22s%22%3A%7B%22v%22%3A%22Collapsed%22%7D%7D%2C%7B%22b%22%3A15%2C%22p%22%3A2%2C%22h%22%3A410%2C%22s%22%3A%7B%22a%22%3Atrue%2C%22c%22%3A%22insertSize%22%2C%22c1%22%3Atrue%2C%22d%22%3Atrue%2C%22g1%22%3A%22chromosomeOfMate%22%2C%22i%22%3Atrue%2C%22m%22%3Atrue%2C%22r%22%3A0%2C%22s1%22%3Afalse%2C%22s2%22%3Atrue%2C%22s3%22%3Afalse%2C%22v1%22%3Afalse%7D%7D%5D)

* FGFR3-TACC-Fusion-Sample dataset: [FGFR3-TACC3 fusion](http://localhost:8080/catgenome#/GRCh38/4/1727714/1729323?rewrite=Off&tracks=%5B%7B%22h%22%3A20%2C%22s%22%3A%7B%7D%2C%22b%22%3A1%2C%22p%22%3A5%7D%2C%7B%22h%22%3A53%2C%22s%22%3A%7B%22g%22%3A%22collapsed%22%7D%2C%22b%22%3A2%2C%22p%22%3A5%7D%2C%7B%22h%22%3A41%2C%22s%22%3A%7B%22v%22%3A%22Collapsed%22%7D%2C%22b%22%3A23%2C%22p%22%3A5%7D%2C%7B%22h%22%3A475%2C%22s%22%3A%7B%22a%22%3Atrue%2C%22c%22%3A%22pairOrientation%22%2C%22c1%22%3Atrue%2C%22d%22%3Atrue%2C%22g1%22%3A%22default%22%2C%22i%22%3Atrue%2C%22m%22%3Atrue%2C%22r%22%3A1%2C%22s1%22%3Afalse%2C%22s2%22%3Atrue%2C%22s3%22%3Afalse%2C%22v1%22%3Afalse%7D%2C%22b%22%3A25%2C%22p%22%3A5%7D%5D)

* PIK3CA-E545K-Sample dataset: [E545K SNV](http://localhost:8080/catgenome#/GRCh38/3/179218269/179218336?rewrite=Off&tracks=%5B%7B%22h%22%3A20%2C%22s%22%3A%7B%7D%2C%22b%22%3A1%2C%22p%22%3A3%7D%2C%7B%22h%22%3A54%2C%22s%22%3A%7B%22g%22%3A%22collapsed%22%7D%2C%22b%22%3A2%2C%22p%22%3A3%7D%2C%7B%22h%22%3A70%2C%22s%22%3A%7B%22v%22%3A%22Collapsed%22%7D%2C%22b%22%3A17%2C%22p%22%3A3%7D%2C%7B%22h%22%3A500%2C%22s%22%3A%7B%22a%22%3Atrue%2C%22c%22%3A%22noColor%22%2C%22c1%22%3Atrue%2C%22d%22%3Atrue%2C%22g1%22%3A%22default%22%2C%22i%22%3Atrue%2C%22m%22%3Atrue%2C%22r%22%3A%221%22%2C%22s1%22%3Afalse%2C%22s2%22%3Atrue%2C%22s3%22%3Afalse%2C%22v1%22%3Afalse%7D%2C%22b%22%3A19%2C%22p%22%3A3%7D%5D)

* Fruitfly dataset: [LIMK 1 SNV-INDELS](http://localhost:8080/catgenome#/DM6/X/12588906/12592411?rewrite=Off&tracks=%5B%7B%22h%22%3A20%2C%22s%22%3A%7B%7D%2C%22b%22%3A4%2C%22p%22%3A6%7D%2C%7B%22h%22%3A187%2C%22s%22%3A%7B%22v%22%3A%22Expanded%22%7D%2C%22b%22%3A35%2C%22p%22%3A6%7D%2C%7B%22h%22%3A482%2C%22s%22%3A%7B%22a%22%3Atrue%2C%22c%22%3A%22noColor%22%2C%22c1%22%3Atrue%2C%22d%22%3Atrue%2C%22g1%22%3A%22default%22%2C%22i%22%3Atrue%2C%22m%22%3Atrue%2C%22r%22%3A%221%22%2C%22s1%22%3Afalse%2C%22s2%22%3Atrue%2C%22s3%22%3Afalse%2C%22v1%22%3Afalse%7D%2C%22b%22%3A37%2C%22p%22%3A6%7D%5D)