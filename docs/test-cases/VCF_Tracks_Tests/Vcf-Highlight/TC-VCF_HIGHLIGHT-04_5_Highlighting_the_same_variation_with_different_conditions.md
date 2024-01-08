# Highlight the same variation that matches to different conditions

Test verifies
 - Background color of the same Variation that matches to different conditions is highlighted with the color from the first condition in the Variants panel and VCF track

**Prerequisites**:

1. dataset = **dm6_data**, .vcf = **[agnts3.09-28.trim.dm606.realign.vcf](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/agnts3.09-28.trim.dm606.realign.vcf.gz)**
2. **interest_profiles.json** that contains **Different conditions (red highlight)** and **Different conditions (green highlight)** profiles with conditions:
```
  "Different conditions (red highlight)": {
  "is_default": true,
    "conditions": [
      {
        "highlight_color": "ff0000",
        "condition": "MQ  == 91.95"
      },
      {
        "highlight_color": "008000",
        "condition": "DP  == 543"
      }
]
},
"Different conditions (green highlight)": {
  "is_default": true,
    "conditions": [
      {
        "highlight_color": "008000",
        "condition": "DP  == 543"
      },
      {
        "highlight_color": "ff0000",
        "condition": "MQ  == 91.95"
      }
]
},
```

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB as system admin user | |
| 2 | Go to  **Settings** -> **VCF** tab | 'VCF' tab is displayed in the Settings' window |
| 3 | Set checkbox in **Enabled** field (if it is not set)| **'Select the profile'** field is displayed |
| 4 | Select **Different conditions (red highlight)** profile in dropdown list of **'Select the profile'** field | |
| 5 | Click **Save** button | <li> Changes successfully saved <li> 'Settings' window is closed | 
| 6 | Go to **Datasets** tab | |
| 7 | Select dataset and vcf file from **Prerequisites** | Vcf file (**agnts3.09-28.trim.dm606.realign.vcf**) is selected in dataset (**dm6_data**) |
| 8 | Go to **Variants** panel | |
| 9 | Find Variation on Position **chr X:12585943**| Background of variation highlight in **'ff0000'(red)** color <li> Variation **isn't** highlighted in **'008000' (green)** color |
| 10 | Click on variation with Position **chr X:12585943** |Browser window is opened with vcf track|
| 11 | At the vcf-track, select **'Collapsed'** view (if it was not set previously)| |
| 12 | Look at variation on vcf track in Browser | <li> Variation is highlighted in color  **'ff0000'(red)** along all its length in **collapsed** view <li> Variation **isn't** highlighted in **'008000' (green)** color |
| 13 | At the vcf-track, select **'Expanded'** view | |
| 14 | Look at variation on vcf track in Browser| <li> Variation is highlighted in color  **'ff0000'(red)** along all its length in **expanded** view <li> Variation **isn't** highlighted in **'008000' (green)** color |
| 15 | Repeat steps 8-14 for **"Different conditions (green highlight)"** profile | <li> Variation is highlighted in color  **'008000'(green)** on Variants panel and vcf track <li> Variation **isn't** highlighted in **'ff0000' (red)** color  on Variants panel and vcf track| 