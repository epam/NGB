# Motifs search from Chromosome

Test verifies
 - Motifs search from Chromosome, displaying results in new MOTIFS panel and on tracks (positive and negative strands)

**Prerequisites**:

 - dataset = **sample1**, .vcf = **sample_1-lumpy.vcf**
<br><li> Sequence for search:
 CTTGATCTTCCCTGTGATGTCATCTGGAGCCCTGCTGCTTGCGGTGGCCTATAAAGCCTC 
 <br><br>

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to **Datasets** panel  | |
| 3 | Select dataset, vcf file from **Prerequisites** | <li> VCF file (**sample_1-lumpy.vcf**) is selected in dataset (**sample1**)|
| 4 | Go to **Browser** panel| 
| 5 | Set **CHR: 1** in coordinates | <li> **REFERENCE** track is displayed |
| 6 | Click **General** dropdown menu in **REFERENCE** track|  <li> Context menu is displayed <li> **Motifs search** displays between **Show reverse strand** and **Font size**| 
| 7 | Select **Motifs search** in the context menu  | **Search motifs** pop-up is appeared with following fields and buttons: <li> **Pattern** (*mandatory*) field - empty by default <li> **Title** (*optional*) field  - empty by default <li> **Search whole reference**. ***Warning: this can take a long a time*** (*optional*) checkbox  - unset by default <li> **SEARCH** button - disabled by deault <li> **CANCEL** button <li> **x** (close) button |
| 8 |Fill the fields in the window: <br>**Pattern** = sequence from **Prerequisites** <br>**Title** = **Chr: 1**| <li> **Pattern** and **Titile** fields are filled <li> **Search** button is enabled|
| 9 | Click **SEARCH** button|<li> **Motifs search** pop-up is closed <li> **MOTIFS** panel with details is displayed first at the right side in the additional panels <li> The search process is displayed at the top of the panel as a loader with the heading **Loading results** <br><br> The following values display in the panel: <li> Motif's name is displayed below loading process - **Chr: 1** <li>  **<**  - button displays to the left of the Motif's name (returns to list of all search motifs) <li> A table with the specific search result details (all found motif matches) with columns: **Chromosome, Start, End, Strand** (*columns are empty until the search process is completed*)|
| 10| Wait until the search process is completed  | <li> The loader is disappeared <li> Motif search results are displayed in the table | 
| 11 | Look at first row in the table | Following values display in the table: <br>  - **Chromosome**: 1 <br> - **Start**:  12826 <br>  - **End**: 12885 <br>  - **Strand**: + (*positve*) | 
| 12| Go to **BROWSER** panel| Two tracks are displayed in the panel: <li> **Chr: 1_positive** <li> **Chr: 1_negative**  <br><br>***Forward Strand** and **Reverse Strand** display by default in the REFERENCE track*|
| 13| Look at **Chr: 1_positive** track | <li> The Sequence matching the specified search pattern is displayed on the track <li> The sequence is shown as colorized rectangle (blue color by default) <li> The rectangle has white arrows inside in the direction of the positive strand (left to right)<br><br> **General** context menu is displayed in track's header|
| 14| Look at **Chr: 1_negative** track | <li> Track is empty (doesn't contain the sequence matching)<br><br> **General** context menu is displayed in track's header|
| 15| Repeat steps 5-7 for Chromosome **15** | |
| 16 | Fill **Pattern** field only by sequence from **Prerequisites**| <li> **Pattern** field is filled <li> **Title** field is empty <li> **Search** button is enabled|
| 17| Click SEARCH button| <li> **Motifs search** pop-up is closed <li> **MOTIFS** panel with details is displayed first at the right side in the additional panels <li> The search process is displayed at the top of the panel as a loader with the heading **Loading results** <br><br> The following values display in the panel: <li> Motif's seqence is displayed below loading process - from **Prerequisites** <li>  **<**  - button displays to the left of the Motif's sequence (returns to list of all search motifs) <li> A table with the specific search result details (all found motif matches) with columns: **Chromosome, Start, End, Strand** (*columns are empty until the search process is completed*)
| 18| Wait until the search process is completed  | <li> The loader is disappeared <li> Motif search results are displayed in the table with following values:  <br>  - **Chromosome**: 15 <br> - **Start**:  101978082 <br>  - **End**: 101978141 <br>  - **Strand**: - (*negative*) | 
| 19| Go to **BROWSER** panel| Two tracks are displayed in the panel: <li> **{Motif}_positive** <li> **{Motif}_negative** <br><br>***Forward Strand** and **Reverse Strand** display by default in the REFERENCE track*|  
| 20| Look at **{Motif}_positive** track| Track is empty (doesn't contain the sequence matching)<br><br> **General** context menu is displayed in track's header|
| 21| Look at **{Motif}_negative** track | <li> The Sequence matching the specified search pattern is displayed on the track <li> The sequence is shown as colorized rectangle (red color by default) <li> The rectangle has white arrows inside in the direction of the positive strand (right to left)<br><br> **General** context menu is displayed in track's header|
| 22| Go back to **MOTIFS** panel| |
| 23| Click **<** button| <li> A table with all runned searches for the current dataset is displayed with following columns: <br><br>**Name** -  displays search task title <br>**Motif** - displays search pattern <br>**Search type** - displays search type that was selected (chromosome or reference) <li> 2 search rows display in the table
| 24| Look at search values| 1) Name = empty <br>Motif = *sequence from Prerequisites* <br> Search type = **Chromosome**  <br> 2) Name = **CHR:1** <br>Motif = *sequence from Prerequisites* <br> Search type = **Chromosome** |