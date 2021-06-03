# BLASTn search from file 

Test verifies
 - BLASTn search from file

**Prerequisites**:
 - dataset = **Felis_catus**, .gtf = **[Felis_catus.Felis_catus_9.0.94.sorted.gtf](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/Felis_catus.Felis_catus_9.0.94.sorted.gtf)**
.bam = **[SRR5373742-10m.bam](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/agnts3.09-28.trim.SRR5373742-10m.bam)**
 - [Sequence of BRCA2-201 transcript for BRCA2 gene](Sequence_data/Sequence_of_BRCA-201_transcript.md)

 - Go to NGB
 - Close **Blast** panel if it is opened

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to **Datasets** tab  | |
| 3 | Select dataset, bam file from **Prerequisites** | <li> Bam file (**SRR5373742-10m.bam**) is selected in dataset (**Felis_catus**)|
| 4 | Go to **Browser header**| 
| 5 | Set checkbox **Felis_catus.Felis_catus_9.0.94.sorted.gtf** in the **FELIS_CATUS** dropdown list (if it is not set)  | 
| 6 | Set checkbox near **Choose file** button| <li>**Choose file** button is enabled <li>**Query sequence** field is disabled |
| 7 | Click on **Choose file** button| A window appears for selecting a file from local workstation| 
| 8 | Select the file with a sequence from the local workstation| The file name is appeared near the "Choose file" button |
| 9 | Fill **Task title** field by any value (e.g. "Manual BLASTn search from Views") | | 
| 10| Select **Homo_sapiens.GRCh38** database from the dropdown in the **Database** field|| 
| 11| Type and select **Homo sapiens** in **Organism** field| |
| 12| Look at **Algoritm** field| **megablast (highly similar sequence)** value displays in the **Algoritm** field by default|
| 13| Click **Search** button|  <li> Search is started <li> **History** sub-tab is opened automatically <li> A new search task created with auto-generated ID <li>  Title specified at step 9 is displayed in the **Task title** column <li> Current state is **Searching...** <li>  Date and time when the certain search was started is displayed in the **Submitted at** column <li> Duration of the certain search task displays in the **Duration** column <li> Button to **cancel search** (conventionally shown by a **cross-button**) displays to the right of the task <li>  Button to **open the search again**  (conventionally shown by a **reverse arrow-button**) displays to the right of the cross-button|
| 14| Wait until status changed to **Done**| <li> **Current state** changed to **Done** <li> **Task ID** becomes a hyperlink <li> Button **to open the search again** in the "Search" sub-tab (reverse arrow-button) displays **only**|
| 15| Click on last search ID in the **Task ID** column | <li> The corresponding results is opened in the same **History** sub-tab <li> **Blast parameters** collapsible section (collapsed by default) is displayed <li> Search results are displayed in the table of **History** sub-tab below **Blast parameters** section
| 16| Click on the first sequence ID in the **Sequence ID** column  | <li> the form with details about all matches (alignments) of the search query to the certain sequence is opened <li> "Alignments" form is opened in the same History sub-tab |
| 17| Click on **Id** of the last searched sequence in the **Task ID** column| <li> Collapsed **Blast parameters** section is displayed with details (parameters/options) of the opened search above **Sequences table** in the same **History** sub-tab <li> The corresponding search results for the desired sequence is displayed in the **Sequences table** <li> **Homo sapiens** organism only displays in the **Organism** column|
| 18| Click on the first **Sequence ID** link in the **Sequence ID** column| <li> Form with details about all matches (alignments) of the search query to the certain sequence is opened <li> **Alignments** form is opened in the same History sub-tab <li> Strands of each sequence (query and subject) - plus or minus displays on the Alignments form <li> Symbols that "link" the corresponding letters in both sequences displays on the Alignments form: straight line if letters are equal, nothing (empty) if letters are not equal (mismatch), minus symbol ("-") in any sequence - for gaps |
| 19| Click on **View at track** button for the first sequence |An alignment of the nucleotide sequence is appered in the **Browser** panel| 
| 20| Go to **Sequences table** on the NGB| 
| 21| Click on **Sequence ID** link |  Corresponding sequence page on NCBI is opened (if exists)|