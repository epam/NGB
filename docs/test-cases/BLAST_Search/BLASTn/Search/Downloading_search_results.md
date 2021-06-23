# Download results of BLAST search 

Test verifies
 - that it is possible to download the search results to the local workstation as CSV file
 
**Prerequisites**:
 - dataset = **dm6_data**
 - Sequence for search: 
 ```GAAATTGTCCAAAGATAGTTACCTCTCATAGGACCCCTCACTGACAGCATCCCCTAGCCGCACGTGACTAGTTAACTTAATTGAAAGTAAACGTTTAAAATTCTGTTCTTGAGTCGCGCTTCCCCCGTTTCAAATGCTTCATGTGGCTAGTGGCGACTCCGTTGGACAGCACAAACACGGAACGCTCCCATCCTCGCAGTGAGTTCAGCTACCGTCCCAAAAGATA```
 - Go to NGB
 - Close **Blast** panel if it is opened

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to **Datasets** tab  | |
| 3 | Select dataset, bam file from **Prerequisites** | <li> Bam file (**agnts3.09-28.trim.dm606.realign.bam**) is selected in dataset (**dm6_data**)|
| 4 | Go to Coordinates and search input control at right of tab's header|  | 
| 5 | Enter **X: 12583582 - 12583805** in the **TYPE COORDINATES** and click **Enter**| <li> **BAM** track displays in the **Browser** panel|
| 6 | At the bam-track, select **Expanded** view (if it was not set previously)| |
| 7 | Click on BAM read with  **EJE9S:00696:00832** name| Context menu is displayed <li> **BLASTn Search**  display between **BLAT Search** and **Copy info to clipboard** <li> **BLASTp Search** isn't visible in the context menu|
| 8 | Click on **BLASTn Search** in the context menu | <li> **BLAST** panel is opened first at the right side in the additional panels <li> **blastn** search tool is selected by default <li> The corresponding sequence displays in the **Query Sequence** field from **Prerequisites** <li> **Task title, Database, Organism** fields are blank <li> **megablast** value displays in the **Algorithm** field <li> **Search** button is disabled|
| 9 | Fill **Task title** field by any value (e.g. "Downloading search results") | | 
| 10 | Select **Homo_sapiens.GRCh38** database from the dropdown in the **Database** field|| 
| 11 | Type and select **Homo sapiens (taxid: 9606)** in **Organism** field| |
| 12 | Expand **Additional parameters** section|<li>Default value `0.05` is set for ***Expect threshold*** <li> Default value `100` is set for  ***Max target sequences***|
| 13 | Change ***Expect threshold*** value to 50 | |
| 14| Click **Search** button|  <li> Search is started <li> **History** sub-tab is opened automatically <li> A new search task created with auto-generated ID <li>  Title specified at step 9 is displayed in the **Task title** column <li> Current state is **Searching...** <li>  Date and time when the certain search was started is displayed in the **Submitted at** column <li> Duration of the certain search task displays in the **Duration** column <li> Button to **cancel search** (conventionally shown by a **cross-button**) displays to the right of the task <li>  Button to **open the search again**  (conventionally shown by a **reverse arrow-button**) displays to the right of the cross-button|
| 15| Wait until status changed to **Done**| <li> **Current state** changed to **Done** <li> **Task ID** becomes a hyperlink <li> Button **to open the search again** in the "Search" sub-tab (reverse arrow-button) displays **only**|
| 16| Click on last search ID in the **Task ID** column | <li> The corresponding search results are displayed in the **Sequence table** of **History** sub-tab below **Blast parameters** section <li> **12** and **6** chromosomes are disaplyed in **Sequence ID** column <li> Each of the chromosomes has only **1** match|
| 17| Click on the **Download results** button in the upper right corner| <li>Full BLAST search results (raw) is downloaded as CSV file to the local workstation<li> Output file has the name `BLAST-<tool_type>-<database>-<task_title>.csv`, where `<tool_type>` - the type of the tool used for the search - `blastn`, `<task_ID>` - ID of the corresponding search task and `<task_title>` title specified at step 9, <database> - database name specified at step 10 |
| 18| Open downloaded CSV file| <li> The file has the header row<li>The file contains the number of matches of all chromosomes for the search performed = **2** |