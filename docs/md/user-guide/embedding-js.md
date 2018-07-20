# Embedding NGB via iFrame controlled with JS API

NGB could be embedded into 3rd paty web application using an iFrame approach. Detailed information about embedding NGB via iFrame and controlling it with URL can be viewed [here](./embedding-url.md)

Communication with NGB window implemented with [postMessage](https://developer.mozilla.org/en-US/docs/Web/API/Window/postMessage "MDN web docks"), that is safely enables cross-origin communication.

When loaded NGB will send postMessage to `window.parent`/`window.opener` with following response:
```javascript
{
    isSuccessful: true,
    message: 'ready'
}
```
Once you receive that response, it is safe to start using postMessage with NGB

Common request structure:
```javascript
{
    //some unique string identifier that will be in returned object
    callerId: "unique string",
    //method name string
    method: "method's name",
    //object
    params: {}
}
```

`callerId` - unique ID to recognize method sending response

`method` - method's name

`params` - method's parameters object

To call JS API methods you need to send this object to NGB window via postMessage

#### Example

For following request:
```javascript
NGBiFrame.contentWindow.postMessage({
        callerId: "loadDatasetUniqueID1",
        method: "loadDataSet",
        params: {
            id: 3
        }
    }, 
    "*"
);
```

You'll get response:
```javascript
{
    callerId: "loadDatasetUniqueID1",
    isSuccessful: true,
    message: "Ok"
}
```

To listen response you can use following:
```javascript
if (window.addEventListener) {
    window.addEventListener("message", () => {
        //response handler
        //response located in event.data
    });
}
```

#### Demonstration example

Example of a JS API usage is available at [here](./embedding-js-demo.html)

This example demonstrates major capabilities of a JS API

## Methods

* [loadDataSet](#loaddataset)
* [loadTracks](#loadtracks)
* [toggleSelectTrack](#toggleselecttrack)
* [navigateToCoordinate](#navigatetocoordinate)
* [setGlobalSettings](#setglobalsettings)
* [setTrackSettings](#settracksettings)
* [setToken](#settoken)

### loadDataSet

Opens/closes dataset according to its current state

The object to be passed to NGB window:

```javascript
{
    //some unique identifier that will be in returned object
    callerId: "unique string",
    //method name
    method: "loadDataSet",
    //method params object
    params: {
        //dataset ID
        id: 4,
        //force reference switching (false by default)
        //if true no confirmation by user will be requested else confirmation dialog will appear
        forceSwitchRef: false,
    }
}
```

###  loadTracks

Loads track and opens it in a Browser panel.

The object to be passed to NGB window:

```javascript
{
    //some unique identifier that will be in returned object
    callerId: "unique string",
    //method name
    method: "loadTracks",
    //method params object
    params: {
        //array with tracks links
        tracks: [
            {
                //track's file URL or path on NGB Server according to tracks loading mode
                path: "url" or "path",
                //track's index file URL or path on NGB Server according to tracks loading mode
                index: "url" or "path",
            },
            ...
        ],
        //reference ID
        referenceId: 1,
        //force reference switching (false by default)
        //if true no confirmation by user will be requested else confirmation dialog will appear
        forceSwitchRef: false,
        //tracks loading mode
        mode: 'url' or 'ngbServer',
    }
}
```

**When track is loaded from URL or NGB Server the URL or the path becomes track's ID so further operations with track are performing by this ID.**


### toggleSelectTrack

Opens/closes track according to its current state

The object to be passed to NGB window:

```javascript
{
    //some unique identifier that will be in returned object
    callerId: "unique string",
    //method name
    method: "toggleSelectTrack",
    //method params object
    params: {
        //track ID
        //number or string when loaded by URL or path
        track: 21,
        //force reference switching (false by default)
        //if true no confirmation by user will be requested else confirmation dialog will appear
        forceSwitchRef: false,

    }
}
```

### navigateToCoordinate

The object to be passed to NGB window:

```javascript
{
    //some unique identifier that will be in returned object
    callerId: "unique string",
    //method name
    method: "navigateToCoordinate",
    //coordinates string
    params: {
        coordinates: "chromosome: start - end"
    }
}
```

Possibble `coordinates` string format are following:

`chromosome:` - opens requested chromosome on minimum zoom level

`chromosome:  position` - opens requested chromosome on requested position

`chromosome:  start - end` - opens requested chromosome on requested range

If chromosome have been already selected:

`position` - moves to requested position on current chromosome

`start - end` - moves to requested range on current chromosome

`empty string` - if chromosome is selected opens current chromosome on minimum zoom level

### setGlobalSettings

Sets global NGB settings

The object to be passed to NGB window:

```javascript
{
    //some unique identifier that will be in returned object
    callerId: "unique string",
    //method name
    method: "setGlobalSettings",
    //method params object    
    params: {
        //all possible parameters are presented in the table below
    }
}
```

| Parameters       | Settings in UI                | Comments |
|:-------------:|:------------------:|:-----:|
| | **General Tab** |
| | **_General Tab -> Tooltips_** |
| displayTooltips: true | Display tooltip |  |
| displayAlignmentsCoverageTooltips: true     | Display tooltips for alignments coverage | This parameter's setting is available only if displayTooltips = false |
| |**_General Tab -> Tracks_** |  |
| showTracksHeaders: true | Show tracks headers |  |
| hoveringEffects: true | Hovering effects |  |
| showCenterLine: true | Show center line |  |
| |**Alignments Tab** |
| |**_Alignments Tab -> Downsampling_** |
| isDownSampling: true | Downsample reads |  |
| maxReadsCount: 300 | max reads count  | This parameter's setting is available only if isDownSampling = true |
| maxFrameSize: 50 | frame size | This parameter's setting is available only if isDownSampling = true |
| |**_Alignments Tab -> Insert size_** |
| minBpCount: 50 | minimum (bp) |  |
| maxBpCount: 10000 | maximum (bp) |  |
| |**_Alignments Tab -> Track options_** |
| showSoftClippedBase: true | Show soft-clipped bases |  |
| ``` filterReads: { failedVendorChecks: true, pcrOpticalDuplicates: true, secondaryAlignments: false, supplementaryAlignments: false }``` | failedVendorChecks - Filter failed vendor checks <br> pcrOpticalDuplicates - Filter PCR/optical duplicates <br>  secondaryAlignments - Filter secondary alignments <br>  supplementaryAlignments - Filter supplementary alignments |  |
| maxBAMBP: 100000 | maximum alignments range(bp) |  |
| maxBAMCoverageBP: 500000 | maximum coverage range(bp) |  |
| |**_Alignments Tab -> Coverage options_** |
| alleleFrequencyThresholdBam: 90 | allele frequency threshold(%) |  |
| |**CFF\CTF Tab** |
| |**_CFF\CTF Tab -> Track options_** |
| gffColorByFeatureType: true | Color by feature type |  |
| gffShowNumbersAminoacid: true | Show AA numbers |  |
| |**_CFF\CTF Tab -> Shorten introns_** |
| shortenedIntronLength: 15 | Intron length(bp) |  |
| shortenedIntronsMaximumRange: 500000 | maximum range(bp) |  |
| |**VCF Tab** |
| variantsMaximumRange: 500000 | maximum variants range(bp) |  |
| |**Customize Tab** |
| |**_Customize Tab -> Layout_** |
| ```hotkeys: {'layout>variants': { hotkey: 'ALT + V' }, 'layout>filter': { hotkey: 'ALT + I' }, 'layout>browser': { hotkey: ''}, 'layout>dataSets': { hotkey: 'ALT + T' }, 'layout>bookmark': { hotkey: 'ALT + B' }, 'layout>molecularViewer': { hotkey: 'ALT + M'}}``` | 'layout>variants' - Variants Panel, 'layout>filter' - Show/hide filters, 'layout>browser' - Browser, 'layout>dataSets' - Datasets, 'layout>molecularViewer' -Molecular Viewer |  |
| |**_Customize Tab -> BAM_** |
| ```hotkeys: {'bam>color>firstInPairStrand': { hotkey: 'SHIFT + 5' }, 'bam>color>insertSize': { hotkey: 'SHIFT + 2' }, 'bam>color>insertSizeAndPairOrientation': { hotkey: 'SHIFT + 3' }, 'bam>color>noColor': { hotkey: '' }, 'bam>color>pairOrientation': { hotkey: 'SHIFT + 1' }, 'bam>color>readStrand': { hotkey: 'SHIFT + 4' }, 'bam>color>shadeByQuality': { hotkey: '' }, 'bam>group>chromosomeOfMate' : { hotkey: 'SHIFT + F' }, 'bam>group>default' : { hotkey: 'SHIFT + A' }, 'bam>group>firstInPairStrand' : { hotkey: 'SHIFT + S' }, 'bam>group>pairOrientation' : { hotkey: 'SHIFT + D' }, 'bam>group>readStrand' : { hotkey: 'SHIFT + G' }, 'bam>readsView>automatic': { hotkey: 'SHIFT + V' }, 'bam>readsView>collapsed': { hotkey: 'SHIFT + X' }, 'bam>readsView>expanded': { hotkey: 'SHIFT + C' }, 'bam>readsView>pairs': { hotkey: 'SHIFT + Z' }, 'bam>showCoverage': { hotkey: 'ALT + A' }, 'bam>showMismatchedBases': { hotkey: 'ALT + Q' }, 'bam>showSpliceJunctions': { hotkey: 'ALT + Z' }, 'bam>showAlignments': { hotkey: 'ALT + W' }, 'bam>sort>base': { hotkey: 'SHIFT + E' }, hotkey: 'SHIFT + Q' }, 'bam>sort>insertSize': { hotkey: 'SHIFT + Y' }, 'bam>sort>mappingQuality': { hotkey: 'SHIFT + T' }, 'bam>sort>strand': { hotkey: 'SHIFT + R' }, 'bam>sort>strandLocation': { hotkey: 'SHIFT + W' }, 'general>repeatLastOperation': { hotkey: 'ALT + R' }}``` | 'bam>showAlignments' - Show alignments<br> 'bam>showMismatchedBases' - Show mismatched bases<br> 'bam>showCoverage' - Show coverage<br> 'bam>showSpliceJunctions' - Show splice junctions <br> Color mode<br> 'bam>color>noColor' - No color<br> 'bam>color>pairOrientation' - By pair orientation<br> 'bam>color>insertSize' - By insert size<br> 'bam>color>insertSizeAndPairOrientation' - By insert size and pair orientation<br> 'bam>color>readStrand' - By read strand<br> 'bam>color>firstInPairStrand' - By first in pair strand<br> 'bam>color>shadeByQuality' - Shade by quality<br> <br> Sort<br> 'bam>sort>default' – Default<br> 'bam>sort>strandLocation' - By start location<br> 'bam>sort>base' - By base<br> 'bam>sort>strand' - By strand<br> 'bam>sort>mappingQuality' - By mapping quality<br> 'bam>sort>insertSize' - By insert size<br> <br> Group<br> 'bam>group>default' – Default<br> 'bam>group>firstInPairStrand' - By first in pair strand<br> 'bam>group>pairOrientation' -  By pair orientation<br> 'bam>group>chromosomeOfMate' - By chromosome of mate<br> 'bam>group>readStrand' - By read strand<br> <br> Reads view<br> 'bam>readsView>pairs' - View as pairs<br> 'bam>readsView>collapsed' - Collapsed<br> 'bam>readsView>expanded' - Expanded<br> 'bam>readsView>automatic' - Automatic<br> <br> Other<br> 'general>repeatLastOperation' - Repeat last operation||
| |**_Customize Tab -> GENE_** |
| ```hotkeys: {'gene>transcript>expanded': { hotkey: ''}, 'gene>transcript>collapsed': { hotkey: '' }}``` | Transcript view <br> 'gene>transcript>expanded' - Expanded <br> 'gene>transcript>collapsed' - Collapsed |  |
| |**_Customize Tab -> VCF_** |
| ```hotkeys: {  'vcf>nextVariation':{   hotkey: 'SHIFT + ARROWRIGHT'  },  'vcf>previousVariation':{   hotkey: 'SHIFT + ARROWLEFT' }, 'vcf>variantsView>collapsed':{  hotkey: 'SHIFT + ]' }, 'vcf>variantsView>expanded':{  hotkey: 'SHIFT + [' } }``` | 'vcf>nextVariation' - Next variation <br> 'vcf>previousVariation' - Previous variation <br>  <br> Variants view <br> 'vcf>variantsView>collapsed' - Collapsed <br> 'vcf>variantsView>expanded' - Expanded <br>  |  |

### setTrackSettings

Sets requested opened track settings

The object to be passed to NGB window:

```javascript
{
    //some unique identifier that will be in returned object
    callerId: "unique string",
    //method name
    method: "setTrackSettings",
    //method params object    
    params: {
        //track ID
        id: 59,
        settings: [
            //all possible parameters are presented in the table below
            {...},
            ...
        ]
    }
}
```

| Settings       | Settings in UI                | Comments |
|:-------------:|:------------------:|:-----:|
| | **Vcf track** |
| ```{name:"vcf>variantsView>collapsed"} {name: "vcf>variantsView>expanded"}``` | Variants View <br> "vcf>variantsView>collapsed" -Collapsed <br> "vcf>variantsView>expanded" - Expanded <br>  |  |
| | **Referense track** |
| ```{name: "reference>showTranslation", value: true}, {name: "reference>showForwardStrand", value: true}, {name: "reference>showReverseStrand", value: true}```  | General <br> "reference>showTranslation" - Show translation <br> "reference>showForwardStrand" - Show forward strand <br> "reference>showReverseStrand" - Show reverse strand <br>  |  |
| | **Wig  track** |
| ```{name: "coverage>scale>default", value: false}, {name: "coverage>scale>manual", value: true,  extraOptions: {     from: 10,     to: 150 }}, {name: "coverage>scale>log", value: true} ``` | Scale <br> "coverage>scale>default" - Default scale <br> "coverage>scale>manual" - Manual scale <br> "coverage>scale>log" - Log scale <br>  | If "coverage>scale>manual" is set and value set to true then `extraOptions: {     from: 10,     to: 150 }` setting required.  <br> If value is set to false then “Scale” setting will be reseted to default. Exclusion: "coverage>scale>log"  |
| | **BAM  track** |
| ```{name: "bam>color>noColor", value: false}, {name: "bam>color>pairOrientation", value: false}, {name: "bam>color>insertSize", value: false}, {name: "bam>color>insertSizeAndPairOrientation", value: false}, {name: "bam>color>readStrand", value: false}, {name: "bam>color>firstInPairStrand", value: true}, {name: "bam>color>shadeByQuality", value: true}```  | Color mode <br> "bam>color>noColor" - No color <br> "bam>color>pairOrientation" - By pair orientation  <br> "bam>color>insertSize" - By insert size  <br> "bam>color>insertSizeAndPairOrientation" - By insert size and pair orientation  <br> "bam>color>readStrand" - By read strand  <br> "bam>color>firstInPairStrand" - By first in pair strand  <br> "bam>color>shadeByQuality" - Shade by quality <br>  | If value is set to false then setting “Color mode” will be reseted to default. Exclusion: "bam>color>shadeByQuality"  |
| ```{name: "bam>group>default", value: false}, {name: "bam>group>firstInPairStrand", value: true}, {name: "bam>group>pairOrientation", value: false}, {name: "bam>group>chromosomeOfMate", value: false}, {name: "bam>group>readStrand", value: false}``` | Group <br> "bam>group>default" - Default  <br> "bam>group>firstInPairStrand" - By first in pair strand s <br> "bam>group>pairOrientation" - By pair orientation  <br> "bam>group>chromosomeOfMate" - By chromosome of mate  <br> "bam>group>readStrand" - By read strand  <br> ) | If value is set to false then Group setting will be reseted to default. |
| ```{name: "bam>readsView>collapsed"}, {name: "bam>readsView>expanded"}, {name: "bam>readsView>automatic"}, {name: "bam>readsView>pairs", value: true}``` | Reads view <br> "bam>readsView>collapsed" - Collapsed  <br> "bam>readsView>expanded" - Expanded  <br> "bam>readsView>automatic" - Automatic  <br> "bam>readsView>pairs" - View as pairs  <br>  |  |
| ```{name: "bam>sort>default"}, {name: "bam>sort>strandLocation"}, {name: "bam>sort>base"}, {name: "bam>sort>strand"}, {name: "bam>sort>mappingQuality"}, {name: "bam>sort>insertSize"}``` | Sort <br> "bam>sort>default" - Default <br> "bam>sort>strandLocation" - By start location <br> "bam>sort>base" - By base <br> "bam>sort>strand" - By strand <br> "bam>sort>mappingQuality" - By mapping quality <br> "bam>sort>insertSize" - By insert size <br>  |  |
| ```{name: "bam>showAlignments", value: true}, {name: "bam>showMismatchedBases", value: true}, {name: "bam>showCoverage", value: true}, {name: "bam>showSpliceJunctions", value: true}``` | General <br> "bam>showAlignments" - Show alignments  <br> "bam>showMismatchedBases" - Show mismatched bases  <br> "bam>showCoverage" - Show coverage  <br> "bam>showSpliceJunctions" - Show splice junctions  <br>  |  |
| ```{name: "coverage>scale>default", value: false}, {name: "coverage>scale>manual", value: true, extraOptions: {     from: 10,     to: 150}}, {name: "coverage>scale>log", value: true}``` | Scale <br> "coverage>scale>default" - Default scale <br> "coverage>scale>manual" - Manual scale <br> "coverage>scale>log" - Log scale <br>  |  If "coverage>scale>manual" is set and value set to true then `extraOptions: {     from: 10,     to: 150 }` setting required.  <br> If value is set to false then “Scale” setting will be reseted to default. Exclusion: "coverage>scale>log"  |
| | **Gene  track** |
| ```{name: "gene>transcript>collapsed"}, {name: "gene>transcript>expanded"}``` | Transcript View <br> "gene>transcript>collapsed" - Collapsed <br> "gene>transcript>expanded" - Expanded  <br>  |  |
| ```{name: "shortenIntrons", value: true}``` | Shorten introns |  |


## Authorization
If authorization is enabled for NGB server each call to API should include a valid JWT token either in header (**"Authorization: Bearer {TOKEN_VALUE}"**) or in cookies.
If token is present in localStorage it will be added to HTTP header to all NGB server API calls 

### setToken

Set value of a token item in a localStorage.

Token will be added into HTTP header `Authorization` with value `Bearer {params.token}`

The object to be passed to NGB window:

```javascript
{
    //some unique identifier that will be in returned object
    callerId: "unique string",
    //method name
    method: "setToken",
    //coordinates string
    params: {
        token: token
    }
}
```

