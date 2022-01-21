# Motifs search from Chromosome

Test verifies that
 - Motifs search from Chromosome, displaying results in new MOTIFS panel and on tracks (positive and negative strands)
 - user can use a sequence of nucleotides, a sequence of nucleotides with IUPAC ambiguity codes or a regular expression as the sequence to search

**Prerequisites**:

 - dataset = **sample1**, .vcf = **sample_1-lumpy.vcf**
 - Test data:

| № | Sequence for search | Search <br> Range | Expected results | Description |
| :---: | ------ | :---: | --- | --- |
| 1 | <div style="width:180px">`CTTGATCTTCCCTGTGATGTCATCTGGAGCCCTGCTGCTTGCGGTGGCCTATAAAGCCTC`</div> | CHR:1 | Following values are displayed in the table: <br> <table><thead><tr><th>Chromosome</th><th>Start</th><th>End</th><th>Strand</th> </tr></thead><tbody><tr><th>1</th><th>12826</th><th>12885</th><th>+</th></tr><tr><th>1</th><th>183345</th><th>183404</th><th>+</th></tr></tbody></table> | <div style="width:200px">Search by ***a sequence of nucleotides*** : Positive track |
| 2 | <div style="width:180px">`CTTGATCTTCCCTGTGATGTCATCTGGAGCCCTGCTGCTTGCGGTGGCCTATAAAGCCTC`</div> | CHR:15 | Following values are displayed in the table: <br> <table><thead><tr><th>Chromosome</th><th>Start</th><th>End</th><th>Strand</th> </tr></thead><tbody><tr><th>15</th><th>101978082</th><th>101978141</th><th>-</th></tr></tbody></table> | <div style="width:200px">Search by ***a sequence of nucleotides*** : Negative track |
| 3 |  <div style="width:180px">`CTTGATCTTCCCTGTGATGTCATCTGGAGCCCTGCTGCTTGCGGTGGCCTATAAAGCCTC`</div> | CHR:2 | <li> Message `No records found.` is shown in the search result details <li> New positive/negative tracks don't appear in the top of **Browser** panel | Search without found results | 
| 4 | `TATA[AT]A[AT]A` | CHR:4 | <li> Results are loaded in pages of 100 records <li> At scroll to end of page next 100 records are loaded | <div style="width:200px">Search by ***a regular expression that follows Java regex syntax*** |
| 5 | `CCWSWSAAAA` | CHR:17 | <li> Results are loaded in pages of 100 records <li> At scroll to end of page next 100 records are loaded  | <div style="width:200px">Search by ***a sequence of nucleotides with IUPAC ambiguity codes*** |

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to **Datasets** panel  | |
| 3 | Select dataset, vcf file from **Prerequisites** | VCF file (**sample_1-lumpy.vcf**) is selected in dataset (**sample1**)|
| 4 | Go to **Browser** panel| 
| 5 | Set Chromosome from *Search Range* column for the first row of  of *Test data* table into coordinates | **REFERENCE** track is displayed|
| 6 | Click **General** dropdown menu in **REFERENCE** track|  <li> Context menu is displayed <li> **Motifs search** displays between **Show reverse strand** and **Font size**| 
| 7 | Select **Motifs search** in the context menu  | **Search motifs** pop-up is appeared with following fields and buttons: <li> **Pattern** (*mandatory*) field - empty by default <li> **Title** (*optional*) field  - empty by default <li> **Search whole reference**. ***Warning: this can take a long a time*** (*optional*) checkbox  - unset by default <li> **SEARCH** button - disabled by deault <li> **CANCEL** button <li> **x** (close) button |
| 8 | Fill the fields in the window: <br>**Pattern** = the first sequence from *Test data* table from the Prerequisites <br>**Title** = corresponding `<Search Range>` from *Test data* table from the Prerequisites | <li> **Pattern** and **Title** fields are filled <li> **Search** button is enabled|
| 9 | Click **SEARCH** button|<li> **Motifs search** pop-up is closed <li> **MOTIFS** panel with details is displayed first at the right side in the additional panels <li> The search process is displayed at the top of the panel as a loader with the heading **Loading results** <br><br> The following values display in the panel: <li> Motif's name specified as Title at step 8 is displayed below loading process <li>  **<**  - button displays to the left of the Motif's name (returns to list of all search motifs) <li> A table with the specific search result details (all found motif matches) with columns: **Chromosome, Start, End, Strand** (*columns are empty until the search process is completed*)|
| 10| Wait until the search process is completed  | <li> The loader is disappeared <li> Motif search results are displayed in the table | 
| 11 | Look at the table | <li> Search results match to expected result from *Test data* table from the Prerequisites for corresponding Search Sequence <li> Search results are sorted ascending by *Start* column | 
| 12 | Go to **BROWSER** panel| Two tracks are displayed in the top of panel (under the **REFERENCE** track): <ul><li> `<Title>_positive` <li> `<Title>_negative`, <br> where `<Title>` is the search task *Title* specified at step 8 </ul> **General** context menu is displayed in the both track's header|
| 13 | Look at `<Title>_positive` and `<Title>_negative` tracks | At track corresponding the strand of first search result, the sequences match the specified search pattern is shown: <li>	the sequence is shown as colorized rectangle <li>	the strand is shown for each sequence by white arrows inside rectangle (left to right for positive strand and right to left for negative) <li>	the color for sequences at one track is the same <li>	the color for sequences of different strands is mismatch |
| 14 | Repeat steps 5-13 for each case of *Sequence for search* from the  *Test data* table from the Prerequisites | At change chromosome in **Browser** coordinates all opened motifs results tracks are closed |
| 15 | Click **<** button| <li> A table with all runned searches for the current dataset is displayed with following columns: <ul><li>**Name** - displays search task titles specified at step 8 for each case <li>**Motif** - displays search patterns for each case <li>**Search type** - displays search type *Chromosome* </ul><li> 5 search rows display in the table <li> search results are sorted in order of appearance (the last search is shown on top) |
| 16 | Step by step click results from **MOTIFS** panel and click **<** button to return back | <li> The corresponding chromosome and coordinates are opened <li> The corresponding motifs tracks are re-opened with results on them <li> For each new successful motifs search, additional two corresponding tracks are opened |
