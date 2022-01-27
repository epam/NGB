# Highlight lineage nodes depending on the selected profile with simple conditions

Test verifies
- Background colour of Lineage node in the Lineage panel is highlighted according to the selected profile

**Prerequisites**:

 **lineage_profiles.json** that contains **'Simple Conditions'** profile with conditions
```
 "Simple Conditions": {
    "is_default": true,
    "conditions": [
      {
        "highlight_color": "ff0000",
        "condition": "Project == Genomics"
      },
      {
          "highlight_color": "ffff00",
        "condition": "Project != Test"
      },
      {
       	"highlight_color": "008000",
        "condition": "amount > 5"
      },
      {
        "highlight_color": "add8e6",
       	"condition": "generation >= 4"
      },
      {
        "highlight_color": "0000ff",
       	"condition": "amount < 6"
      },
      {
        "highlight_color": "ffc0cb",
        "condition": "generation <= 3"
      },
      {
        "highlight_color": "ffa500",
        "condition": "Class in [Birds]"
      },
      {
       	"highlight_color": "f5f5dc",
        "condition": "Species notin [Jackal]"
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
| 9 | Find node with **Node_1** name| Background of node highlight in **'ff0000'(red)** color |
| 10| Repeat step 9 for every node's name shown in the table below |   | 


| Steps | Condition | Node's name| Expected results |
| :---: | --- | --- | --- | 
| 1 | Project == Genomics |Node_1 | <li> **Node_1** is highlighted in **'ff0000' (red)**  <li>Project = Genomics | 
| 2 | Project != Test |Node_2| <li> **Node_2** is highlighted in **'ffff00' (yellow)**  <li> Project = Genomics |
| 3 | amount > 5 |Node_8| <li> **Node_8**  is highlighted in **'008000' (green)** <li> amount = 6|
| 4 | generation >= 4 | <li>Node_6 <li>Node_7| <li> **Node_6** is highlighted in **'add8e6' (lightblue)**, generation = 4 <li> **Node_7** is highlighted in **'add8e6' (lightblue)**, generaton = 5
| 5 | amount < 6| Node_3 | <li> **Node_3** is highlighted in **'0000ff' (blue)** <li> amount = 5|
| 6 | generation <= 3|<li>Node_4 <li>Node_5| <li> **Node_4** is highlighted in **'ffc0cb' (pink)**, generation = 2 <li>**Node_5** is highlighted in **'ffc0cb' (pink)**, generation = 3|
| 7 | Class in [Birds] |Node_9| <li>**Node_9** is highlighted in **'ffa500' (orange)**, <li> Class = Birds|
| 8 | Species notin [Jackal] |Node_10| <li>**Node_10** is highlighted in **'f5f5dc' (beige)**  , <li> Species = Wolf |