# Command reference
## General commands
### Configure CLI connection to NGB server
```
ngb set_srv|srv [<NGB_API_URL>] [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```
*Description*

Sets NGB server API URL. By default CLI uses **localhost** to call API. If one need to use CLI on a remote machine - appropriate URL should be set.

URL should be specified using the following format: **http://{NGB_SERVER_NAME_OR_IP}:{NGB_SERVER_PORT}/catgenome**

When URL is set - it would be stored and used next time CLI is launched

*Example*
```
//Sets remote server for CLI
ngb set_srv http://10.248.33.51:8080/catgenome
```

## Search an object on a server
```
ngb search|s [<QUERY>] [options]

//Options:
//-l (--like)           If specified - search will be done using a substring. Be default strong equality comparison will be used
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```
*Description*

Searches for a specified string in reference and file names. By default, command will search for exactly equal name,  if -l option is specified - a search query will used as a substring.

*Example*
```
//Search for all objects, that contain "vcf" substring in their names and ouput result as a human-readable table
ngb search -l vcf -t

//Search file, named exactly "sample_1.bam" and ouput result as a json string
ngb search sample_1.bam
```

## Reference commands
### Register reference sequence
```
ngb reg_ref|rr [<PATH_TO_GENOME_FASTA>] [options]

//Options:
//-n (--name) [value]       Use specified value for reference sequence name. If not specified - fasta file name will be used. Should be unique
//-t (--table)              Print result as a human-readable table
//-j (--json)               Print result as a JSON string
//-g (--genes) [value]      Add a gene (gtf or gff) file to the reference. If file is already registered, it can be addressed by name or an identifier. Otherwise a path to the file should be provided.
//-ngc (--nogccontent)      Disables calculation of GC-content for large scale reference view
```
*Description*

Registers a specified reference sequence file. FASTA, FA, FNA files are accepted. GZip-compressed files are not supported.
Path to local file and remote URL are accepted as a path to the reference. For local files, NGB will try to find a matching "fai" index 
in the folder with the reference, if index isn't found it will be created. For remote references, "fai" index must be present on the
remote source. NGB assumes that reference index will have the same name as reference with "fai" extension added. If reference path is
**"/opt/genomes/hg38.fa"**, NGB will look for index file at path **"/opt/genomes/hg38.fa.fai"**.

*Example*
```
//Register reference, use "grch.38.fa" as name
ngb reg_ref /opt/genomes/grch.38.fa

//Register reference, use "grch38" as name
ngb rr /opt/genomes/grch.38.fa -n grch38
```
### List reference sequences
```
ngb list_ref|lr [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```
*Description*

Lists all reference sequences registered on the NGB server. The command output format is specified by
-t and -j options, default format is Json.

*Example*
```
//List all reference files from the server
ngb list_ref -t
```

### Delete reference sequence
```
ngb del_ref|dr [<REFERENCE_NAME|REFERENCE_ID>] [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```
*Description*

Deletes a specified reference sequence file from NGB server. Reference file can be addressed by name or an identifier (retrieved from **reg_ref** command, at registration time or search command)

*Example*
```
//Delete reference with name "grch38"
ngb del_ref grch38

//Delete reference with id 1
ngb dr 1
```

### Add gene file to the reference
```
ngb add_genes|ag [<REFERENCE_NAME|REFERENCE_ID>] [<FILE_NAME|FILE_ID|FILE_PATH>] [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```
*Description*

Adds a gene (GFF or GTF) file to the reference on NGB server. Reference file can be addressed by name or an identifier (retrieved from **reg_ref** command, at registration time or search command).
If gene file is already registered on NGb server, it can be addressed by name or by an identifier. Otherwise a path to the gene file should be provided.

*Example*
```
//Add a regitered gene file to the reference with name "grch38"
ngb add_genes grch38 genes.gtf

//Add a new gene file to the reference with name ID "1"
ngb ag 1 /opt/tracks/genes.gtf
```

### Remove gene file from the reference
```
ngb remove_genes|rg [<REFERENCE_NAME|REFERENCE_ID>] [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```
*Description*

Removes any gene file from the reference on NGB server. Reference file can be addressed by name or an identifier (retrieved from **reg_ref** command, at registration time or search command)

*Example*
```
//Remove gene file from the reference with name "grch38"
ngb remove_genes grch38

//Remove gene file from the reference with ID "1"
ngb rg 1
```


