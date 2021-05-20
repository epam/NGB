# Changing profile in the 'Settings'

Test verifies
 - the highlighted variation(s) changes depending on the selected profile

**Prerequisites**:

1. dataset = **dm6_data**, .vcf = **[agnts3.09-28.trim.dm606.realign.vcf](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/agnts3.09-28.trim.dm606.realign.vcf.gz)**
2. **interest_profiles.json** that contains **'Profile 1'** and **'Profile 2'** profiles with conditions:
```
 "Profile 1": {
 "is_default": true,
    "conditions": [
      {"highlight_color": "ff0000",
        "condition": "MQ == 2.81"},
      ]
},
"Profile 2": {
 "is_default": true,
    "conditions": [
      {"highlight_color": "fff000",
        "condition": "DP == 572"},
      ]
}
```

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB as system admin user | |
| 2 | Go to  **Settings** -> **VCF** tab | 'VCF' tab is displayed in the Settings' window|
| 3 | Set checkbox in **Enabled** field (if it is not set)| **'Select the profile'** field is displayed |
| 4 | Select **Profile 1** profile in dropdown list of **'Select the profile'** field|
| 5 | Click **Save** button | <li> Changes successfully saved <li> 'Settings' window is closed | 
| 6 | Go to **Datasets** tab | 
| 7 | Select dataset and vcf file from **Prerequisites** | Vcf file (**agnts3.09-28.trim.dm606.realign.vcf**) is selected in dataset (**dm6_data**) 
| 8 | Go to **Variants** panel | |
| 9 | Find Variation on Position **chr X:12584271**| Background of variation highlight in **'ff0000'(red)** color |
| 10| Click on variation with Position **chr X:12584271** |Browser window is opened with vcf track|
| 11| At the vcf-track, select **'Collapsed'** view (if it was not set previously)| |
| 12| Look at variation on vcf track in Browser| Variation is highlighted in color  **'ff0000'(red)** along all its length in **collapsed** view
| 13| At the vcf-track, select **'Expanded'** view 
| 14| Look at variation on vcf track in Browser| Variation is highlighted in color  **'ff0000'(red)** along all its length in **expanded** view |
| 15| Repeat 2-8 steps for **Profile 2** 
| 16| Find Variation on Position **chr X:12584271**| Conditions are changed. Background of variation **isn't highlighted** in 'ff0000' (red) color **on Variants panel** and **vcf track in Browser in expanded view**|
| 17| At the vcf-track, select **'Collapsed'** view | Variation **isn't highlighted** in 'ff0000' (red) color on **vcf track in Browser** in **collapsed** view
| 18| Find Variation on Position **chr X:12587867**| Background of variation is highlighted  in **'fff000'(yellow)** along all its length on **Variants panel** and **vcf track in Browser** in **collapsed** view|
| 20| At the vcf-track, select **'Expanded'** view | Variation isn't highlighted in **'ff0000' (red)** color on vcf track in Browser in **expanded** view
