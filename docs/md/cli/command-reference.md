# Command reference

- [General commands](#general-commands)
    - [Configure CLI connection to NGB server](#configure-cli-connection-to-ngb-server)
- [Display CLI version](#display-cli-version)
- [Search an object on a server](#search-an-object-on-a-server)
- [Reference commands](#reference-commands)
- [File commands](#file-commands)
- [Datasets commands](#datasets-commands)
- [Species commands](#species-commands)
- [Blast database commands](#blast-database-commands)
- [Heatmap commands](#heatmap-commands)
- [Lineage tree commands](#lineage-tree-commands)
- [Metabolic pathway commands](#metabolic-pathway-commands)
- [PDB file commands](#pdb-file-commands)
- [BAM coverage commands](#bam-coverage-commands)
- [Utility commands](#utility-commands)
- [Security commands](#security-commands)

## General commands

### Configure CLI connection to NGB server

#### Set NGB server URL

```
ngb set_srv|srv [<NGB_API_URL>] [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```

*Description*

Sets NGB server API URL. By default CLI uses **localhost** to call API. If one need to use CLI on a remote machine - appropriate URL should be set.

URL should be specified using the following format: **`http://{NGB_SERVER_NAME_OR_IP}:{NGB_SERVER_PORT}/catgenome`**

When URL is set - it would be stored and used next time CLI is launched.

*Example*

```bash
# Sets remote server for CLI
$ ngb set_srv http://10.248.33.51:8080/catgenome
```

#### Set authorization token

```
ngb set_token|st [<JWT_TOKEN>]
```

*Description*

Sets JWT token to authorize CLI requests to NGB server API. Required if authorization is enabled on NGB.

*Example*

```bash
# Sets remote server for CLI
$ ngb set_token eyJhbGciOiJSUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0X3VzZXIiLCJ1c2VyX
```

***

## Display CLI version

```
ngb version|v
```

*Description*

Prints CLI version to the console standard output.

***

## Search an object on a server

```
ngb search|s [<QUERY>] [options]

//Options:
//-l (--like)               If specified - search will be done using a substring. Be default strong equality comparison will be used
//-t (--table)              Print result as a human-readable table
//-j (--json)               Print result as a JSON string
//-perm (--permissions)     Print permissions on each found object (if they are set)
```

*Description*

Searches for a specified string in reference and file names. By default, command will search for exactly equal name,  if `-l` option is specified - a search query will used as a substring. If `-perm` option is specified - permissions on each found file for user(s)/group(s)/role(s) will be printed (if they are set).

*Example*

```bash
# Search for all objects, that contain "vcf" substring in their names and
# ouput result as a human-readable table
$ ngb search -l vcf -t

# Search file, named exactly "sample_1.bam" and ouput result as a json string
$ ngb search sample_1.bam

# Search for all objects, that contain "gene" substring in their names and
# output permissions on them:
$ ngb search -l gene -perm
```

***

## Reference commands

### Register reference sequence

```
ngb reg_ref|rr [<PATH_TO_GENOME_FASTA>] [options]

//Options:
//-n (--name) [value]       Use specified value for reference sequence name. If not specified - fasta file name will be used. Should be unique and mustn't be a number (it will be recognized as ID).
//-t (--table)              Print result as a human-readable table
//-j (--json)               Print result as a JSON string
//-g (--genes) [value]      Add a gene (gtf or gff) file to the reference. If file is already registered, it can be addressed by name or an identifier. Otherwise a path to the file should be provided.
//-ngc (--nogccontent)      Disables calculation of GC-content for large scale reference view
//-pt (--pretty)            Add pretty name to the reference genome
//-s (--species)            Add species version to registering reference. Note: species should be already registered on NGB server.
```

*Description*

Registers a specified reference sequence file. FASTA, FA, FNA, GENBANK, GBK, GB files are accepted. Compressed files are not supported.  
Path to local file and remote URL are accepted as a path to the reference:

- For local files, NGB will try to find a matching "fai" index in the folder with the reference, if index isn't found it will be created.
- For remote references, "fai" index must be presented on the remote source.
- NGB assumes that reference index will have the same name as reference with "fai" extension added. If reference path is **"/opt/genomes/hg38.fa"**, NGB will look for index file at path **"/opt/genomes/hg38.fa.fai"**.

*Example*

```bash
# Register reference, use "grch.38.fa" as name
$ ngb reg_ref /opt/genomes/grch.38.fa

# Register reference, use "grch38" as name
$ ngb rr /opt/genomes/grch.38.fa -n grch38
```

### List reference sequences

```
ngb list_ref|lr [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```

*Description*

Lists all reference sequences registered on the NGB server. The command output format is specified by `-t` and `-j` options, default format is Json.

*Example*

```bash
# List all reference files from the server
$ ngb list_ref -t
```

### Delete reference sequence

```
ngb del_ref|dr [<REFERENCE_NAME|REFERENCE_ID>] [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```

*Description*

Deletes a specified reference sequence file from NGB server. Reference file can be addressed by name or an identifier (retrieved from [**reg_ref**](#register-reference-sequence) command, at registration time or search command)

*Example*

```bash
# Delete reference with name "grch38"
$ ngb del_ref grch38

# Delete reference with id 1
$ ngb dr 1
```

### Rename reference

```
ngb rename_ref|rnr [<REFERENCE_NAME>] [options]

//Options:
//-n (--name)           Use specified value for new reference name.
//-pt (--pretty)        Use specified value for new reference pretty name.
```

*Description*

Renames specified reference sequence file.

*Example*

```bash
# Rename reference with name "grch38" and change pretty name
$ ngb rename_ref grch38 -n new_name -pt new_pretty_name
```

### Add gene file to the reference

```
ngb add_genes|ag [<REFERENCE_NAME|REFERENCE_ID>] [<FILE_NAME|FILE_ID|FILE_PATH>] [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```

*Description*

Adds a gene (GFF or GTF) file to the reference on NGB server. Reference file can be addressed by name or an identifier (retrieved from [**reg_ref**](#register-reference-sequence) command, at registration time or search command).
If gene file is already registered on NGB server, it can be addressed by name or by an identifier. Otherwise a path to the gene file should be provided.

*Example*

```bash
# Add a regitered gene file to the reference with name "grch38"
$ ngb add_genes grch38 genes.gtf

# Add a new gene file to the reference with name ID "1"
$ ngb ag 1 /opt/tracks/genes.gtf
```

### Remove gene file from the reference

```
ngb remove_genes|rg [<REFERENCE_NAME|REFERENCE_ID>] [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```

*Description*

Removes any gene file from the reference on NGB server. Reference file can be addressed by name or an identifier (retrieved from [**reg_ref**](#register-reference-sequence) command, at registration time or search command)

*Example*

```bash
# Remove gene file from the reference with name "grch38"
$ ngb remove_genes grch38

# Remove gene file from the reference with ID "1"
$ ngb rg 1
```

### Add annotation file to the reference

```
ngb add_ann|an [<REFERENCE_NAME|REFERENCE_ID>] [<FILE_NAMES|FILE_IDS|FILE_PATHS>] [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```

*Description*

Adds an annotation (GFF, GTF, BED, VCF) file to the reference on NGB server. Reference file can be addressed by name or an identifier (retrieved from [**reg_ref**](#register-reference-sequence) command, at registration time or search command).
If annotation file is already registered on NGB server, it can be addressed by name or by an identifier. Otherwise a path to the annotation file should be provided.

*Example*

```bash
# Add a registered gene file to the reference with name "grch38"
$ ngb add_ann grch38 annotation.gtf

# Add a new annotation file to the reference with name ID "1"
$ ngb an 1 /opt/tracks/annotation.gtf
```

### Remove annotation file from the reference

```
ngb remove_ann|ran [<REFERENCE_NAME|REFERENCE_ID>] [<FILE_NAMES|FILE_IDS|FILE_PATHS>] [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```
*Description*

Removes an annotation (GFF, GTF, BED, VCF) file from the reference on NGB server. Reference file can be addressed by name or an identifier (retrieved from [**reg_ref**](#register-reference-sequence) command, at registration time or search command).  
Annotation file can be addressed by name or by an identifier.

*Example*

```bash
# Remove an annotation file from the reference with name "grch38"
$ ngb remove_ann grch38 annotation.gtf
```

### Add species to the reference

```
ngb add_spec|as [<REFERENCE_NAME|REFERENCE_ID>] [<REGISTERED_SPECIES_VERSION>] [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```

*Description*

Adds a species version to the reference on NGB server. Reference file can be addressed by name or an identifier (retrieved from [**reg_ref**](#register-reference-sequence) command, at registration time or search command).

> **Note**: species with specified version should be already registered on NGB server.

*Example*

```bash
# Add a regitered species version to the reference with name "grch38"
$ ngb add_spec grch38 "hg19"
```

### Remove species from the reference

```
ngb remove_spec [<REFERENCE_NAME|REFERENCE_ID>] [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```

*Description*

Removes a species version from the reference on NGB server. Reference file can be addressed by name or an identifier (retrieved from [**reg_ref**](#register-reference-sequence) command, at registration time or search command).

*Example*

```bash
# Remove an annotation file from the reference with name "grch38"
$ ngb remove_spec grch38
```

***

## File commands

### Register file

```
ngb reg_file|rf [<REFERENCE_NAME>|<REFERENCE_ID>] [<PATH_TO_NGS_FILE>] [options]

//Options:
//-n (--name)   [value]     Use specified value for file name. If not specified - filesystem name will be used. Should be unique and mustn't be a number (it will be recognized as ID).
//-ni (--no_index)          Defines if a feature index should not be built during file registration (could be used to speed up registration process)
//-t (--table)              Print result as a human-readable table
//-j (--json)               Print result as a JSON string
//-pt (--pretty)            Add pretty name to the file
```

*Description*

Registers a specified file. At least two arguments have to be specified:

- Previously registered reference sequence file from NGB server. Reference file can be addressed by name or an identifier.
- Filesystem path to the file to be registered.

BAM, VCF, GFF, GTF, BED, SEG, WIG, BED GRAPH files are accepted.  
BGZipped files are also accepted in a format `<FILE_NAME>.<FILE_EXT>.gz`, e.g.: `my_variants.vcf.gz` (**bgzip** tool is available as a part of [htslib](http://www.htslib.org/) package. Or NGB CLI [**sort**](#sort-feature-file) command can be used for that as well).  
BAM file path must be followed by a `?` symbol and a path to an index file (.BAI) (if a folder with BAM file also contains index for this BAM with the same name, CLI will find this index automatically. It also works well for VCF, BED and gene files).  
If and only if CLI located on the same filesystem with NGB server relative path can be used.

To register files from the external cloud data storages (e.g. AWS S3), file path must be in the full view (starting from `s3://`, then the bucket name, folder name and so on slash-separated, ending with `<FILE_NAME>.<FILE_EXT>`), e.g.: `s3://ngb-s3/fruitfly/agnX1.09-28.trim.dm606.realign.vcf`. In case with AWS S3 storages, path to the files with indexes (BAM, VCF, BED, genes files) must be strongly followed by a `?` symbol and a path to their index files. CLI will not find such indexes automatically.

> **Note**: for ability to work with files from AWS S3, do not forget to configure your NGB instance before start (see *"Configure for working with AWS S3"* paragraph [here](../installation/standalone.md)).

*Example*

```bash
# Register file, use "sample.vcf" as a name for reference with id 18
$ ngb reg_file 18 /opt/tracks/sample.vcf

# Register file, use "sample" as a name for reference with the name grch38
$ ngb reg_file grch38 /opt/tracks/sample.vcf -n sample

# Register indexed file, use "sample.bam" as a name
$ ngb reg_file grch38 /opt/tracks/sample.bam?/opt/tracks/sample.bam.bai

# Register indexed file, use "sample.bam" as a name, index for the BAM file is contained in the same directory as the BAM file
$ ngb reg_file grch38 /opt/tracks/sample.bam

# Register file with relative path
$ ngb reg_file hg19 ../tracks/sample.vcf

# Register file from AWS S3, use "sample1" as a name
$ ngb reg_file grch38 s3://ngb-s3/human/grch38_tracks/sample1.vcf -n sample1

# Register indexed file from AWS S3, use "sample1.bam" as a name
$ ngb reg_file grch38 s3://ngb-s3/human/grch38_tracks/sample1.bam?s3://ngb-s3/human/grch38_tracks/sample1.bam.bai --name sample1.bam
```

### Delete file

```
ngb del_file|df [<FILE_NAME>|<FILE_ID>] [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```

*Description*

Deletes a specified file from NGB server. File can be addressed by name or an identifier (retrieved from [**reg_file**](#register-file) command, at registration time or [**search**](#search-an-object-on-a-server) command).

> **Note**: file couldn't be deleted, if it is added to any dataset. For deleting such file first remove it from all datasets. 

*Example*

```bash
# Delete file with name "my_sample"
$ ngb del_file my_sample

# Delete file with id 18
$ ngb df 18
```

### Rename file

```
ngb rename_file|rnf [<FILE_NAME>] [options]

//Options:
//-n (--name)           Use specified value for new file name.
//-pt (--pretty)        Use specified value for new file pretty name.
```

*Description*

Renames specified file.

*Example*

```bash
# Rename file with name "my_sample" and change pretty name
$ ngb rename_file my_sample -n new_name -pt new_pretty_name
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

> **Note**: this command is used for internal NGB indexing - it could not be used to index BAM/CRAM files

*Example*

```bash
# Build index for "sample.vcf" file 
$ ngb index_file sample.vcf

# Build index for a file with identifier "18"
$ ngb if 18
```

***

## Datasets commands

### Register dataset

```
ngb reg_dataset|rd [<REFERENCE_NAME>|<REFERENCE_ID>] [<DATASET_NAME>] [<files_names>|<files_ids>|<files_paths>] [options]

//Options:
//-p (--parent) [value] Specifies dataset parent for registration. Parent could addressed using a name or an identifier
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
//-pt (--pretty)        Add pretty name to the dataset
```

*Description*

Registers a dataset. At least two arguments have to be specified:

- Previously registered reference sequence file from NGB server. Reference file can be addressed by name or an identifier
- Dataset name

Optionally a list of files to be added to a newly created dataset can be specified.  
Also a parent dataset could be specified to build a dataset hierarchy.

Files can be addressed:

- For previously registered files (see [**reg_file**](#register-file) command) - by name or an identifier
- For new files - by filesystem path (see [**reg_file**](#register-file) command for a list of supported files)

> **Note**: both options can be used in one command

*Example*

```bash
# Create new dataset with name "new_dataset" and use reference, named "grch38",
# do not add any files at the moment
$ ngb reg_dataset grch38 new_dataset

# Create new dataset with name "new_dataset" and use reference with id "1"
$ ngb rd 1 new_dataset

# Create new dataset with name "new_dataset" and reference "grch38",
# add two files (previously registered: "my_sample", "sample.vcf") to "new_dataset"
$ ngb rd grch38 new_dataset my_sample sample.vcf

# Create new dataset with name "new_dataset" and reference "grch38",
# register two files and add them to new_dataset
$ ngb rd grch38 new_dataset /opt/tracks/sample.vcf /opt/tracks/sample.bam /opt/tracks/sample.bam.bai
```

### Add file(-s) to dataset

```
ngb add_dataset|add [<DATASET_NAME>|<DATASET_ID>] [<FILES_NAMES>|<FILES_IDS>|<FILES_PATHS>] [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)               Print result as a JSON string
```

*Description*

Adds file (-s) to a specified dataset. Dataset can be addressed by name or by an identifier (retrieved from [**reg_dataset**](#register-dataset) command, at registration time, or [**search**](#search-an-object-on-a-server) command)

Files can be addressed:

- For previously registered files (see [**reg_file**](#register-file) command) - by name or an identifier
- For new files - by filesystem path (see [**reg_file**](#register-file) command for a list of supported files)

> **Note**: both options can be used in one command

*Example*

```bash
# Add one file (named "my_sample") to dataset named "new_dataset"
$ ngb add_dataset new_dataset my_sample

# Add two files (named "my_sample" and "sample.vcf") to dataset named "new_dataset"
$ ngb add new_dataset my_sample sample.vcf

# Add three files (with identifiers: "1","2" and "3") to dataset with id "1"
$ ngb add 1 1 2 3
```

### Remove file from dataset

```
ngb remove_dataset|rmd [<DATASET_NAME>|<DATASET_ID>] [<FILE_NAME>|<FILE_ID>] [options]

//Options:
//-t (--table)              Print result as a human-readable table
//-j (--json)               Print result as a JSON string
```

*Description*

Removes a file from a specified dataset. Dataset can be addressed by name or by an identifier (retrieved from [**reg_dataset**](#register-dataset) command, at registration time, or [**search**](#search-an-object-on-a-server) command).

Only previously registered files (see [**reg_file**](#register-file) command) can be addressed. A file name or an identifier can be used.

*Example*

```bash
# Remove one file (named "my_sample") from dataset named "new_dataset"
$ ngb remove_dataset new_dataset my_sample

# Remove two files (named "my_sample" and "sample.vcf") from dataset named "new_dataset"
$ ngb rmd new_dataset my_sample sample.vcf

# Remove three files (with identifiers: "1","2" and "3") from dataset with id "1"
$ ngb rmd 1 1 2 3
```

### Move dataset (change dataset's hierarchy)

```
ngb move_dataset|md [<DATASET_NAME>|<DATASET_ID>] [options]

//Options:
//-p (--parent)         Change the dataset's parent to this value 
```

*Description*

Changes the dataset's hierarchy. Without options the command will move the specified dataset to the top level of datasets' hierarchy (dataset's parent will be removed). If option `-p` is specified the dataset's parent will be changed to this option value.

*Example*

```bash
# Make dataset with ID 21 a top level dataset without a parent
$ ngb move_dataset 21

# Make dataset with name "data_parent" the parent dataset for a dataset with name "data_1" 
$ ngb md data_1 -p data_parent
```

### List datasets

```
ngb list_dataset|ld [options]

//Options:
//-t (--table)              Print result as a human-readable table
//-j (--json)               Print result as a JSON string
//-p (--parent)             List dataset's hierarchy structure for a specified by this option root dataset
//-perm (--permissions)     Print permissions on each dataset (if they are set)
```

*Description*

Lists datasets registered on NGB server. By default the command will output only top-level datasets without nested datasets. Dataset's hierarchy may be loaded with an option `--parent`: if a parent dataset is set, the command will output the parent itself and all nested datasets. Parent dataset may be addressed by name or ID. If `-perm` option is specified - permissions on each dataset for user(s)/group(s)/role(s) will be printed (if that permissions are set).

*Example*

```bash
# List all top-level datasets from the server with permissions on them
$ ngb list_dataset -perm

# List hierarchy tree for a dataset with the name "data_1"
$ ngb ld -p data_1
```

### Delete dataset
```
ngb del_dataset|dd [<DATASET_NAME>|<DATASET_ID>] [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
//-f (--force)          Allow to delete project with nested projects
```
*Description*

Deletes a specified dataset from NGB server. Dataset could be addressed by name or by an identifier (retrieved from [**reg_dataset**](#register-dataset) command, at registration time, or [**search**](#search-an-object-on-a-server) command)

Files that were added to a dataset are not deleted by this command, as soon they might be used (now or later) in other datasets.

*Example*

```bash
# Delete dataset, named "new_dataset"
$ ngb del_dataset new_dataset

# Delete dataset with id "1"
$ ngb dd 1
```

### Rename dataset

```
ngb rename_dataset|rnd [<DATASET_NAME>] [options]

//Options:
//-n (--name)           Use specified value for new dataset name.
//-pt (--pretty)        Use specified value for new dataset pretty name.
```

*Description*

Renames specified dataset.

*Example*

```bash
# Rename dataset with name "dataset" and change pretty name
$ ngb rename_dataset dataset -n new_dataset -pt new_pretty_name
```

### Add dataset description

```
ngb add_description [<DATASET_NAME>|<DATASET_ID>] <DESCRIPTION_FILE_PATH>  [options]

//Options:
//-n (--name)           Use specified value for description name. If not specified - fasta file name will be used. Should be unique and mustn't be a number (it will be recognized as ID).
//--send-content        Use to read description file and send as binary data. Otherwise only the file path will be sent.
```

*Description*

Attaches a html file to a dataset. Dataset could be addressed by name or by an identifier (retrieved from [**reg_dataset**](#register-dataset) command, at registration time, or [**search**](#search-an-object-on-a-server) command)

*Example*

```bash
# Add html description to a dataset, named "new_dataset"
$ ngb add_description new_dataset /opt/data/report.html --name report
```

### Remove dataset description

```
ngb remove_description [<DATASET_NAME>|<DATASET_ID>]  [options]

//Options:
//-n (--name)           Delete only dataset description specified by name
```

*Description*

Removes description from a dataset. If `--name` option is provided, only selected description is deleted, otherwise all existing descriptions are removed.

*Example*

```bash
# Remove all descption files from a dataset, named "new_dataset"
$ ngb remove_description new_dataset

# Remove description with name "report" from a dataset, named "new_dataset"
$ ngb remove_description new_dataset --name report
```

### Generate URL for browsing selected files

```
ngb url [<DATASET_NAME>|<DATASET_ID>] [<FILE_IDS>|<FILE_NAMES>] [options]

//Options:
//-loc (--location) chr:startIndex-endIndex     Browse selected files on a specific location
//-loc (--location) chr                         Browse selected files on a specific chromosome
//--alias                                       Allows to get short share link
```

*Description*

Creates an URL, that will open NGB browser with selected tracks, opened on an optionally specified position.

*Example*

```bash
# Create URL for dataset with name 'data' and files with IDs 42, 45 and a file with name 'sample.vcf'
$ ngb url data 42 45 sample.vcf

# Create URL for dataset with ID '5' and a file with name 'sample.vcf'
# on a chromosome 1 on positions from 13476 to 23476
$ ngb url 5 sample.vcf -loc 1:13476-23476

# Create URL for dataset with name 'data' and a file with name 'sample.vcf'
# on a chromosome 1
$ ngb url data sample.vcf -loc 1
```

***

## Species commands

### Register species

```
ngb reg_spec|rs [<SPECIES_NAME>] [<SPECIES_VERSION>] [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```

*Description*

Registers a species. Two arguments have to be specified:

- Species name
- Species version

> **Note**: species version should be unique. During registration a species with already registered version a proper exception will be thrown.

*Example*

```bash
# Create new species with name "human" and version "hg19"
$ ngb reg_spec "human" "hg19"
```

### List species

```
ngb list_spec [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```

*Description*

List all species registered on NGB server.

*Example*

```bash
# List all species registered on NGB server
$ ngb list_spec
```

### Delete species

```
ngb del_spec|ds [<SPECIES_VESRSION>] [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```

*Description*

Deletes a specified species from NGB server. Species is be addressed by version.

*Example*

```bash
# Delete species with version "hg19"
$ ngb del_spec "hg19"
```

***

## Blast database commands

### Register database

```
ngb reg_blast_db|rbd [<NAME>] [<PATH>] [<TYPE>] [<SOURCE>]
```

*Description*

Registers a database. Four arguments have to be specified:

- Database name
- Database path
- Database type
- Database source

> **Note**: type should be "PROTEIN" or "NUCLEOTIDE". Source should be "NCBI" or "CUSTOM".

*Example*

```bash
# Create new database with name "Homo_sapiens", path "Homo_sapiens.GRCh38", type "NUCLEOTIDE" and "CUSTOM" source
$ ngb reg_blast_db "Homo_sapiens" "Homo_sapiens.GRCh38" "NUCLEOTIDE" "CUSTOM"
```

### List databases

```
ngb list_blast_db|lbd [options]

//Options:
//-dt (--db-type)       Show databases with specified type
//-dp (--db-path)       Show databases with specified path
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```

> **Note**: type should be "PROTEIN" or "NUCLEOTIDE".

*Description*

List all databases registered on NGB server.  
One argument can be specified:

- Database type

*Example*

```bash
# List all "PROTEIN" databases registered on NGB server
$ ngb list_blast_db -dt "PROTEIN"
```

### Delete database

```
ngb del_blast_db|dbd [<ID>]
```

*Description*

Deletes a specified database from NGB server.

*Example*

```bash
# Delete database with id 2
$ ngb del_blast_db 2
```

***

## Heatmap commands

### Register heatmap

```
ngb reg_heatmap|rh [<PATH>] [options]
//Options:
//-n (--name)                   Heatmap name
//-pt (--pretty)                Heatmap pretty name
//-cap (--heatmap-cap)          Heatmap cell annotation path
//-cell-at (--heatmap-cell-at)  Heatmap cell annotation type [REFERENCE|GENE|DATASET|COORDINATE]
//-lap (--heatmap-lap)          Heatmap label annotation path
//-skip-row (--skip-row)        Number of last heatmap rows to skip
//-skip-col (--skip-col)        Number of last heatmap columns to skip
//-row-at (--heatmap-row-at)    Heatmap row annotation type [REFERENCE|GENE|DATASET|COORDINATE]
//-col-at (--heatmap-col-at)    Heatmap column annotation type [REFERENCE|GENE|DATASET|COORDINATE]
```

*Description*

Registers a heatmap. One argument has to be specified:

- Heatmap path

Heatmap suport navigation from annotation labels to specific locations.  
Type of annotation maybe specified during heatmap registration for cell, row and column labels.  
If annotation type is not specified navigation isn't enabled for a heatmap.  
Four annotation types are supported:

- REFERENCE - labels match reference name
- GENE - labels match gene name: `KRAS` (navigate to gene in current reference) or `GRCh38:ROS1` (navigate to gene in `GRCh38` reference)
- DATASET - labels match dataset name
- COORDINATE - labels contain genomic coordinates, e.g. `chr1:234-1566` or `GRCh38:chr1:234-1566`

*Example*

```bash
# Create new heatmap with name "Heatmap", path "heatmap.csv"
$ ngb reg_heatmap "heatmap.csv" -n "Heatmap" -lap  "/opt/heatmap-labels.tsv" -row-at GENE
```

### List heatmaps

```
ngb list_heatmaps|lh
```

*Description*

List all heatmaps registered on NGB server.

*Example*

```bash
# List all heatmaps registered on NGB server
$ ngb list_heatmaps
```

### Delete heatmap

```
ngb del_heatmap|dh [<ID>]
```

*Description*

Deletes a specified heatmap from NGB server. One argument has to be specified:

- Heatmap id

*Example*

```bash
# Delete heatmap with id 1
$ ngb del_heatmap 1
```

### Update heatmap cell annotation

```
ngb upd_cell_annotation|uca [<ID>] [options]
//Options:
//-path          Heatmap cell annotation path
```

> **Note**: annotation will be deleted if `-path` option is absent.

*Description*

Updates heatmap cell annotation. One argument has to be specified:

- Heatmap id

*Example*

```bash
# Update cell annotation for heatmap id 1, annotation path "cell_annotation.csv"
$ ngb upd_cell_annotation 1 -path "cell_annotation.csv"
```

### Update heatmap label annotation

```
ngb upd_label_annotation|ula [<ID>] [options]
//Options:
//-path          Heatmap label annotation path
```

> **Note**: annotation will be deleted if "-path" option is absent.

*Description*

Updates heatmap label annotation. One argument has to be specified:

- Heatmap id

*Example*

```bash
# Update label annotation for heatmap id 1, annotation path "label_annotation.csv"
$ ngb upd_label_annotation 1 -path "label_annotation.csv"
```

### Update heatmap row tree

```
ngb upd_row_tree|urt [<ID>] [options]
//Options:
//-path          Heatmap row tree path
```

> **Note**: row tree will be deleted if "-path" option is absent.

*Description*

Updates heatmap row tree. One argument has to be specified:

- Heatmap id

*Example*

```bash
# Update row tree for heatmap id 1, row tree path "row_tree.csv"
$ ngb upd_row_tree 1 -path "row_tree.csv"
```

### Update heatmap column tree

```
ngb upd_column_tree|urt [<ID>] [options]
//Options:
//-path          Heatmap column tree path
```

> **Note**: column tree will be deleted if `-path` option is absent.

*Description*

Updates heatmap column tree. One argument has to be specified:

- Heatmap id

*Example*

```bash
# Update column tree for heatmap id 1, column tree path "column_tree.csv"
$ ngb upd_column_tree 1 -path "column_tree.csv"
```

***

## Lineage tree commands

### Register lineage tree

```
ngb reg_lineage|rl [<NODES_PATH>] [<EDGES_PATH>] [options]
//Options:
//-n (--name)                   Lineage tree name
//-pt (--pretty)                Lineage tree pretty name
```

*Description*

Registers a lineage tree. Two arguments have to be specified:

- Nodes file path
- Edges file path

*Example*

```bash
# Create new lineage tree with nodes path "nodes.txt", edges path "edges.txt"
$ ngb reg_lineage "nodes.txt" "edges.txt"
```

### List lineage trees

```
ngb list_lineage|ll
```

*Description*

List all lineage trees registered on NGB server.

*Example*

```bash
# List all lineage trees registered on NGB server
$ ngb list_lineage
```

### Delete lineage tree

```
ngb del_lineage|dl [<ID>]
```

*Description*

Deletes a specified lineage tree from NGB server.

*Example*

```bash
# Delete lineage tree with id 2
$ ngb del_lineage 2
```

***

## Metabolic pathway commands

### Register pathway

```
ngb reg_pathway|rp [<PATH>] [options]
//Options:
//-n (--name)                   Pathway name
//-pt (--pretty)                Pathway pretty name
//-desc (--description)         Pathway description
//--taxids                      List of taxonomy IDs, separated by comma
```

*Description*

Registers a Pathway. One argument has to be specified:

- Pathway file path

*Example*

```bash
# Create new pathway with path "pathway.sbgn"
$ ngb reg_pathway "pathway.sbgn"
```

### Register BioPAX

```
ngb reg_biopax|rb [<PATH>] [options]
//Options:
//--taxids                      List of taxonomy IDs, separated by comma
```

*Description*

Registers pathways in BioPAX format. One argument has to be specified:

- BioPAX file path

*Example*

```bash
# Create new pathways from bioPAX file with path "biopax.owl"
$ ngb reg_biopax "biopax.owl"
```

### List metabolic pathways

```
ngb list_pathway|lp
```

*Description*

List all pathways registered on NGB server.

*Example*

```bash
# List all pathways registered on NGB server
$ ngb list_pathway
```

### Delete metabolic pathway

```
ngb del_pathway|dp [<ID>]
```

*Description*

Deletes a specified pathway from NGB server.

*Example*

```bash
# Delete pathway with id 2
$ ngb del_pathway 2
```

***

## PDB file commands

### Register PDB file

```
ngb reg_pdb|rpdb [<GENE_ID>][<PATH>] [options]
//Options:
//-n (--name)                   PDB file name
//-pt (--pretty)                PDB file pretty name
//-mtd (--metadata)             PDB file metadata (string in json format)
```

*Description*

Registers a PDB file. Two arguments have to be specified:

- Ensemble gene Id
- PDB file path

*Example*

```bash
# Create new PDB file with path "pdb.cif"
$ ngb reg_pdb "GENE_ID" "pdb.cif"
```

### List PDB files

```
ngb list_pdb|lpdb
```

*Description*

List all PDB files registered on NGB server.

*Example*

```bash
# List all PDB files registered on NGB server
$ ngb list_pdb
```

### Delete PDB file

```
ngb del_pdb|dpdb [<ID>]
```

*Description*

Deletes a specified PDB file from NGB server.

*Example*

```bash
# Delete PDB file with id 2
$ ngb del_pdb 2
```

## Target commands
### Register target
```
ngb reg_target|rt [<NAME>] [<PRODUCTS>] [<DISEASES>] [<TARGET_GENES_PATH>] [<TYPE>]

```
*Description*

Registers a target. Five arguments have to be specified:
* Target name
* Target products
* Target diseases
* Target genes path
* Target type

Products and diseases should be specified as a list of items, separated by comma.
Target genes should be specified in CSV format:
```bash
geneId,geneName,taxId,speciesName,priority
ENSG00000133703,KRAS,9606,Homo Sapiens,0
```

*Example*
```bash
# Create new target with name "Target", 
# products "Product1" and "Product2", 
# diseases "Disease1" and "Disease2" 
# and genes file path "genes.csv"
# and type DEFAULT
$ ngb reg_target "Target" "Product1,Product2" "Disease1,Disease2" "genes.csv" "DEFAULT"
```

### List targets
```
ngb list_targets|lt
```

*Description*

List all targets registered on NGB server.

*Example*
```bash
# List all targets registered on NGB server
$ ngb list_targets
```

### Delete target
```
ngb del_target|dt [<ID>]
```
*Description*

Deletes a specified target from NGB server. One argument has to be specified:
* target id

*Example*
```bash
# Delete target with id 1
$ ngb del_target 1
```

***

## BAM coverage commands

### Register coverage

```
ngb add_coverage|ac [<BAM_ID>] [<STEP>]
```

*Description*

Registers a Bam coverage. Two arguments have to be specified:

- BAM file id or name
- Step - length of the interval in base pairs. Coverage index will be calculated according to chromosome breakdown by this intervals

*Example*

```bash
# Create new coverage for Bam file with id 2, step 100
$ ngb add_coverage 2 100
```

### List coverages

```
ngb list_coverage|lc
```

*Description*

List all coverages registered on NGB server.

*Example*

```bash
# List all coverages registered on NGB server
$ ngb list_coverage
```

### Delete Bam file coverage

```
ngb remove_coverage|rc [<BAM_ID>] [options]
//Options:
//--step                   Step
```

*Description*

Deletes coverages for specified Bam file from NGB server.

*Example*

```bash
# Delete coverage with step 100 for Bam file with id 2
$ ngb remove_coverage 2 --step 100
```

***

## Utility commands

### Sort feature file

```
ngb sort [<ORIGINAL_FILE_PATH>] [<SORTED_FILE_PATH>] [options]

//Options:
//-m (--max_memory) [value] Specifies amount of memory in megabytes to use for sorting (default: 500). Since memory usage estimation is approximate, real memory usage may vary to some extend.
```

*Description*

Sorts given feature file. At least one argument should be specified:

- Path to a feature file to be sorted.

VCF, BED, GTF, GFF, GFF3 formats are supported. Uncompressed and BGZip-compressed files are supported.

Optional argument is:

- Target path to store a sorted feature file.

If this argument is not specified, sorted file will be stored in the same folder as the original one with the `.sorted.` suffix in the name.  
Sorted file will be automatically BGZip-compressed, if a target file name contains `.gz` postfix. If target file is not specified, file compression is inherited from the original file.

*Example*

```bash
# Will sort given VCF file and place it in the same folder
# with original file ('/samples/sample.vcf.gz') with name: sample.sorted.vcf.gz
$ ngb sort /samples/sample.vcf.gz

# Will sort given GFF file and place sorted file to the specified path
# '/samples/sample-sorted.gff'
$ ngb sort /samples/sample.gff /samples/sample-sorted.gff

# Will sort given BED file, compress result and place sorted file
# to the specified path '/samples/sorted_sample.bed.gz'
$ ngb sort /samples/unsorted.bed /samples/sorted_sample.bed.gz
```

***

## Security commands

### Create user

```
ngb reg_user|ru <USER_NAME> [options]

//Options:
//-gr (--groups) <GROUP_ID>|<GROUP_NAME>[,<GROUP_ID>|<GROUP_NAME>...]    Add newly created user to the groups and assign roles, specified in this option. A comma-separated list can be specified
//-t (--table)                                                          Print result as a human-readable table
//-j (--json)                                                           Print result as a JSON string
```

*Description*

Registers a user with the specified <USER_NAME> in the NGB. Optionally, a comma-separated list of groups/roles can be specified using `-gr` option.

> For the correct command's behavior use list from only group/role ids or only names at once. Don't mix them.

Disregarding whether `-gr` is specified - default *ROLE_USER* will be always set.

*Example*

```bash
# Register user with a default `ROLE_USER` role assigned
$ ngb reg_user test_user@example.com

# Register user and assign to additional groups: "Developers" and "QA"
$ ngb reg_user test_user@example.com -gr Developers,QA
```

### Delete user

```
ngb del_user|du <USER_ID>|<USER_NAME> [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```

*Description*

Deletes a user, specified by a <USER_ID> or <USER_NAME> parameter. This will result in 401 "Access denied" for all subsequent requests from the deleted user.

*Example*

```bash
# Delete user 'test_user@example.com' from the NGB server
$ ngb del_user test_user@example.com
```

### Create user group

```
ngb reg_group|rgrp <GROUP_NAME> [options]

//Options:
//-u (--users) <USER_ID>|<USER_NAME>[,<USER_ID>|<USER_NAME>...]     Once a group is created, specified user(s) will be added to that group as members. A comma-separated list of user ids/names can be specified
//-t (--table)                                                      Print result as a human-readable table
//-j (--json)                                                       Print result as a JSON string
```

*Description*

Creates a new user group named, as specified by <GROUP_NAME> parameter. If `-u` option is specified - a list of user(s) will be added into the new group as members.

> For the correct command's behavior use list from only user ids or only names at once. Don't mix them.

*Example*
```bash
# Create a new empty group (users shall be added to the group further)
ngb reg_group Developers

# Create a new group with two members 'test_user@example.com', 'test_user2@example.com'
ngb reg_group Developers -u test_user@example.com,test_user2@example.com
```

### Delete user group

```
ngb del_group|dgrp <GROUP_ID>|<GROUP_NAME> [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```

*Description*

Deletes user group, specified by a <GROUP_ID>|<GROUP_NAME> parameter, from the NGB server. Members of the group won't be deleted, they will be just unassigned from the deleted group.

*Example*

```bash
# Delete a user group named 'Developers'
$ ngb del_group Developers
```

### Add user to an existing group

```
ngb add_group|agrp <GROUP_ID>|<GROUP_NAME> -u|--users <USER_ID>|<USER_NAME>[,<USER_ID>|<USER_NAME>...] [options]

//Options:
//-t (--table)          Print result as a human-readable table
//-j (--json)           Print result as a JSON string
```

*Description*

Adds a single user or a list of users to the group, specified by <GROUP_ID> or <GROUP_NAME> parameter. A comma-separated list of user ids or names can be specified.

> For the correct command's behavior use list from only user ids or only names at once. Don't mix them.

*Example*

```bash
# Add 'test_user@example.com' and 'test_user2@example.com' users as members of the 'Developers' group
$ ngb add_group Developers test_user@example.com,test_user2@example.com
```

### Set permissions on the object

```
ngb chmod {mod} [options]

//Arguments:
//{mod} - mandatory parameter, that contains two parts: [Permission][Action type]
//Permission:
//r - read permission
//w - write permission
//rw - both, read and write permissions
//Action type:
//`+` - to grant permission
//`-` - to deny permission
//`!` - to remove all already assigned permissions

//Options:
//-ds|--datasets <DATASET_NAME>[,<DATASET_NAME>...]
//-fl|--files <FILE_NAME>[,<FILE_NAME>...]
//-tg|--targets <TARGET_ID>[,<TARGET_ID>...]
//-u|--users <USER_NAME>[,<USER_NAME>...]
//-gr|--groups <GROUP_NAME>[,<GROUP_NAME>...]
```

*Description*

Grants or denies permissions on the objects (files and datasets) for specific user(s), group(s), role(s). Comma-separated lists of file/dataset/user/group/role names can be specified.

> **Notes**:
>
> - One of or all `--datasets`/`--files`/`--targets` shall be specified
> - One of or both `--users`/`--groups` shall be specified
> - Operation is NOT recursive, applied only to the specified `--datasets`/`--files`/`--targets`

*Example*

```bash
# Grant 'read' permission on the file 'gene_file' for the user 'test_user@example.com' and 'Developers' group:
$ ngb chmod r+ -fl gene_file -u test_user@example.com -gr Developers

# Remove assigned permissions on the dataset 'fruitfly_local' for the user 'test_user@example.com':
$ ngb chmod ! -ds fruitfly_local -u test_user@example.com

# Deny 'read' and 'write' permissions on the file 'gene_file' and dataset 'fruitfly_local' for the 'Developers' group:
$ ngb chmod rw- -fl gene_file -ds fruitfly_local -gr Developers
```
