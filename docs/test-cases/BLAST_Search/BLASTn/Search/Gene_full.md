# BLASTn search from full transcript's sequence of Gene 

Test verifies
 - BLASTn search from full transcript's sequence of Gene (All transcript info)

**Prerequisites**:

 - dataset = **Felis_catus**, .gtf = **[Felis_catus.Felis_catus_9.0.94.sorted.gtf](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/Felis_catus.Felis_catus_9.0.94.sorted.gtf)**
.bam = **[SRR5373742-10m.bam](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/agnts3.09-28.trim.SRR5373742-10m.bam.bai)**
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
| 6 | Go to Coordinates and search input control at right of tab's header|  | 
| 7 | Enter **A1: 11534536 - 11650701** in the **TYPE COORDINATES** and click **Enter**| **BRCA2** gene displays in the Browser|
| 8 | At the gene-track, select **'Expanded'** view| | 
| 9 | Click on **BRCA2-201** transcript of **BRCA2** gene | <li> Context menu is displayed <li> **BLASTn Search** and **BLASTp Search** display between **BLAT Search** and **Copy info to clipboard** |
| 10 | Click on **BLASTn Search** in the context menu | 2 sub-items appear to the right of the context menu: <li> **Exon only** <li> **All transcript info** |
| 11 | Select **All transcript info** | **BLAST** panel is displayed first at the right side in the additional panels <li> **BLAST** panel has 2 sub-tabs: **Search** and **History** <li> **Search** sub-tab is opened by default  <li> **blastn** search selector chosen by default <li> All the corresponding sequence of **BRCA2-201** transcript displays in the **Query Sequence** field as in **Sequence of BRCA2-201 transcript for BRCA2 gene** file from **Prerequisites** <li> **Task title** ,**Database**, **Organism** fields are blank <li> **megablast** value is displayed in **Algorithm** field by default <li> **Additional parameters** section is collapsed with blank values inside |
| 12 | Fill **Task title** field by any value (e.g. "Search from full **BRCA2-201** transcript") | | 
| 13| Select **Homo_sapiens.GRCh38** database from the dropdown in the  **Database** field | | 
| 14 | Type and select **Homo sapiens** in **Organism** field| | 
| 15| Select **blastn** value in the **Algorithm** dropdown field| |
| 16| Remember the values of the parameters entered on the **Search** sub-tab | Values of the parameters are remembered|
| 17| Click **Search** button|  <li> Search is started <li> **History** sub-tab is opened automatically <li> A new search task created with auto-generated ID <li>  Title specified at step 12 is displayed in the **Task title** column <li> Current state is **Searching...** <li>  Date and time when the certain search was started is displayed in the **Submitted at** column <li> Duration of the certain search task displays in the **Duration** column <li> Button to **cancel search** (conventionally shown by a **cross-button**) displays to the right of the task <li>  Button to **open the search again**  (conventionally shown by a **reverse arrow-button**) displays to the right of the cross-button|
| 18| Wait until status changed to **Done**| <li> **Current state** changed to **Done** <li> **Task ID** becomes a hyperlink <li> Button **to open the search again** in the "Search" sub-tab (reverse arrow-button) displays **only**|
| 19| Click on last search ID in the **Task ID** column |<li> The name of the task is displayed at the top of the tab - **Search from full BRCA2-201 transcript** <li> Collapsed **Blast parameters** section is displayed <li> The corresponding search results are displayed in the **Sequence table** of **History** sub-tab below **Blast parameters** section <li>**Tax ID** =9606   only displays in the **Organism** column <li> Chromosome **13** is displayed first in the **Sequence Id** column of table and has **10** matches in the **Matches** column|
| 20| Expand **BLAST parameters** section|  **BLAST parameters** section contains **Query** button (in the upper right corner) and  details (parameters/options) of the opened search: <li> **Used tool:** blastn <li>**Submitted at:** (current date with time when the certain search was started) <li> **Organism:** Homo sapiens (taxid: 9606) <li> **Database:** Homo_sapiens.GRCh38 <li> **Algorithm:** blastn|
| 21| Click on **Query** | **Search query info** pop-up is displayed with with the corresponding sequence and label with its length|
| 22| Go back to **Sequence table** ||
| 23| Click on the **13** value in the **Sequence ID** column  | <li> The form with details about all matches (alignments) of the search query to the certain sequence is opened in "Alignments" form in the same History sub-tab <li> "Alignments" form contains **10** (positions) of the current sequence where the match is defined <li> First alignment maximally corresponds to the coordinates corresponding to the human gene|
| 24| Click on **View at track** button for the first alignment on the Alignments form| An alignment of the nucleotide sequence is appered in the **Browser** panel| 
| 25| Click on **Sequence ID** link on the Alignments form|  Corresponding sequence page on NCBI is opened (if exists)|
