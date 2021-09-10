import * as  geneTypes from '../../modules/render/tracks/gene/geneTypes';
import {colorModes, groupModes, readsViewTypes} from '../../modules/render/tracks/bam/modes';
import {scaleModes} from '../../modules/render/tracks/common/scaleModes';
import {displayModes} from '../../modules/render/tracks/wig/modes';
import {variantsView} from '../../modules/render/tracks/vcf/modes';
import {displayModes as featureCountsDisplayModes} from '../../modules/render/tracks/featureCounts/modes';

export default {
    defaultSettings: {
        colors: {
            'A': 0x8BC743,
            'C': 0xDD4C46,
            'G': 0x3C99C4,
            'N': 0x000000,
            'T': 0x9139C4,
            base: 0xCCD8DD,
            bg: 0xf9f9f9,
            del: 0x000000,
            ins: 0x7018de,
            pairOrientationAndInsertSize: {
                LL: 0x19a5a6,
                LR: 0xCCD8DD,
                RL: 0x12a448,
                RR: 0x273795,
                happy: 0xCCD8DD,
                long: 0xA50000,
                otherChr: 0xFFA500,
                short: 0x18009E
            },
            pairedBase: 0xACB5B9,
            spliceJunctions: 0x96B8C8,
            strand: {
                forward: 0xe69696,
                reverse: 0x9696e6
            }
        },
        defaultFeatures: {
            arrows: true,
            alignments: true,
            colorMode: colorModes.noColor,
            coverage: true,
            diffBase: true,
            geneTranscript: geneTypes.transcriptViewTypes.collapsed,
            groupMode: groupModes.defaultGroupingMode,
            ins_del: true,
            mismatches: true,
            readsViewMode: readsViewTypes.readsViewExpanded.toString(),
            shadeByQuality: false,
            softClip: true,
            spliceJunctions: false,
            variantsView: variantsView.variantsViewCollapsed,
            viewAsPairs: false,
            coverageDisplayMode: displayModes.defaultDisplayMode,
            coverageScaleMode: scaleModes.defaultScaleMode,
            coverageLogScale: false,
            coverageScaleFrom: undefined,
            coverageScaleTo: undefined,
            referenceShowTranslation: false,
            referenceShowForwardStrand: true,
            referenceShowReverseStrand: false,
            featureCountsDisplayMode: featureCountsDisplayModes.barChart,
            singleBarChartColors: true
        },
        displayAlignmentsCoverageTooltips: true,
        displayTooltips: true,
        showTrackOriginalName: true,
        showTracksHeaders: true,
        hoveringEffects: true,
        filterReads: {
            failedVendorChecks: true,
            pcrOpticalDuplicates: true,
            secondaryAlignments: false,
            supplementaryAlignments: false
        },
        gffColorByFeatureType: true,
        gffShowNumbersAminoacid: true,
        hotkeys: {
            'bam>color>firstInPairStrand': {
                hotkey: 'SHIFT + 5'
            },
            'bam>color>insertSize': {
                hotkey: 'SHIFT + 2'
            },
            'bam>color>insertSizeAndPairOrientation': {
                hotkey: 'SHIFT + 3'
            },
            'bam>color>noColor': {
                hotkey: ''
            },
            'bam>color>pairOrientation': {
                hotkey: 'SHIFT + 1'
            },
            'bam>color>readStrand': {
                hotkey: 'SHIFT + 4'
            },
            'bam>color>shadeByQuality': {
                hotkey: ''
            },
            'bam>group>chromosomeOfMate': {
                hotkey: 'SHIFT + F'
            },
            'bam>group>default': {
                hotkey: 'SHIFT + A'
            },
            'bam>group>firstInPairStrand': {
                hotkey: 'SHIFT + S'
            },
            'bam>group>pairOrientation': {
                hotkey: 'SHIFT + D'
            },
            'bam>group>readStrand': {
                hotkey: 'SHIFT + G'
            },
            'bam>readsView>automatic': {
                hotkey: 'SHIFT + V'
            },
            'bam>readsView>collapsed': {
                hotkey: 'SHIFT + X'
            },
            'bam>readsView>expanded': {
                hotkey: 'SHIFT + C'
            },
            'bam>readsView>pairs': {
                hotkey: 'SHIFT + Z'
            },
            'bam>showCoverage': {
                hotkey: 'ALT + A'
            },
            'bam>showMismatchedBases': {
                hotkey: 'ALT + Q'
            },
            'bam>showSpliceJunctions': {
                hotkey: 'ALT + Z'
            },
            'bam>showAlignments': {
                hotkey: 'ALT + W'
            },
            'bam>sort>base': {
                hotkey: 'SHIFT + E'
            },
            'bam>sort>default': {
                hotkey: 'SHIFT + Q'
            },
            'bam>sort>insertSize': {
                hotkey: 'SHIFT + Y'
            },
            'bam>sort>mappingQuality': {
                hotkey: 'SHIFT + T'
            },
            'bam>sort>strand': {
                hotkey: 'SHIFT + R'
            },
            'bam>sort>strandLocation': {
                hotkey: 'SHIFT + W'
            },
            'general>repeatLastOperation': {
                hotkey: 'ALT + R'
            },
            'layout>bookmark': {
                hotkey: 'ALT + B'
            },
            'layout>browser': {
                hotkey: ''
            },
            'layout>dataSets': {
                hotkey: 'ALT + T'
            },
            'layout>filter': {
                hotkey: 'ALT + I'
            },
            'layout>molecularViewer': {
                hotkey: 'ALT + M'
            },
            'layout>genes': {
                hotkey: 'ALT + G'
            },
            'layout>variants': {
                hotkey: 'ALT + V'
            },
            'vcf>nextVariation': {
                hotkey: 'SHIFT + ARROWRIGHT'
            },
            'vcf>previousVariation': {
                hotkey: 'SHIFT + ARROWLEFT'
            },
            'vcf>variantsView>collapsed': {
                hotkey: 'SHIFT + ]'
            },
            'vcf>variantsView>expanded': {
                hotkey: 'SHIFT + ['
            }
        },
        isDownSampling: true,
        maxBAMBP: 100000,
        maxBAMCoverageBP: 500000,
        alleleFrequencyThresholdBam: 0.1,
        spliceJunctionsFiltering: false,
        maxBpCount: 10000,
        maxFrameSize: 50,
        maxReadsCount: 300,
        minBpCount: 50,
        shortenedIntronLength: 15,
        shortenedIntronsMaximumRange: 500000,
        variantsMaximumRange: 500000,
        showCenterLine: true,
        showSoftClippedBase: true,
        isVariantsHighlighted: false,
        highlightProfile: undefined
    }
};
