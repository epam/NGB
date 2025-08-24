# BLAST - Results: Rerun job from job details
Test verifies
 - that user has the ability to "rerun" any BLAST search task from job details.

**Prerequisites**:
 - Sequence for search:
 `TCGTAACCCGGAAGGGGAATATTTTTCTGGCTATTGTGTTGTTATTTTCAAGCTGCTGCAGTTTCTGTGGCCAAGGGAACCGTCGGGGAAGGATGGTGTGCGAAAAATGTGAGTTAAGAGGCCGCATCTTTGTGAGAAGGAGGCTAGGAGGAGCTAACTGGGCTTCTTCGGGTCCTTGTCTTTTGTTTATTTTTCTTTCAGCCTCAGTTCTCGTGTTCCAGCCTCTGTCCTCTTACCCTTTTTGACCATTCCTGTCCCTCCCCAACGA`
 - Go to NGB
 - Close **Blast** panel if it is opened

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to  **Views** menu on the main toolbar| <li> **Views** menu is displayed <li> **BLAST** panel is displayed after Variants panel|
| 3 | Select **BLAST** panel | <li>**BLAST** panel is displayed first at the right side in the additional panels <li> **BLAST** panel has 2 sub-tabs: **Search** and **History** <li> **Search** sub-tab is opened by default  |
| 4 | Select **blastn** algorithm in the **Algorithm** field |  |
| 5 | Enter sequence from **Prerequisites** in the **Query sequence** field | **Query sequence** field is filled by sequence from **Prerequisites**|
| 6 | Select **Homo_sapiens.GRCh38** database from the dropdown in the **Database** field||
| 7 | Type and select **Homo sapiens (taxid: 9606)** in **Organism** field| |
| 8 | Fill **Task title** field by any value (e.g. "Rerun job")  | |
| 9 | Expand ***Additional parameters*** section | <li> Default value `0.05` is set for ***Expect threshold*** <li> Default value `100` is set for  ***Max target sequences*** |
| 10 | Clear ***Expect threshold*** field |  |
| 11 | Clear ***Max target sequences*** field |  |
| 12 | Click **Search** button|  <li> Search is started <li> **History** sub-tab is opened automatically <li> A new search task created with auto-generated ID <li>  Title specified at step 8 is displayed in the **Task title** column <li> Current state is **Searching...** |
| 13 | Wait until status changed to **Done** | <li>Current state changed to Done <li> Task ID becomes a hyperlink |
| 14 | Click on last search ID in the Task ID column | <li> The name of the task is displayed at the top of the tab <li> Collapsed Blast parameters section is displayed <li> The corresponding search results are displayed in the Sequence table of History sub-tab below Blast parameters section |
| 15 | Click ***Rerun*** button in the top right corner | <li>The ***Search*** sub-tab of the ***BLAST*** panel is opened and <li> All parameters at the "Search" sub-tab are set the same as they were set at the task which is opened to "rerun" <ul><li>***Tool*** : **blastn** <li> **Query sequence** field is filled by sequence from **Prerequisites** <li> ***Task title*** is equal ***Task title*** specified at step 8 <li> ***Database***  is equal ***Database*** specified at step 6 <li> ***Organism*** is equal organism specified at step 7 and ***Exclude*** checkbox isn't checked <li> ***Additional parameters*** have default values |
| 16 | Click **Search** button|  <li> Search is started <li> **History** sub-tab is opened automatically |
| 17 | Wait until status changed to **Done** and click on last search ID in the Task ID column | Search results are the same as search results for task which was opened to "rerun" |