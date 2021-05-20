# Highlight bubbles depending on the highlighted variations

Test verifies
 - Background color of bubbles in the Collapsed VCF track is highlighted according to the highlighted variations

**Prerequisites**:

1. dataset = **dm6_data**, .vcf = **[agnX1.09-28.trim.dm606.realign.vcf](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/agnX1.09-28.trim.dm606.realign.vcf.gz)**
2. **interest_profiles.json** that contains **'Bubbles'** profile with conditions

```
  "Bubbles": {
  "is_default": true,
    "conditions": [
      {
        "highlight_color": "ff0000",
        "condition": "MQRankSum  == -9.612"
      },
      {
        "highlight_color": "008000",
        "condition": "BaseQRankSum  == -3.191"
      },
      {
        "highlight_color": "add8e6",
        "condition": "MQ == 86.87"
      }
]
},
```
**Colors of variations**
| Position Chr X| Color |
| :---: | --- |
| 12585001 | ff0000 (red) |
| 12585008 | 008000 (green) |
| 12585016 | add8e6 (lightblue) |

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB as system admin user | |
| 2 | Go to  **Settings** -> **VCF** tab | 'VCF' tab is displayed in the Settings' window|
| 3 | Set checkbox in **Enabled** field (if it is not set)| **'Select the profile'** field is displayed |
| 4 | Select **Bubbles** profile in dropdown list of **'Select the profile'** field |
| 5 | Click **Save** button | <li> Changes successfully saved <li> 'Settings' window is closed | 
| 6 | Go to **Datasets** tab | 
| 7 | Select dataset and vcf file from **Prerequisites** | Vcf file (**agnX1.09-28.trim.dm606.realign.vcf**) is selected in dataset (**dm6_data**) 
| 8 | Go to **Variants** panel| |
| 9 | Find Variation on Position **chr X:12585001**| Background of variation highlight in **'ff0000'(red)** color |
| 10| Click on variation with Position **chr X:12585001** | <li>Browser window is opened with vcf track <li>All variations from table above are displayed on the track|
| 11| At the vcf-track, select **'Collapsed'** view (if it was not set previously) |
| 12| Zoom out the track (click '-' button)   until a bubble with a 3 number inside appears | The bubble is highlighted with all 3 colors of variations (red, green, lightblue), evenly distributing the colors like on a pie |
| 13| Zoom in the track (click '+' button) until the bubble breaks up into separate variations | The bubble split back into 3 variations with 3 different colors (red, green, lightblue)|
