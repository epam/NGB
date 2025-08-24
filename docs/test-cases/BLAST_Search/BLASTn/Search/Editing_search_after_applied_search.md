# Editing search after applied search

Test verifies
 - that the search is applied to the changed parameters
  
**Prerequisites**
 - Sequence for search: 
 ```GAAATTGTCCAAAGATAGTTACCTCTCATAGGACCCCTCACTGACAGCATCCCCTAGCCGCACGTGACTAGTTAACTTAATTGAAAGTAAACGTTTAAAATTCTGTTCTTGAGTCGCGCTTCCCCCGTTTCAAATGCTTCATGTGGCTAGTGGCGACTCCGTTGGACAGCACAAACACGGAACGCTCCCATCCTCGCAGTGAGTTCAGCTACCGTCCCAAAAGATA```
 - Go to NGB
 - Close **Blast** panel if it is opened

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to  **Views** menu on the main toolbar| <li> **Views** menu is displayed <li> **BLAST** panel is displayed after Variants panel|
| 3 | Select **BLAST** panel | <li>**BLAST** panel is displayed first at the right side in the additional panels <li> **BLAST** panel has 2 sub-tabs: **Search** and **History** <li> **Search** sub-tab is opened by default  <li> **blastn** search selector chosen by default <li> **Query sequence** , **Task title** ,**Database**, **Organism** fields are blank <li> **megablast** value is displayed in **Algorithm** field by default  <li> **Additional parameters** section is collapsed with blank values inside |
| 4 | Enter sequence from **Prerequisites** in the **Query sequence** field | **Query sequence** field is filled by sequence from **Prerequisites**| 
| 5 | Fill **Task title** field by any value (e.g. "Manual BLASTn search from Views") | | 
| 6 | Select **Homo_sapiens.GRCh38** database from the dropdown in the **Database** field|| 
| 7 | Type and select **Homo sapiens (taxid: 9606)** in **Organism** field| |
| 8| Select **blastn** value in the **Algorithm** dropdown field| |
| 9| Remember the values of the parameters entered on the **Search** sub-tab | Values of the parameters are remembered|
| 10| Click **Search** button|  <li> Search is started <li> **History** sub-tab is opened automatically <li> A new search task created with auto-generated ID <li>  Title specified at step 5 is displayed in the **Task title** column <li> Current state is **Searching...** <li>  Date and time when the certain search was started is displayed in the **Submitted at** column <li> Duration of the certain search task displays in the **Duration** column <li> Button to **cancel search** (conventionally shown by a **cross-button**) displays to the right of the task <li>  Button to **open the search again**  (conventionally shown by a **reverse arrow-button**) displays to the right of the cross-button|
| 11| Wait until status changed to **Done**| <li> **Current state** changed to **Done** <li> **Task ID** becomes a hyperlink <li> Button **to open the search again** in the "Search" sub-tab (reverse arrow-button) displays **only**|
| 12| Click on last search ID in the **Task ID** column |<li> The name of the task is displayed at the top of the tab - **Manual BLASTn search from Views** <li> Collapsed **Blast parameters** section is displayed <li> The corresponding search results are displayed in the **Sequence table** of **History** sub-tab below **Blast parameters** section <li>**Tax ID** =9606   only displays in the **Organism** column <li> Chromosome **13** is displayed first in the **Sequence Id** column of table and has **10** matches in the **Matches** column|
| 13| Expand **BLAST parameters** section|  **BLAST parameters** section contains **Query** button (in the upper right corner) and  details (parameters/options) of the opened search: <li> **Used tool:** blastn <li>**Submitted at:** (current date with time when the certain search was started) <li> **Organism:** Homo sapiens (taxid: 9606) <li> **Database:** Homo_sapiens.GRCh38 <li> **Algorithm:** blastn|
| 14| Click on **Query** | **Search query info** pop-up is displayed with with the corresponding sequence and label with its length|
| 15| Go back to **Sequence table** ||
| 16| Click on the **13** value in the **Sequence ID** column  | <li> The form with details about all matches (alignments) of the search query to the certain sequence is opened in "Alignments" form in the same History sub-tab <li> "Alignments" form contains **10** (positions) of the current sequence where the match is defined <li> First alignment maximally corresponds to the coordinates corresponding to the human gene|
| 17| Click on **View at track** button for the first alignment on the Alignments form| An alignment of the nucleotide sequence is appered in the **Browser** panel| 
| 18| Click on **Sequence ID** link on the Alignments form|  Corresponding sequence page on NCBI is opened (if exists)|