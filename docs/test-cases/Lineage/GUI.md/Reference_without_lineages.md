# Displaying lineage panel for reference without lineage trees 

Test verifies
 - the case checks the page display if the reference has no attached lineage


| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2  | Go to **DATASETS** panel| |
| 3 | Select any dataset without lineage (e.g.**test**)|
| 4 | Go to **Views** menu on the main toolbar| <li> **Views** menu is displayed <li> **LINEAGE** panel is displayed after HEATMAP panel|
|  5 | Select **LINEAGE** panel| <li> **LINEAGE** panel is displayed first at the right side in the additional panels <li> The following text displays in the panel: <br> **No strain lineages selected. You can select dataset of reference with lineages** <br><br><li> **No** **'Source'** dropdon field <li> **No** lineage tree in the panel|