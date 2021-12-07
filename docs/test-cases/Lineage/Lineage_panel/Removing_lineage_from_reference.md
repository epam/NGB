# Removing lineage 

Test verifies
 - that the Lineage is not displayed in the Lineage panel for all attached references after removing.

**Prerequisites:**

1. References = **GRCH38**, **HG38**
2. Name of lineage: **Remove-lineage**
<br> node file: 
```
name    description     reference_id    creation_date   attributes
strain-01	description1    1	2020-11-10	key1=value1,key2=value2
strain-02	description2    225	2020-11-11	key1=value1,key2=value2
```
 edge file:
```
from    to	attributes	type_of_interaction
strain-01	strain-02	key1=value1,key2=value2 UV
```

4. Go to CLI and use the following command for removing lineage to necessary reference: <br> *template* `ngb delete_lineage[dl] <lineage ID> ` <br> *example* `ngb dl 299`

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to **DATASETS** panel| |
| 3 | Select any dataset in **GRCH38** reference(e.g. **SV_Sample1**)||
| 4 | Go to **Views** menu on the main toolbar| <li> **Views** menu is displayed <li> **LINEAGE** panel is displayed after HEATMAP panel|
| 5 | Select **LINEAGE** panel| <li> **LINEAGE** panel is opened|
| 6 | Select **Remove-lineage** lineage in the **Source** drop-down field| Visualization of the chosen lineage tree is displayed in the **LINEAGE** panel|
| 7 | Click on **Strain-02** name of node | **HG38** reference displays in the **BROWSER** header| 
| 8 | Expand **Source** field | Only one value is displayed in the field list: **Remove-lineage**| 
| 9| Remove **Remove-lineage** in CLI using the command from step 4 in **Prerequisites**|
| 10| Reload the NGB(Ctrl+F5)| The following message displays in the **LINEAGE** panel for all datasets of the **HG38** reference: ***No strain lineages selected. You can select dataset of reference with lineages*** <li> No **Source** field and tree visualization|
| 11 | Go to **DATASETS** panel| |
| 12 | Select any dataset in **GRCH38** reference (e.g. **SV_Sample1**)| <li> The first tree in the list of the **Source** field displays in the Lineage panel in all datasets of **GRCH38** reference <li> Visualization of the chosen lineage tree is displayed in the panel| 
| 13 | Expand **Source** dropdown field| No **Remove-lineage** in the list for all datasets of **GRCH38** reference |