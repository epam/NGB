# BLAST search is failed

Test verifies
 - how blast search error messages are displayed

**Prerequisites**:
- dataset = **Felis_catus**, .gtf = **[Felis_catus.Felis_catus_9.0.94.sorted.gtf](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/Felis_catus.Felis_catus_9.0.94.sorted.gtf)**
  
 - Go to NGB
 - Close **Blast** panel if it is opened


| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to **Datasets** tab||
| 3 | Select dataset, gtf file from **Prerequisites** | Gtf file (**Felis_catus.Felis_catus_9.0.94.sorted.gtf**) is selected in dataset (**Felis_catus**)
| 4 | Go to  **Views** menu on the main toolbar| <li> **Views** menu is displayed <li> **BLAST** panel is displayed in the Views menu |
| 5 | Select **BLAST** panel | <li>**BLAST** panel is displayed first at the right side in the additional panels <li> **BLAST** panel has 2 sub-tabs: **Search** and **History** <li> **Search** sub-tab is opened by default  <li> **blastn** search selector chosen by default <li> **Query sequence** field is blank <li> **Task title** field is blank <li> The first of the available databases value displays in the **Database** field by default <li> **Organism** field is blank <li> **megablast** value is displayed in **Algorithm** field by default  <li> **Additional parameters** section is collapsed with blank values inside |
| 6 | Enter any value in the **Query sequence** field | | 
| 7 | Enter any title in the **Task title** field | | 
| 8 | Select **Homo_sapiens.GRCh38** in the **Database** field|| 
| 9 | Type and select **Cat** in the **Organism** dropdown field| |
| 10| Click on **Search** button|  <li> Search is started <li> **History** sub-tab is opened automatically <li> A new search task created with auto-generated ID <li>  Title specified at step 6 is displayed in the **Task title** column <li> Current state is **Searching...** <li>  Date and time when the certain search was started is displayed in the **Submitted at** column <li> Duration of the certain search task displays in the **Duration** column <li> Button to **cancel search** (conventionally shown by a **cross-button**) displays to the right of the task <li>  Button to **open the search again**  (conventionally shown by a **reverse arrow-button**) displays to the right of the cross-button|
| 11| Wait until state changed to **Failed**| <li> **Current state** changed to **Failed** <li> Button to open the search again in the "Search" sub-tab (reverse arrow-button) displays |
| 12| Click on last search id in the **Task ID** column with Failed state| <li> No table with corresponding results <li> Collapsed **Blast parameters** section is displayed with details (parameters/options) of the opened search above **Sequences table** in the same **History** sub-tab <li> The following message is displayed in red color under **BLAST parameters** collapsed block: **Search was failed. ERROR: An error has occured on the server.** <li> Download results button is hidden |