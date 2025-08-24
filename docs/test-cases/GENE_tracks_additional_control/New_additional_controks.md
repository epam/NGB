# UI view of additional controls
Test verifies
 - UI view of additional controls for GENE files with one and several features

**Prerequisites**:

Add dataset with GENE files:
1. GENE with one feature only (e.g.GENE = 1 feature)
2. GENE with several features (e.g. GENE>1 feature)


| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB  | |
| 2 | Go to **Datasets** tab| |
| 3 | Select added dataset from **Prerequisites**| <li> Dataset is selected with all related files| 
| 4 | Select any Chromosome in the Browser| <li> GENE tracks are displayed in the Browser window|
| 5 | Zoom in to medium level by clicking on **+** contol| <li> Values in GENE track is increased <li> GENES with their names and strands are shown|
| 6 |  Look at header of **GENE = 1 feature** track|<li> GENE with only one feature is displayed <li> Additional controls (***Features**, **Duplicate To Track***) **aren't displayed** on the track's header|
| 7 | Look at header of **GENE > 1 feature** track| <li> Additional controls (***Features, Duplicate To Track***) **are displayed** on the track's header between **Transcript view** and **Shorten Introns** controls <li> **'All'** value displays in the **Feature** control <li> Context menu is expanded with checkboxes with list of existing features in the current GENE track by clicking on the **Features** control|
| 8 | Look at **GENE > 1 feature** track | <li> Separate features in the opened GENE track are displayed <li> Each feature of the GENE track is colored in its own unique color|
| 9 | Click on **Features** control | <li> Context menu is expanded with checkboxes with list of existing features in the current GENE track <li> All the features are checked by default|
| 10 | Uncheck one of the feature (e.g. 'gene')| <li> Feature (e.g. 'gene') is unchecked in the context menu <li> Unchecked feature (e.g. 'gene') isn't displayed on the current GENE track|
| 11 | Click on **Features** control| <li> Context menu is expanded with checkboxes with list of existing features in the current GENE track <li> All features are checked except for the one that was unchecked in the previous step (**10** step)| 
| 12 | Uncheck all the remaining features in the context menu| <li> All features were unchecked, except for the last one (e.g.) <li> The last feature is selected and disabled <li> Only one feature displays in the current GENE track <br> ***It is impossible to uncheck all the features. At least one feature should be enabled***|