# BLASTn search from file 

Test verifies
 - BLASTn search from file

**Prerequisites**:
  - [Sequence of BRCA2-201 transcript for BRCA2 gene](Sequence_data/Sequence_of_BRCA-201_transcript.md)

 - Go to NGB
 - Close **Blast** panel if it is opened

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to  **Views** menu on the main toolbar| <li> **Views** menu is displayed <li> **BLAST** panel is displayed after Variants panel|
| 3 | Select **BLAST** panel | <li>**BLAST** panel is displayed first at the right side in the additional panels <li> **BLAST** panel has 2 sub-tabs: **Search** and **History** <li> **Search** sub-tab is opened by default  <li> **blastn** search selector chosen by default <li> **Query sequence** , **Task title** ,**Database**, **Organism** fields are blank <li> **megablast** value is displayed in **Algorithm** field by default  <li> **Additional parameters** section is collapsed with blank values inside |
| 3 | Set checkbox near **Choose file** button| <li>**Choose file** button is enabled <li>**Query sequence** field is disabled |
| 7 | Click on **Choose file** button| A window appears for selecting a file from local workstation| 
| 8 | Select the file with a sequence from the local workstation (look at **Prerequisites**)| The file name is appeared near the "Choose file" button |
| 9 | Fill **Task title** field by any value (e.g. "BLASTn search from file") | | 
| 10 | Select **Homo_sapiens.GRCh38** database from the dropdown in the **Database** field|| 
| 11 | Type and select **Homo sapiens (taxid: 9606)** in **Organism** field| |
| 12| Select **blastn** value in the **Algorithm** dropdown field| |
| 13| Remember the values of the parameters entered on the **Search** sub-tab | Values of the parameters are remembered|
| 14| Click **Search** button|  <li> Search is started <li> **History** sub-tab is opened automatically <li> A new search task created with auto-generated ID <li>  Title specified at step 7 is displayed in the **Task title** column <li> Current state is **Searching...** <li>  Date and time when the certain search was started is displayed in the **Submitted at** column <li> Duration of the certain search task displays in the **Duration** column <li> Button to **cancel search** (conventionally shown by a **cross-button**) displays to the right of the task <li>  Button to **open the search again**  (conventionally shown by a **reverse arrow-button**) displays to the right of the cross-button|
| 15| Wait until status changed to **Done**| <li> **Current state** changed to **Done** <li> **Task ID** becomes a hyperlink <li> Button **to open the search again** in the "Search" sub-tab (reverse arrow-button) displays **only**|
| 16| Click on last search ID in the **Task ID** column |<li> The name of the task is displayed at the top of the tab - **BLASTn search from file** <li> Collapsed **Blast parameters** section is displayed <li> The corresponding search results are displayed in the **Sequence table** of **History** sub-tab below **Blast parameters** section <li>**Tax ID** =9606   only displays in the **Organism** column <li> Chromosome **13** is displayed first in the **Sequence Id** column of table and has **10** matches in the **Matches** column|
| 17| Expand **BLAST parameters** section|  **BLAST parameters** section contains **Query** button (in the upper right corner) and  details (parameters/options) of the opened search: <li> **Used tool:** blastn <li>**Submitted at:** (current date with time when the certain search was started) <li> **Organism:** Homo sapiens (taxid: 9606) <li> **Database:** Homo_sapiens.GRCh38 <li> **Algorithm:** blastn|
| 18| Click on **Query** | **Search query info** pop-up is displayed with the corresponding link of uploaded file and label with its length|
| 19| Click on file's link| The corresponding file is downloaded to the local workstation|
| 20| Go back to **Sequence table** ||
| 21| Click on the **13** value in the **Sequence ID** column  | <li> The form with details about all matches (alignments) of the search query to the certain sequence is opened in "Alignments" form in the same History sub-tab <li> "Alignments" form contains **10** (positions) of the current sequence where the match is defined <li> First alignment maximally corresponds to the coordinates corresponding to the human gene|
| 22| Click on **View at track** button for the first alignment on the Alignments form| An alignment of the nucleotide sequence is appered in the **Browser** panel| 
| 23| Click on **Sequence ID** link on the Alignments form|  Corresponding sequence page on NCBI is opened (if exists)|