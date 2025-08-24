# BLASTp search from Gene 

Test verifies
 - BLASTp search from full transcript's sequence of Gene in expanded and collapsed view and it's results

**Prerequisites**:

 - dataset = **Felis_catus**, <br>.gtf = **[Felis_catus.Felis_catus_9.0.94.sorted.gtf](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/Felis_catus.Felis_catus_9.0.94.sorted.gtf)**
<br>.bam = **[SRR5373742-1m.bam](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/agnts3.09-28.trim.SRR5373742-1m.bam.bai)**
 - Sequence of UFM1 gene: `MSKVSFKITLTSDPRLPYKVLSVPESTPFTAVLKFAAEEFKVPAATSAIITNDGIGINPAQTAGNVFLKHGSELRIIPRDRVGSC`

 - Go to NGB
 - Close **Blast** panel if it is opened

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to **Datasets** tab  | |
| 3 | Select dataset, bam file from **Prerequisites** | <li> Bam file (**SRR5373742-1m.bam**) is selected in dataset (**Felis_catus**)|
| 4 | Go to **Browser header**| 
| 5 | Set checkbox **Felis_catus.Felis_catus_9.0.94.sorted.gtf** in the **FELIS_CATUS** dropdown list (if it is not set)  | 
| 6 | Enter **A1: 16980338 - 16992002** in the **TYPE COORDINATES** and click **Enter** in the coordinates at right of tab's header|  **UFM1** gene displays in the Browser| 
| 7 | At the gene-track, select **'Expanded'** view| | 
| 8 | Click on **UFM1-201** transcript of **UFM1** gene | <li> Context menu is displayed <li> **BLASTn Search** and **BLASTp Search** display between **Show Info** and **Show 3D Structure** |
| 9 | Click on **BLASTp Search** in the context menu | **BLAST** panel is displayed first at the right side in the additional panels <li> **BLAST** panel has 2 sub-tabs: **Search** and **History** <li> **Search** sub-tab is opened by default  <li> **BLASTP** search selector chosen by default <li> All the corresponding sequence of **UFM1-201** transcript displays in the **Query Sequence** field (from **Prerequisites**) <li> **Task title** ,**Database**, **Organism** fields are blank <li> **blastp** value is displayed in **Algorithm** field by default <li> **Additional parameters** section is collapsed with default values inside |
| 10 | Fill **Task title** field by any value (e.g. "Search from full **UFM1-201** transcript") |
| 11 | Select **Refseq Selected Proteins** database from the dropdown in the  **Database** field | 
| 12 | Type and select **Homo sapiens (taxid:9606)** in **Organism** field
| 13| Remember the values of the parameters entered on the **Search** sub-tab | Values of the parameters are remembered|
| 14| Click **Search** button|  <li> Search is started <li> **History** sub-tab is opened automatically <li> A new search task created with auto-generated ID <li>  Title specified at step 10 is displayed in the **Task title** column <li> Current state is **Searching...** <li>  Date and time when the certain search was started is displayed in the **Submitted at** column <li> Duration of the certain search task displays in the **Duration** column <li> Button to **cancel search** (conventionally shown by a **cross-button**) displays to the right of the task <li>  Button to **reverse search**  (conventionally shown by a **reverse arrow-button**) displays to the right of the cross-button|
| 15| Wait until status changed to **Done**| <li> **Current state** changed to **Done** <li> **Task ID** becomes a hyperlink <li> Button **reverse search** in the "Search" sub-tab (reverse arrow-button) displays **only**|
| 16| Click on last search ID in the **Task ID** column |<li> The name of the task is displayed at the top of the tab - **Search from full UFM1-201 transcript** <li> Collapsed **Blast parameters** section is displayed <li> The corresponding search results are displayed in the **Sequence table** of **History** sub-tab below **Blast parameters** section <li>**Tax ID** =9606   only displays in the **Organism** column <li> Found chromosome is displayed in the **Sequence Id** column of table and has **1** match in the **Matches** column|
| 17| Expand **BLAST parameters** section|  **BLAST parameters** section contains **Query info** button (in the upper right corner) and  details (parameters/options) of the opened search: <li> **Used tool:** blastp <li>**Submitted at:** (current date with time when the certain search was started) <li> **Organism:** Homo sapiens (taxid: 9606) <li> **Database:** Refseq Selected Proteins <li> **Algorithm:** blastp|
| 18| Click on **Query  info** | **Search query info** pop-up is displayed with with the corresponding sequence and label with its length|
| 19| Go back to **Sequence table** ||
| 20| Click on the value in the **Sequence ID** column | <li> The form with details about all matches (alignments) of the search query to the certain sequence is opened in "Alignments" form in the same History sub-tab <li> "Alignments" form contains **1** (position) of the current sequence where the match is defined|
| 21| Click on **Sequence ID** link on the Alignments form|  Corresponding sequence page on NCBI is opened|
| 22|Go back to Alignments in NGB|
| 23| Click on **View at track** button for the first alignment on the Alignments form| <li> An alignment of the protein sequence is appered in the **BLAST Search results** track in Browser panel |
| 24|Go to Browser header| **GRCH38** dataset displays in the Browser header|
| 25| Set checkbox **GRCh38_Genes** in the **GRCH38** dropdown list (if it is not set) | 
| 26| Look at **GRCh38_Genes** track| **UFM1** gene displays in the **GRCh38.Genes** track
| 27| At the gene-track, select **'Collapsed'** view (if it is not set)||
| 29| Repeat 8-9 steps for **UFM1** gene on **GRCh38_Genes** track|

