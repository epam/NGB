# Lineage edges

Test verifies
 - the correct display of the lineage edges in the tree

**Prerequisites:**
 <br>Name of Lineage: **Lineage-test**,<br> Description: "Lineage for test-cases" 

Node file: **Lineage_test_node.tsv**
```
name    description     reference_id    creation_date   attributes
Wild_type	Original wild type	-	1998-05-11	Project=Genomics,Class=Insecta
Strain-01	First strain from the wild type 1	1999-12-01	Project=Genomics,Class=Insecta
Strain-02	First strain created from Strain-01     2	.	Project=Genomics,Class=Insecta
Strain-03	Second strain created from Strain-01    5	-	-
Long-long test name     First strain created from Strain-02 and from Strain-03  3	2003-01-25	Project=Genomics_test_project,Class=Insecta_test_class
```
Edge file: **Lineage-test-edge.tsv** 

```
from    to	attributes	type_of_interaction
Wild_type	Strain-01	Project=Genomics,Interaction=UV UV1
Strain-01	Strain-02	-	UV2
Strain-01	Strain-03	Project=Genomics,Interaction=UV .
Strain-02	Long-long test name     .	.
Strain-03	Long-long test name     Project=Genomics_test_project,Class=Insecta_test_class  Type of interaction with long name
```
| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2  | Go to **DATASETS** panel||
| 3  | Select **SV_Sample1** dataset ||
| 4 | Go to **Views** menu on the main toolbar| <li> **Views** menu is displayed <li> **LINEAGE** panel is displayed after HEATMAP panel|
| 5  | Select **LINEAGE** panel| <li> **LINEAGE** panel is opened|
| 6  | Click on **Source** dropdown field | <li> A drop-down list is displayed with all the trees attached to the selected reference in ascending order <li> The first lineage in the list displays by default <li>A visualization of the first lineage tree is displayed below the **Source** field|
| 7  | Select **Lineage-test** lineage in **Source** field| <li>**Lineage-test** name of tree displays in **Source** field <li> Description text displays below **Source** field: **Lineage for test-cases** <li> A visualization of the selected lineage tree is displayed below the **Source** field at the middle of the panel| 
| 8  | Find edge **UV1**** in the tree | <li> **UV1** arrow goes from **Wild_type** to **Strain-01** <li> Arrow is grey <li> Name of edge is blue|
| 9 | Click on arrow with **UV1** name| <li> The edge arrow turned blue <li> The label of the selected edge appears in the upper right corner of the panel with the following values: <br> Name: **UV1** - *simple black text* <br>*Attributes:* **PROJECT:GENOMICS <br>CLASS: INCESTA** <br><br><li> Close (x) button displays at the right upper corner of label|
| 10| Repeat steps 8-10 for every edge shown in the **Edge table** below ||


<br> **Edge table**

| Steps | From->To Node| Edge-name |  Label |
| :---: | --- | --- | ---|
| 1 | Wild_type -> Strain-01| UV1 |<li> *Name:* **UV1** <li> *Attributes:* **PROJECT: GENOMICS <br>INTERACTION: UV**|
| 2 | Strain-01 -> Strain-02| UV2 | <li> *Name:* **UV2** |
| 3 | Strain-01 -> Strain-03 | - | <li> *Attributes:* **PROJECT: GENOMICS <br>INTERACTION: UV**|
| 4 | Strain-02 -> Long-long test name| - |**No label**|
| 5 | Strain-03 -> Long-long test name| Type of intera... | *Name:* **Type of interaction with long name** <br> *Attributes:* **PROJECT: GENOMICS_TEST_PROJECT <br> CLASS: INSECTA_TEST_CLASS**