# Duplication of GENE tracks 
Test verifies
 - duplication of GENE tracks by features 

**Prerequisites**:

Add dataset with GENE file:
 - GENE with several features (e.g. GENE>1 feature)


| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB  | |
| 2 | Go to **Datasets** tab| |
| 3 | Select added dataset from **Prerequisites**| <li> Dataset is selected with all related files| 
| 4 | Select any Chromosome in the Browser| <li> GENE tracks are displayed in the Browser window|
| 5 | Zoom in to medium level by clicking on **+** contol| <li> Values in GENE track is increased <li> GENES with their names and strands are shown|
| 6 | Look at header of **GENE > 1 feature** track | <li> Additional controls (***Features, Duplicate To Track***) **are displayed** on the track's header between |
| 7 | Look at **GENE > 1 feature** track| <li> Separate features in the opened GENE track are displayed <li> Each feature of the GENE track is colored in its own unique color|
| 8 | Click on **Duplicate Track** control| Context menu is exanded with list of existing features in the current GENE track|
| 9 | Click on any feture (e.g. 'gene')| <li> New one GENE track is appeared <li> Only one chosen feature should be displayed in the track|  
| 10 | Look at **Feature** control | Feature that was selected (e.g.'gene') in the previous step (**9** step) is displayed in the **Feature** control|
| 11 | Click on **Features** control| <li> Context menu is expanded with checkboxes with list of existing features in the current GENE track <li> Selected feature (from **9** step) are checked | 
| 12 | Choose another one feature (e.g. CDS) | <li> 2 features a displayed in the GENE track and in the **Features** control <li> Each of the features have its own color|
| 13 | Click on **Duplicate Track** control in the duplicated GENE track| Context menu is expanded with checkboxes with list of existing features in the current GENE track |
| 14 | Click on any feature (e.g.'exon')| <li> New one GENE track is appeared <li> Only one chosen feature should be displayed in the track|
| 15 | Close 
