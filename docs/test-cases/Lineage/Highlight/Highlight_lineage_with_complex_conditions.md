# Highlight lineage nodes depending on the selected profile with complex conditions

Test verifies
- Background colour of Lineage node in the Lineage panel is highlighted according to the selected profile with complex conditions

**Prerequisites**:

**lineage_profiles.json** that contains **'Complex Conditions'** profile with conditions
```
 "Complex Conditions" : {
        "is_default" : false,
        "conditions" : [
            {
             	"highlight_color" : "0083ff",
                "condition" : "(generation > 1) and (generation <3) or (generation == 2)"
            },
            {
             	"highlight_color" : "149150",
                "condition" : "(generation in [2]) or (generation == 3)"
            },
            {
             	"highlight_color" : "ce632d",
                "condition" : "(Project in [Genomics]) and ((Class == Insecta) or (Class == Mammalia))"
            },
            {
             	"highlight_color" : "f92900",
                "condition" : "(generation notin [2]) and (amount >= 5)"
            },
              {
               	"highlight_color" : "ff00b2",
                "condition" : "(generation > 4) or (generation < 3) or (amount != 7)"
            }
           ]
         },
```

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB  | |
| 2 | Go to  **Settings** -> **LINEAGE** tab | **LINEAGE** tab is displayed in the **Settings** window|
| 3 | Set checkbox in **Enabled** field (if it is not set)| **'Select the profile'** field is displayed |
| 4 | Select **Simple Conditions** profile in dropdown list of **'Select the profile'** field|
| 5 | Click **Save** button | <li> Changes successfully saved <li> **Settings** window is closed | 
| 6 | Go to **Views** menu ||
| 7 | Select **LINEAGE** panel | **LINEAGE** panel is displayed first at the right side in the additional panels|
| 8 | Open **Highlight-nodes** in the **Source** dropdown field | |
| 9 | Find node with **Node_4** name| Background of node highlight in **'0083ff' (blue)** color |
| 10| Repeat step 9 for every node's name shown in the table below |

| Steps | Condition | Position chr X| Expected results |
| :---: | --- | --- | --- | 
| 1 | (generation > 1) and (generation <3) or (generation == 2) |Node_4| <li> **Node_4** is highlighted in **'0083ff' (blue)** <li> generation = 2 | 
| 2 | (generation in [2]) or (generation == 3) |Node_5| <li> **Node_5** is highlighted in **'149150' (green)** <li> generation = 3 |
| 3 | (Project in [Genomics]) and ((Class == Insecta) or (Class == Mammalia)) | <li> Node_1 <li> Node_2 | <li> **Node_1** is highlighted in **'ce632d' (brown)**, Project = Genomics, Class = Insecta <li>**Node_2** is highlighted in **'ce632d' (brown)**, Project = Genomics, Class = Mammalia|
| 4 | (generation notin [2]) and (amount >= 5) | Node_3 | <li>**Node_3** is highlighted in **'f92900' (red)** <li> generation = 1, amount = 5|
| 5 | (generation > 4) or (generation < 3) or (amount != 7)| <li> Node_7 <li>Node_8| <li> **Node_7** is highlighted in **'ff00b2' (pink)**, generation = 5  <li> **Node_8** is highlighted in **'ff00b2' (pink)**, amount = 6|
