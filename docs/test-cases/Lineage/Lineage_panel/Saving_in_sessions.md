# Saving lineage in sessions

Test verifies
 - that the Lineage view can be saved in Sessions

**Prerequisites:**

Name of lineage: **Empty** 
<br> node file - **empty-nodes1.txt**
```
name    description     reference_id    creation_date   attributes
strain-01	description1    1	-	key1=value1,key2=value2
strain-02	description2    2	2020-11-10	-
strain-03	description3    3	2020-11-10	key1=value1,key2=value2$
strain-04	.	4	2020-11-10	key=value
strain-05	description5    -	2020-11-10	Project=Bacteria,Type=A$
strain-06	description6    6	2020-11-10	Testkey=testvalue
strain-07	description7    7	2020-11-10	.
```

edge file - **empty-edges1.txt**
```
from    to	attributes	type_of_interaction
strain-01	strain-02	-	.
strain-02	strain-03	.	-
strain-02	strain-05	key1=value1,key2=value2 testzdfbzdrbvzs`egvzsvz$
strain-03	strain-04	key1=value1,key2=value2 -
strain-03	strain-05	key1=value1,key2=value2 UV
strain-04	strain-06	key1=value1,key2=value2 UV
strain-04	strain-07	Project=Bacteria,Type=AMR study UV
```


| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to **DATASETS** panel| |
| 3 | Select any dataset in **GRCH38** reference(e.g. **SV_Sample1**)||
| 4 | Go to **Views** menu on the main toolbar| <li> **Views** menu is displayed <li> **LINEAGE** panel is displayed after HEATMAP panel|
| 5| Select **LINEAGE** panel| <li> **LINEAGE** panel is opened|
| 6 | Select **Empty** lineage in the **Source** drop-down field| Visualization of the chosen lineage tree is displayed in the **LINEAGE** panel|
| 7 | Сhange the arrangement of **Empty** tree nodes in the panel | Nodes are changed| 
| 8 | Remember new arrangement of nodes in the tree ||
| 9| Go to **BROWSER** panel|| 
| 10| Set CHR:**2** in **Chromosome** field of **BROWSER** header|The **Location** icon is appeared in the main menu on the left page side|
| 11| Click **Location** button| **Save session** pop-up is appeared| 
| 12| Fill Session name - **Empty Lineage**| |
| 13| Click **Save** button| Pop-up is closed| 
| 14| Go to **Views** in Main Menu||
| 15| Open **Sessions** panel| **Sessions** panel is opened| 
| 16| Find created session and click on it: **Empty Lineage**| <li> All panels that were open when saving the session are displayed: Sessions, Browser, Lineage, Datasets,Variants
| 17| Go to **LINEAGE** panel|  <li> **Empty** lineage is selected in **LINEAGE** panel <li> The tree is displayed as it was remembered in 8 step|