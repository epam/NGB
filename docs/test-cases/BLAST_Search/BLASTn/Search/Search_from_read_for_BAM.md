# BLASTn search from read (BAM)

Test verifies
 - BLASTn search from BAM track 

**Prerequisites**:

 - dataset = **Felis_catus**, .bam = **[SRR5373742-10m.bam](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/agnts3.09-28.trim.SRR5373742-10m.bam)**
 - Sequence for search: 
 - Go to NGB
 - Close **Blast** panel if it is opened

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to **Datasets** tab  | |
| 3 | Select dataset, bam file from **Prerequisites** | <li> Bam file (**SRR5373742-10m.bam**) is selected in dataset (**Felis_catus**)|
| 4 | Go to Coordinates and search input control at right of tab's header|  | 
| 5 | Enter **** in the **TYPE COORDINATES** and click **Enter**|  **BAM** track displays in the **Browser** panel|
| 6 | Go to BAM track| | 
| 7 | Click on BAM alignment **specific or any**? | Context menu is displayed <li> **BLASTn Search**  display between **BLAT Search** and **Copy info to clipboard** <li> **BLASTp Search** isn't visible in the context menu||
| 8 | Click on **BLASTn Search** in the context menu | <li> **BLAST** panel is opened first at the right side in the additional panels <li> **blastn** search tool is selected by default <li> The corresponding sequence displays in the **Query Sequence** field from **Prerequisites**|
| 9 | Fill **Task title** field by any value (e.g. Test BAM nucleotide) in the **Task title** field | **Task titile** field is filled| 
| 10| Select **Homo_sapiens.GRCh38** database from the dropdown in the  **Database** field | | 
| 11| Type and select **Homo sapiens** in the dropdown list of **Organism** field||
| 12| Click **Search** button | <li> Search is started <li> **History** sub-tab is opened automatically <li> A new search task created with auto-generated ID <li> Title specified at step 9 is displayed in the **Task title** column <li> Current state is **Searching...** <li>  Date and time when the certain search was started is displayed in the **Submitted at** column <li> Duration of the certain search task displays in the **Duration** column <li> Button to **cancel search** (conventionally shown by a **cross-button**) displays to the right of the task <li>  Button to **open the search again**  (conventionally shown by a **reverse arrow-button**) displays to the right of the cross-button |
| 13| Wait until status changed to **Done**| <li> **Current state** changed to **Done**  <li> **Task ID** becomes a hyperlink <li> Button **to open the search again** in the "Search" sub-tab (reverse arrow-button) displays **only** |
| 14| Click on **Id** of the last searched sequence in the **Task ID** column | <li> Collapsed **Blast parameters** section is displayed with details (parameters/options) of the opened search above **Sequences table** in the same **History** sub-tab <li> The corresponding search results for the desired sequence is displayed in the **Sequences table** <li> **Homo sapiens** organism only displays in the **Organism** column|
| 15| Click on the first **Sequence ID** link in the **Sequence ID** column | <li> Form with details about all matches (alignments) of the search query to the certain sequence is opened <li> **Alignments** form is opened in the same History sub-tab <li> Strands of each sequence (query and subject) - plus or minus displays on the Alignments form <li> Symbols that "link" the corresponding letters in both sequences displays on the Alignments form: straight line if letters are equal, nothing (empty) if letters are not equal (mismatch), minus symbol ("-") in any sequence - for gaps|
| 16| Click on **View at track** link for the first sequence| An alignment of the nucleotide sequence displays at a track (graphic visualization) in the **Browser** panel |
| 17| Go to **Sequences table** on the NGB| 
| 18| Click on **Sequence ID** link |  Corresponding sequence page on NCBI is opened (if exists)|

