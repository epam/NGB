# View job details: View jobs history 

Test verifies
 - that User has the ability to view the specific BLAST search history

**Prerequisites**
 - Sequence for search:
`GAAATTGTCCAAAGATAGTTACCTCTCATAGGACCCCTCACTGACAGCATCCCCTAGCCGCACGTGACTAGTTAACTTAATTGAAAGTAAACGTTTAAAATTCTGTTCTTGAGTCGCGCTTCCCCCGTTTCAAATGCTTCATGTGGCTAGTGGCGACTCCGTTGGACAGCACAAACACGGAACGCTCCCATCCTCGCAGTGAGTTCAGCTACCGTCCCAAAAGATA`
 - Go to NGB
 - Close **Blast** panel if it is opened

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to  **Views** menu on the main toolbar| <li> **Views** menu is displayed <li> **BLAST** panel is displayed after Variants panel|
| 3 | Select **BLAST** panel | <li>**BLAST** panel is displayed first at the right side in the additional panels <li> **Search** sub-tab is opened by default  <li> **blastn** search selector chosen by default |
| 4 | Enter sequence from **Prerequisites** in the **Query sequence** field | **Query sequence** field is filled by sequence from **Prerequisites**|
| 5 | Fill **Task title** field by any value (e.g. "Search history") | |
| 6 | Select **Homo_sapiens.GRCh38** database from the dropdown in the **Database** field||
| 7 | Select **blastn** value in the **Algorithm** dropdown field| |
| 8 | Click **Search** button | <li> **History** sub-tab is opened automatically <li> A new search task created with auto-generated ID |
| 9 | Look at **History** sub-tab| <br> **History** sub-tab has a table with following columns: <li> **Task ID** - *automatic created ID of the certain search task* <li> **Task title** - *title of the certain search task (if it was specified before the search)* <li> **Current state** - *status of the search task* <li> **Submitted at** - *date and time when the certain search was started* <li> **Duration** - *duration of the certain search task*  <li> **Rerun button** (conventionally shown by a reverse arrow-button) displays to the right of all columns without header <br>  <br> **Clear History** button is displayed in the upper right corner above the table in the form of a blue basket <br> **Pagination** are displayed in front of the Clear history button if there are more records than the window can display <br> **Vertical scroll** is displayed only if this is required according to screen size <br> The table is **auto-refreshed every 5 sec** (only if the tab is opened)|
| 10| Look at **Currant state** column | **Currant state** column can have following values with buttons: <li> **"Searching"** - *for task being performed at the moment* <br>  - ***Cancel search*** button (conventionally shown by a cross-button) displays to the right of the task <br>  - ***Rerun*** button (conventionally shown by a reverse arrow-button) displays to the right of the cross-button <li> **"Done"** - *for task successfully finished* <br>  - ***Rerun*** button (conventionally shown by a reverse arrow-button) displays to the right of the task <li>  **"Interrupted"** - *for task canceled during the searching* <br>  - ***Rerun*** button (conventionally shown by a reverse arrow-button) displays to the right of the task <li> **"Failure"** - *for failed task (task finished with errors)* <br>  - ***Rerun*** button (conventionally shown by a reverse arrow-button) displays to the right of the task |
| 11 | Change the columns order in the table | |
| 12| Check that table can be sorted by any column | The table is sortable by any column |
| 13 | Close BLAST tab | |
| 14 | Go to  **Views** menu on the main toolbar | |
| 15 | Select **BLAST** panel | |
| 16 | Go to **History** sub-tab | The corresponding search history are displayed in the table of **History** sub-tab <li> Table is sorted by **Submitted At** column (from the newer tasks to older ones) <li> Column order specified at step 11 is kept |