# Lineage nodes

Test verifies
 - the correct display of the lineage nodes in the tree

**Prerequisites:**
1. References = **GRCH38**, **DM6**, **FELIS_CATUS**
2. Name of Lineage: **Lineage-test**,<br> Description: "Lineage for test-cases" 

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
| 2 | Go to **DATASETS** panel||
| 3  | Select **SV_Sample1** dataset ||
| 4 | Go to **Views** menu on the main toolbar| <li> **Views** menu is displayed <li> **LINEAGE** panel is displayed after HEATMAP panel|
| 5  | Select **LINEAGE** panel| <li> **LINEAGE** panel is opened|
| 6  | Click on **Source** dropdown field | <li> A drop-down list is displayed with all the trees attached to the selected reference in ascending order <li> The first lineage in the list displays by default <li>A visualization of the first lineage tree is displayed below the **Source** field|
| 7  | Select **Lineage-test** lineage in **Source** field| <li>**Lineage-test** name of tree displays in **Source** field <li> Description text displays below **Source** field: **Lineage for test-cases** <li> A visualization of the selected lineage tree is displayed below the **Source** field at the middle of the panel| 
| 8  | Find node with **Wild_type** name in the tree | <li> **Wild_Type** node is a simple black text (not a link) <li> Date is displayed below the name in the following format: MM DD, YYYY (e.g.May 11, 1998) <li> Color of node is white |
| 9 | Click on node with **Wild_type** name | <li> The node color turned blue <li> The label of the selected node appears in the upper right corner of the panel with the following values: <br> Name: **Wild type** - *simple black text* <br>*Date:* **(Sequenced: May 11,1998)** <br>*Description*:**Original wild type** <br>*Attributes:* **PROJECT:GENOMICS <br>CLASS: INCESTA** <br><br><li> Close (x) button displays at the right upper corner of label|
| 10| Click on **Wild_type** name of node| <li> **Wild_type** name is not a link <li> Switching to another reference does not occur in the **BROWSER** header <li>|
| 11 | Look at **BROWSER** panel | **GRCH38** reference displays in the header of panel|
| 12 | Go back to **LINEAGE** panel| 
| 13 |Repeat steps 8-12 for every node shown in the **Node table** below ||

<br>**Nodes table**
| Steps | Node-name | Node-date|Reference ID | Expected results |
| :---: | --- | --- | --- | --- |
| 1 | Wild_type | May 11, 1998  |ID="-" (*no reference*)|  <li>*Label* = <br>*Name:* **Wild_type** <br>*Date:* ***(Sequenced: May 11, 1998)*** <br>*Description:* **Original wild type** <br>*Attributes:* **PROJECT: GENOMICS,<br>CLASS: INSECTA** <li>*Reference in Browser* = **GRCH38**|
| 2 | Strain-01 | Dec 1, 1999| ID=1|<li>*Label* =  <br> *Name:* **Strain-01** <br>*Date:* ***(Sequenced: Dec 1, 1999)***<br>*Description:* **First strain from the wild type** <br>*Attributes:* **PROJECT: GENOMICS <br>CLASS: INSECTA** <li>*Reference in Browser* = **GRCH38**|
| 3 | Strain-02 | - |ID=2| <li>*Label* = <br>*Name:* **Strain-02**<br>*Description:* **First strain from the wild type** <br>*Attributes:* **PROJECT: GENOMICS <br>CLASS: INSECTA**<li>*Reference in Browser* = **DM6**|
| 4 | Strain-03 | - |ID=5 (*reference not exist*) | <li>*Label* = <br> *Name:* **Strain-03** <br>*Description:* **Second strain created from Strain-01** <li>*Reference in Browser* = **DM6**|
| 5 | Long-long test name | Jan 25, 2003| ID=3|<li>*Label* = <br> *Name:* **Long-long test name** <br>*Date:* ***(Sequenced: Jan 25, 2003)*** <br>*Description:*  **First strain created from Strain-02 and from Strain-03** <br>*Attributes:* **PROJECT: GENOMICS_TEST_PROJECT <br>CLASS: INSECTA_TEST_CLASS** <li>*Reference in Browser* = **FELIS_CATUS**|