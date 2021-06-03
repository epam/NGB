# Manual BLASTn search from Views

Test verifies
 - search of any sequence type by open a new BLAST panel from Views and specify manually all desired search settings
 
**Prerequisites**
- dataset = **Felis_catus**, .gtf = **[Felis_catus.Felis_catus_9.0.94.sorted.gtf](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/Felis_catus.Felis_catus_9.0.94.sorted.gtf)**

 - Sequence for search: 
 ```GAAATTGTCCAAAGATAGTTACCTCTCATAGGACCCCTCACTGACAGCATCCCCTAGCCGCACGTGACTAGTTAACTTAATTGAAAGTAAACGTTTAAAATTCTGTTCTTGAGTCGCGCTTCCCCCGTTTCAAATGCTTCATGTGGCTAGTGGCGACTCCGTTGGACAGCACAAACACGGAACGCTCCCATCCTCGCAGTGAGTTCAGCTACCGTCCCAAAAGATA```
 - Go to NGB
 - Close **Blast** panel if it is opened

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to **Datasets** tab||
| 3 | Select dataset, bam file from **Prerequisites** | Bam file (**SRR5373742-10m.bam**) is selected in dataset (**Felis_catus**)
| 4 | Go to  **Views** menu on the main toolbar| <li> **Views** menu is displayed <li> **BLAST** panel is displayed in the Views menu |
| 5 | Select **BLAST** panel | <li>**BLAST** panel is displayed first at the right side in the additional panels <li> **BLAST** panel has 2 sub-tabs: **Search** and **History** <li> **Search** sub-tab is opened by default  <li> **blastn** search selector chosen by default <li> **Query sequence** field is blank <li> **Task title** field is blank <li> The first of the available databases value displays in the **Database** field by default <li> **Organism** field is blank <li> **megablast** value is displayed in **Algorithm** field by default  <li> **Additional parameters** section is collapsed with blank values inside |
| 6 | Enter sequence from **Prerequisites** in the **Query sequence** field | **Query sequence** field is filled by sequence from **Prerequisites**| 
| 7 | Fill **Task title** field by any value (e.g. "Manual BLASTn search from Views") | | 
| 8 | Select **Homo_sapiens.GRCh38** database from the dropdown in the **Database** field|| 
| 9 | Type and select **Homo sapiens** in **Organism** field| |
| 10| Look at **Algoritm** field| **megablast (highly similar sequence)** value displays in the **Algoritm** field by default|
| 11| Click **Search** button|  <li> Search is started <li> **History** sub-tab is opened automatically <li> A new search task created with auto-generated ID <li>  Title specified at step 7 is displayed in the **Task title** column <li> Current state is **Searching...** <li>  Date and time when the certain search was started is displayed in the **Submitted at** column <li> Duration of the certain search task displays in the **Duration** column <li> Button to **cancel search** (conventionally shown by a **cross-button**) displays to the right of the task <li>  Button to **open the search again**  (conventionally shown by a **reverse arrow-button**) displays to the right of the cross-button|
| 13| Wait until status changed to **Done**| <li> **Current state** changed to **Done** <li> **Task ID** becomes a hyperlink <li> Button **to open the search again** in the "Search" sub-tab (reverse arrow-button) displays **only**|
| 14| Click on last search ID in the **Task ID** column | <li> The corresponding results is opened in the same **History** sub-tab <li> **Blast parameters** collapsible section (collapsed by default) is displayed <li> Search results are displayed in the table of **History** sub-tab below **Blast parameters** section
| 15| Click on the first sequence ID in the **Sequence ID** column  | <li> the form with details about all matches (alignments) of the search query to the certain sequence is opened <li> "Alignments" form is opened in the same History sub-tab |
| 16| Click on **Id** of the last searched sequence in the **Task ID** column| <li> Collapsed **Blast parameters** section is displayed with details (parameters/options) of the opened search above **Sequences table** in the same **History** sub-tab <li> The corresponding search results for the desired sequence is displayed in the **Sequences table** <li> **Homo sapiens** organism only displays in the **Organism** column|
| 17| Click on the first **Sequence ID** link in the **Sequence ID** column| <li> Form with details about all matches (alignments) of the search query to the certain sequence is opened <li> **Alignments** form is opened in the same History sub-tab <li> Strands of each sequence (query and subject) - plus or minus displays on the Alignments form <li> Symbols that "link" the corresponding letters in both sequences displays on the Alignments form: straight line if letters are equal, nothing (empty) if letters are not equal (mismatch), minus symbol ("-") in any sequence - for gaps |
| 18| Click on **View at track** button for the first sequence |An alignment of the nucleotide sequence is appered in the **Browser** panel| 
| 19| Go to **Sequences table** on the NGB| 
| 20| Click on **Sequence ID** link |  Corresponding sequence page on NCBI is opened (if exists)|
