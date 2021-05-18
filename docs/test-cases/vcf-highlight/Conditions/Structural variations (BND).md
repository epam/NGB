# Highlight structural variations depending on the selected profile 

Test verifies
 - Background colour of Structural Variations in the Variants panel and VCF track is highlighted according to the selected profile

**Prerequisites**:

1. dataset = **SV_Sample1**, .vcf = **[sample_1-lumpy.vcf](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/sample_1-lumpy.vcf.gz)**
2. dataset = **SV_Sample2**, .vcf = **[sample_2-lumpy.vcf](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/sample_2-lumpy.vcf.gz)**
3. **interest_profiles.json** that contains **'Structural variations'** profile with conditions
```
 "Structural variations": {
  "is_default": true,
    "conditions": [
      {
        "highlight_color": "ff0000",
        "condition": "CIPOS == [-6, 2]"
      },
      {
        "highlight_color": "008000",
        "condition": "SU  > 5"
      },
      {
        "highlight_color": "add8e6",
        "condition": "EVENT >= 111"
      },
      {
        "highlight_color": "0000ff",
        "condition": "EVENT < 31"
      },
      {
        "highlight_color": "ffc0cb",
        "condition": "EVENT <= 34"
      },
      {
        "highlight_color": "ffa500",
        "condition": "EVENT  in [96]"
      },
      {
        "highlight_color": "ffff00",
        "condition": "PE != 4"
      },
      {
        "highlight_color": "f5f5dc",
        "condition": "PE notin [5]"
      }
    ]
},
```

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB as system admin user | |
| 2 | Go to  **Settings** -> **VCF** tab | 'VCF' tab is displayed in the Settings' window|
| 3 | Set checkbox in **Enabled** field (if it is not set)| **'Select the profile'** field is displayed |
| 4 | Select **Structural variations** profile in dropdown list of **'Select the profile'** field|
| 5 | Click **Save** button | <li> Changes successfully saved <li> 'Settings' window is closed | 
| 6 | Go to **Datasets** tab | 
| 7 | Select datasets and vcf files from **Prerequisites** | Vcf files is selected in datasets | 
| 8 | Go to **Variants** panel | |
| 9 | Find Variation on Position **Chr5 :181050143**| Background of variation highlight in **'ff0000'(red)** color |
| 10| Click on variation with Position **Chr5 :181050143** |Browser window is opened with vcf track|
| 11| Look at variation on vcf track in Browser| Variation and it's label is highlighted in color  **'ff0000'(red)** along all its length in expanded and collapsed view
| 12| Repeat steps 8-11 for every variation's position shown in the table below |
 

| Steps | Condition | Position chr| Expected results |
| :---: | --- | --- | --- | 
| 1 | CIPOS == [-6, 2] | Chr:5 181050143| <li> 'BND' variation is highlighted in **'ff0000' (red)**  <li> CIPOS == [-6, 2] | 
| 2 | SU  > 5 |Chr:4 25665006| <li> 'BND' variation is highlighted in  **'008000' (green)** <li>Label of variation is highlighted in  **'008000' (green)** <li> SU = 421 |
| 3 | EVENT >= 111 |Chr:13 48459903 | <li> 'BND' variation is highlighted in **'add8e6' (lightblue)** <li> EVENT = 124|
| 4 | EVENT < 31 | Chr:6 51295112 | <li> 'BND' variation is highlighted in **'0000ff' (blue)** <li>  EVENT = 30
| 5 | EVENT <= 34|Chr:6 117314770 | <li> 'BND' variation is highlighted in **'ffc0cb"' (pink)** <li> EVENT = 34|
| 6 | EVENT  in [96]| Chr:17 31200612 | <li> 'BND' variation is highlighted in **'ffa500' (orange)** <li> EVENT = 96 
| 7 | PE != 4 |Chr:17 31226616| <li>'BND' variation is highlighted in **'ffff00' (yellow)** <li> PE = 5
| 8 | PE notin [5] | Chr:17 31182626 | <li> 'BND' variation is highlighted in **'f5f5dc' (beige)** <li> DP = 216|