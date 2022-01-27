# Highlight the same lineage node that matches to different conditions

Test verifies
- Background color of the same lineage node that matches to different conditions is highlighted with the color from the first condition in the Lineage tree

**Prerequisites**:

**lineage_profiles.json** that contains **Different conditions (red highlight)** and **Different conditions (green highlight)** profiles with conditions:
```
   "Different conditions (red highlight)": {
  "is_default": true,
    "conditions": [
      {
       	"highlight_color": "ff0000",
        "condition": "generation  == 1"
      },
      {
       	"highlight_color": "008000",
        "condition": "amount  == 5"
      }
]
},
"Different conditions (green highlight)": {
  "is_default": true,
    "conditions": [
      {
       	"highlight_color": "008000",
        "condition": "amount  == 5"
      },
      {
        "highlight_color": "ff0000",
        "condition": "generation == 1"
      }
]
},
```

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB as system admin user | |
| 2 | Go to  **Settings** -> **LINEAGE** tab | '**LINEAGE** tab is displayed in the **Settings** window|
| 3 | Set checkbox in **Enabled** field (if it is not set)| **'Select the profile'** field is displayed |
| 4 | Select **Different conditions (red highlight)** profile in dropdown list of **'Select the profile'** field|
| 5 | Click **Save** button | <li> Changes successfully saved <li> 'Settings' window is closed | 
| 6 | Go to **Views** menu ||
| 7 | Select **LINEAGE** panel | **LINEAGE** panel is displayed first at the right side in the additional panels|
| 8 | Open **Highlight-nodes** in the **Source** dropdown field | |
| 9 | Look at **Node_3** node| Background of node highlight in **'ff0000'(red)** color <li> Node **isn't** highlighted in **'008000' (green)** color|
| 10| Repeat step 9 for **"Different conditions (green highlight)"** profile | <li> **Node_3** is highlighted in color **'008000'(green)** on Lineage panel <li> Node **isn't** highlighted in **'ff0000' (red)** color on Lineage panel| 