## File commands
### Register file
```
ngb reg_file|rf [<REFERENCE_NAME>|<REFERENCE_ID>] [<PATH_TO_NGS_FILE>] [options]

//Options:
//-n (--name)   [value]     Use specified value for file name. If not specified - filesystem name will be used. Should be unique
//-ni (--no_index)          Defines if a feature index should not be built during file registration (could be used to speed up registration process)
//-t (--table)              Print result as a human-readable table
//-j (--json)               Print result as a JSON string
```
*Description*

Registers a specified file. At least two arguments have to be specified:
Previously registered reference sequence file from NGB server. Reference file can be addressed by name or an identifier
Flesystem path foor the file to be registered. BAM, VCF, GFF, GTF, BED, SEG files are accepted. GZipped files are also accepted in a format <FILE_NAME>.<FILE_EXT>.gz, e.g.: my_variants.vcf.gz.
BAM file path has to be followed by a '?' symbol and a path to an index file (.BAI).

*Example*
```
//Register file, use "sample.vcf" as name for reference with id 18
ngb reg_file 18 /opt/tracks/sample.vcf

//Register file, use "sample" as name for reference with name grch38
ngb reg_file grch38 /opt/tracks/sample.vcf -n sample

//Register indexed file, use "sample.bam" as name
ngb reg_file grch38 /opt/tracks/sample.bam?/opt/tracks/sample.bam.bai
```

### Delete file
```
ngb del_file|df [<FILE_NAME>|<FILE_ID>] [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```
*Description*

Deletes a specified file from NGB server. File can be addressed by name or an identifier (retrieved from **reg_file** command, at registration time or **search** command)

*Example*
```
//Delete file with name "my_sample"
ngb del_file my_sample

//Delete file with id 18
ngb df 18
```

### Build feature index for a file
```
ngb index_file|if [<PATH_TO_NGS_FILE>] [options]

//Options:
//-t (--table)              Print result as a human-readable table
//-j (--json)               Print result as a JSON string
```
*Description*

Command builds a feature index for a specified file.
This can be useful to reindex a file or to create one if a file was registered with `--no_index` option.
Feature index is used for search and filtering capabilities for BED/VCF/GFF/GTF files.
File could be address by name or an identifier.

*Note: this command is used for internal NGB indexing - it could not be used to index BAM/CRAM files*

*Example*
```
//Build index for "sample.vcf" file 
ngb index_file sample.vcf

//Build index for a file with identifier "18"
ngb if 18
```

## Datasets commands
### Register dataset
```
ngb reg_dataset|rd [<REFERENCE_NAME>|<REFERENCE_ID>] [<DATASET_NAME>] [<files_names>|<files_ids>|<files_paths>] [options]

//Options:
//-p (--parent) [value] Specifies dataset parent for registration. Parent could addressed using a name or an indentifier
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```
*Description*

Registers a dataset. At least two arguments have to be specified:
* Previously registered reference sequence file from NGB server. Reference file can be addressed by name or an identifier
* Dataset name

Optionally a list of files to be added to a newly created dataset can be specified.
Also a parent dataset could be specified to build a dtaset hierarchy

Files can be addressed:
* For previously registered files (see **reg_file** command) - by name or an identifier
* For new files - by filesystem path (see **reg_file** command for a list of supported files)

*Note: both options can be used in one command*

*Example*
```
//Create new dataset with name "new_dataset" and use reference, named "grch38", do not add any files at the moment
ngb reg_dataset grch38 new_dataset

//Create new dataset with name "new_dataset" and use reference with id "1"
ngb rd 1 new_dataset

//Create new dataset with name "new_dataset" and reference "grch38", add two files
//(previously registered: "my_sample", "sample.vcf") to "new_dataset"
ngb rd grch38 new_dataset my_sample sample.vcf

//Create new dataset with name "new_dataset" and reference "grch38", register
//two files and add them to new_dataset
ngb rd grch38  new_dataset /opt/tracks/sample.vcf /opt/tracks/sample.bam /opt/tracks/sample.bam.bai
```

### Add file(-s) to dataset
```
ngb add_dataset|add [<DATASET_NAME>|<DATASET_ID>] [<FILES_NAMES>|<FILES_IDS>|<FILES_PATHS>] [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)               Print result as a JSON string
```
*Description*

