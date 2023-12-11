# BLASTn search from read (BAM)

Test verifies
 - BLASTn search from BAM track 

**Prerequisites**:

 - dataset = **dm6_data**, .vcf = **[agnts3.09-28.trim.dm606.realign.bam](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/agnts3.09-28.trim.dm606.realign.bam.bai)**
 - Sequence for search: 
```CGGAATCTAGTGACGGAGTCCTTCCCCCAAAGTCCGCCTGTAAGTATACCATGTAAAGTTCGTGGATACACATCCGTTTTTCGTATAGATACCGAACGTATG```
 - Go to NGB
 - Close **Blast** panel if it is opened

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to **Datasets** tab  | |
| 3 | Select dataset, bam file from **Prerequisites** | <li> Bam file (**agnts3.09-28.trim.dm606.realign.bam**) is selected in dataset (**dm6_data**)|
| 4 | Go to Coordinates and search input control at right of tab's header|  | 
| 5 | Enter **X: 12583582 - 12583805** in the **TYPE COORDINATES** and click **Enter**| <li> **BAM** track displays in the **Browser** panel|
| 6 | At the bam-track, select **Collapsed** view (if it was not set previously)| | 
| 7 | Click on BAM read with  **EJE9S:00696:00832** name| Context menu is displayed <li> **BLASTn Search**  display between **BLAT Search** and **Copy info to clipboard** <li> **BLASTp Search** isn't visible in the context menu|
| 8 | Click on **BLASTn Search** in the context menu | <li> **BLAST** panel is opened first at the right side in the additional panels <li> **blastn** search tool is selected by default <li> The corresponding sequence displays in the **Query Sequence** field from **Prerequisites** <li> **Task title, Database, Organism** fields are blank <li> **megablast** value displays in the **Algorithm** field <li> **Search** button is disabled|
| 9 | Fill **Task title** field by any value (e.g. Test BAM nucleotide) in the **Task title** field | | 
| 10| Select **Homo_sapiens.GRCh38** database from the dropdown in the  **Database** field | | 
| 11| Type and select **Homo sapiens** in the dropdown list of **Organism** field||
| 12| Remember the values of the parameters entered on the **Search** sub-tab | Values of the parameters are remembered|
| 13| Click **Search** button | <li> Search is started <li> **History** sub-tab is opened automatically <li> A new search task created with auto-generated ID <li> Title specified at step 9 is displayed in the **Task title** column <li> Current state is **Searching...** <li>  Date and time when the certain search was started is displayed in the **Submitted at** column <li> Duration of the certain search task displays in the **Duration** column <li> Button to **cancel search** (conventionally shown by a **cross-button**) displays to the right of the task <li>  Button to **open the search again**  (conventionally shown by a **reverse arrow-button**) displays to the right of the cross-button |
| 14| Wait until status changed to **Done**| <li> **Current state** changed to **Done**  <li> **Task ID** becomes a hyperlink <li> Button **to open the search again** in the "Search" sub-tab (reverse arrow-button) displays **only** |
| 15|  Click on last search ID in the **Task ID** column |<li> The name of the task is displayed at the top of the tab - **Manual BLASTn search from Views** <li> Collapsed **Blast parameters** section is displayed <li> The corresponding search results are displayed in the **Sequence table** of **History** sub-tab below **Blast parameters** section <li>**Tax ID** =9606   only displays in the **Organism** column <li> Chromosome **12** is displayed first in the **Sequence Id** column of table and has **1** matche in the **Matches** column|
| 16| Expand **BLAST parameters** section|  **BLAST parameters** section contains **Query** button (in the upper right corner) and  details (parameters/options) of the opened search: <li> **Used tool:** blastn <li>**Submitted at:** (current date with time when the certain search was started) <li> **Organism:** Homo sapiens (taxid: 9606) <li> **Database:** Homo_sapiens.GRCh38 <li> **Algorithm:** blastn|
| 17| Click on **Query** | **Search query info** pop-up is displayed with with the corresponding sequence and label with its length|
| 18| Go back to **Sequence table** ||
| 19| Click on the first **12** link in the **Sequence ID** column | <li> The form with details about all matches (alignments) of the search query to the certain sequence is opened in "Alignments" form in the same History sub-tab <li> "Alignments" form contains **1** (position) of the current sequence where the match is defined|
| 20| Click on **View at track** button for the first alignment on the Alignments form| An alignment of the nucleotide sequence is appered in the **Browser** panel| 
| 21| Click on **Sequence ID** link on the Alignments form|  Corresponding sequence page on NCBI is opened (if exists)|
