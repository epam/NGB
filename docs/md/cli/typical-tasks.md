# Accomplishing typical tasks

## Fill new NGB server instance with data
As said above, at [NGB object model section](introduction.md), to make any data visible one should initialize **Reference -> File -> Dataset** objects and link them together.

*Do not forget to replace values of GRCH38_SEQ_PATH and GRCH38_GENES_PATH variables with your own paths.*

```
GRCH38_SEQ_PATH=/ngs_data/genomes/grch.38.fa.gz
GRCH38_GENES_PATH=/ngs_data/genomes/grch38_genes.gtf.gz

$ ngb reg_ref $GRCH38_SEQ_PATH -n grch38 -t

$ ngb reg_file grch38 $GRCH38_GENES_PATH -n grch38_genes -t

$ ngb reg_dataset grch38 my_first_dataset grch38_genes -t
```
As a result you will see new dataset at **NGB Web-client GUI** and be able to browse GRCh38 genome.

After that you can already add your own NGS files.

*Do not forget to replace values of PATH_TO_BAM and PATH_TO_VCF variables with your own paths.*
```
PATH_TO_BAM=/ngs_data/my_first_sample/sample1.bam
PATH_TO_VCF=/ngs_data/my_first_sample/sample1.vcf

$ ngb add_dataset $PATH_TO_BAM $PATH_TO_BAM.bai $PATH_TO_VCF
```
This will allow viewing variations from VCF and view alignments from BAM.