Adds file (-s) to a specified dataset. Dataset can be addressed by name or by an identifier (retrieved from **reg_dataset** command, at registration time, or **search** command)

Files can be addressed:
* For previously registered files (see **reg_file** command) - by name or an identifier
* For new files - by filesystem path (see **reg_file** command for a list of supported files)

*Note: both options can be used in one command*

*Example*
```
//Add one file (named "my_sample") to dataset named "new_dataset"
ngb add_dataset new_dataset my_sample

//Add two files (named "my_sample" and "sample.vcf") to dataset named "new_dataset"
ngb add new_dataset my_sample sample.vcf

//Add three files (with identifiers: "1","2" and "3") to dataset with id "1"
ngb add 1 1 2 3
```

### Remove file from dataset
```
ngb remove_dataset|rmd [<DATASET_NAME>|<DATASET_ID>] [<FILE_NAME>|<FILE_ID>] [options]

//Options:
//-t (--table)              Print result as a human-readable table
//-j (--json)               Print result as a JSON string
```
*Description*

Removes a file from a specified dataset. Dataset can be addressed by name or by an identifier (retrieved from **reg_dataset** command, at registration time, or **search** command)

Only previously registered files (see **reg_file** command)  can be addressed. A file name or an identifier can be used.

*Example*
```
//Remove one file (named "my_sample") from dataset named "new_dataset"
ngb remove_dataset new_dataset my_sample

//Remove two files (named "my_sample" and "sample.vcf") from dataset named "new_dataset"
ngb rmd new_dataset my_sample sample.vcf

//Remove three files (with identifiers: "1","2" and "3") from dataset with id "1"
ngb rmd 1 1 2 3
```

### Move dataset (change dataset's hierarchy)
```
ngb move_dataset|md [<DATASET_NAME>|<DATASET_ID>] [options]

//Options:
//-p (--parent)         Change the dataset's parent to this value 
```
*Description*

Changes the dataset's hierarchy. Without options the command will move the specified dataset
to the top level od datasets' hierarchy (dataset's parent will be removed). If option -p (--parent)
is specified the dataset's parent will be changed to this option value.

*Example*
```
//Make dataset with ID 21 a top level dataset without a parent
ngb move_dataset 21

//Make dataset with name "data_parent" the parent dataset for a dataset with name "data_1" 
ngb md data_1 -p data_parent
```


### List datasets
```
ngb list_dataset|ld [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
//-p (--parent)         List dataset's hierarchy structure for a specified by this option root dataset
```
*Description*

Lists datasets registered on NGB server. By default the command will output only top-level
datasets without nested datasets. Dataset's hierarchy may be loaded with an option 'parent': if 
a parent dataset is set, the command will output the parent itself and all nested datasets. Parent
dataset may be addressed by name or ID.

*Example*
```
//List all top-level datasets from the server
ngb list_dataset

//List hierarchy tree for a dataset with the name "data_1"
ngb ld -p data_1
```


### Delete dataset
```
ngb del_dataset|dd [<DATASET_NAME>|<DATASET_ID>] [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```
*Description*

Delete a specified dataset from NGB server. Dataset could be addressed by name or by an identifier (retrieved from **reg_dataset** command, at registration time, or **search** command)

Files that were added to a dataset are not deleted by this command, as soon they might be used (now or later) in other datasets.

*Example*
```
//Delete dataset, named "new_dataset"
ngb del_dataset new_dataset

//Delete dataset with id "1"
ngb dd 1
```


### Generate URL for browsing selected files
```
ngb url [<DATASET_NAME>|<DATASET_ID>] [<FILE_IDS>|<FILE_NAMES>] [options]

//Options:
//-loc (--location) chr:startIndex-endIndex     Browse selected files on a specific location
//-loc (--location) chr                         Browse selected files on a specific chromosome
```
*Description*

Create an URL, that will open NGB browser with selected tracks, opened on a optionally specified position

*Example*
```

//Create URL for dataset with name 'data' and files with IDs 42, 45 and a file with name 'sample.vcf'
ngb url data 42 45 sample.vcf

//Create URL for dataset with ID '5' and a file with name 'sample.vcf' on a chromosome 1 on positions from 13476 to 23476
ngb url 5 sample.vcf -loc 1:13476-23476

//Create URL for dataset with name 'data' and a file with name 'sample.vcf' on a chromosome 1
ngb url data sample.vcf -loc 1
```
