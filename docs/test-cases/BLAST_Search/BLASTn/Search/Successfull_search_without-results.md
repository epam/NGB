# Successfull BLASTn Search without results

Test verifies
 - how blast search messages are displayed for succeed search without results

  
**Prerequisites**:
- dataset = **Felis_catus**, .gtf = **[Felis_catus.Felis_catus_9.0.94.sorted.gtf](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/Felis_catus.Felis_catus_9.0.94.sorted.gtf)**
- Sequence for search: ```GAAATTGTCCAAAGATAGTTACCTCTCATAGGACCCCTCACTGACAGCATCCCCTAGCCGCACGTGACTAGTTAACTTAATTGAAAGTAAACGTTTAAAATTCTGTTCTTGAGTCGCGCTTCCCCCGTTTCAAATGCTTCATGTGGCTAGTGGCGACTCCGTTGGACAGCACAAACACGGAACGCTCCCATCCTCGCAGTGAGTTCAGCTACCGTCCCAAAAGATA```
  
 - Go to NGB
 - Close **Blast** panel if it is opened

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to  **Views** menu on the main toolbar| <li> **Views** menu is displayed <li> **BLAST** panel is displayed in the Views menu |
| 3 | Select **BLAST** panel | <li>**BLAST** panel is displayed first at the right side in the additional panels <li> **BLAST** panel has 2 sub-tabs: **Search** and **History** <li> **Search** sub-tab is opened by default  <li> **blastn** search selector chosen by default <li> **Query sequence**, **Task title** ,**Database**, **Organism** fields are blank <li> **megablast** value is displayed in **Algorithm** field by default  <li> **Additional parameters** section is collapsed with blank values inside |
| 4 | Enter sequence from the **Prerequisites** in the **Query sequence** field | | 
| 5 | Enter any title in the **Task title** field (e.g. No results) | | 
| 6 | Select **Felis_catus** in the **Database** field|| 
| 7 | Type and select **Homo_sapiens** in the **Organism** field| |
| 8 | Click on **Search** button|  <li> Search is started <li> **History** sub-tab is opened automatically <li> A new search task created with auto-generated ID <li>  Title specified at step 7 is displayed in the **Task title** column <li> Current state is **Searching...** <li>  Date and time when the certain search was started is displayed in the **Submitted at** column <li> Duration of the certain search task displays in the **Duration** column <li> Button to **cancel search** (conventionally shown by a **cross-button**) displays to the right of the task <li>  Button to **open the search again**  (conventionally shown by a **reverse arrow-button**) displays to the right of the cross-button|
| 9 | Wait until status changed to **Done**|  <li> **Current state** changed to **Done** <li> Button **to open the search again** in the **Search** sub-tab (reverse arrow-button) displays **only** |
| 10| Click on **Id** of the last searched sequence in the **Task ID** column | <li> No table with corresponding results <li> Collapsed **Blast parameters** section is displayed with details (parameters/options) of the opened search above **Sequences table** in the same **History** sub-tab <li> The following message is displayed under **BLAST parameters** collapsed block: **No significant similarity found. Try to change search parameters.** <li> Download results button is hidden |
