# Highlight variations depending on the selected profile with simple conditions

Test verifies
 - Background colour of Variations in the Variants panel and VCF track is highlighted according to the selected profile

**Prerequisites**:

1. dataset = **dm6_data**, .vcf = **[agnts3.09-28.trim.dm606.realign.vcf](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/agnts3.09-28.trim.dm606.realign.vcf.gz)**
2. **interest_profiles.json** that contains **'Simple Conditions'** profile with conditions.
```
 "Simple Conditions": {
    "is_default": true,
    "conditions": [
      {
        "highlight_color": "ff0000",
        "condition": "DP == 384"
      },
      {
        "highlight_color": "ffff00",
        "condition": "AC != 1"
      },
      {
        "highlight_color": "008000",
        "condition": "FS >450"
      },
      {
        "highlight_color": "add8e6",
        "condition": "MQ >= 91.95"
      },
      {
        "highlight_color": "0000ff",
        "condition": "ReadPosRankSum < -1.600"
      },
      {
        "highlight_color": "ffc0cb",
        "condition": "QD <= 0.32"
      },
      {
        "highlight_color": "ffa500",
        "condition": "DP  in [114,137]"
      },
      {
        "highlight_color": "f5f5dc",
        "condition": "DP not in [206]"
      }
    ]
  },

```

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB as system admin user | |
| 2 | Go to  **Settings** -> **VCF** tab | 'VCF' tab is displayed in the Settings' window|
| 3 | Set checkbox in **Enabled** field (if it is not set)| **select the profile** field is displayed |
| 4 | Select **Simple Conditions** profile in dropdown list of **'select the profile'** field| 
| 5 | Click **Save** button | <li> Changes successfully saved <li> 'Settings' window is closed | 
| 6 | Go to **Datasets** tab | 
| 7 | Choose dataset and vcf file from **Prerequisites** | Vcf file (**agnts3.09-28.trim.dm606.realign.vcf**) is chosen in dataset (**dm6_data**) 
| 8 | Go to **Variants** panel | |
| 9 | Find Variation on Position **chr X:12586950**| Background of variation highlight in **'"ff0000'(red)** color |
| 10| Click on variation with position chr X:12586950 |Browser window is opened with vcf track|
| 11| Look at variation on vcf track in Browser| Variation is highlighted in color  = **'"ff0000'(red)** along all its length
| 12| Repeat steps 8-12 for every variation's position shown in the table below |   | 
 

| Steps | Condition | Position chr X| Expected results |
| :---: | --- | --- | --- | 
| 1 | "DP == 384" | 12586950| <li> 'DEL' variation is highlighted in **'ff0000' (red)** color in variants panel and browser<li> "DP = 384" value | 
| 2 | "AC != 1" | 12586560 | <li>'DEL' variation is highlighted by **'ffff00'(yellow)** color in Variants Panel and VCF tracker in Browser <li>"AC = 2" |
| 3 | "FS >450" | 12586790 | <li>'DEL' variation is highlighted in **'008000'(green)** color in Variants Panel and  VCF tracker in Browser <li>"FS =453.737"|
| 4 | Mq >= 91.95 | <li>12585943 <li>12587867 | <li> 'INS' variation is highlighted in **'add8e6'(lightblue)**, MQ = 91.95 <li> 'DEL' variation is highlighted by **'add8e6'(lightblue)**, MQ = 92.2
| 5 | "ReadPosRankSum < -1.600"| 12589324 | <li>'SNV' variation is highlighted in **'0000ff'(blue)** <li>"ReadPosRankSum = -1.651"|
| 6 | Qd <= 0.32| <li> 12589261 <li>12591635 | <li> 'DEL' variation is highlighted in **'ffc0cb'(pink)**, Qd = 0.32 <li>'DEL' variation is highlighted in **'ffc0cb'(pink)**, Qd=0.26 |
| 7 | "DP  in [114,137]" | <li>12589600 <li>12590846 | <li>'DEL' variation is highlighted in **'ffa500'(orange)**, "DP = 114" <li>'INS' variation is highlighted in **'ffa500'(orange)**, "DP = 137" |
| 8 | "DP not in [206]"| <li>12585171  <li>12585001| <li> 'DEL' variation **isn't highlighted** in any color,  DP = 206  <li> 'DEL' variation is highlighted in **'f5f5dc'(beige)**,  DP = 216