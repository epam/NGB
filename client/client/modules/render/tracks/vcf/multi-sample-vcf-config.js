import {variantsView} from './modes';
import vcfConfig from './vcfConfig';

const coverageHeight = 60;
const sampleHeight = 60;
const collapsedSampleHeight = 13;

const samplesToDisplayByDefault = 3;
const collapsedSamplesToDisplayByDefault = Math.floor(
    samplesToDisplayByDefault * (sampleHeight / collapsedSampleHeight)
);
const maxSamplesToDisplay = 16;

function getSampleHeight (state) {
    if (
        !state ||
        state.variantsView === variantsView.variantsViewExpanded
    ) {
        return sampleHeight;
    }
    return collapsedSampleHeight;
}

function getSamplesToDisplayByDefault (state) {
    if (
        !state ||
        state.variantsView === variantsView.variantsViewExpanded
    ) {
        return samplesToDisplayByDefault;
    }
    return collapsedSamplesToDisplayByDefault;
}

function defaultHeight (state) {
    return coverageHeight + getSamplesToDisplayByDefault(state) * getSampleHeight(state);
}

export default {
    ...vcfConfig,
    height: defaultHeight(),
    defaultHeight,
    getSampleHeight,
    maxHeight: (state, config, track) => {
        const samplesCount = track && track.samples
            ? track.samples.length
            : Infinity;
        if (state && state.variantsView === variantsView.variantsViewCollapsed) {
            return coverageHeight + samplesCount * getSampleHeight(state);
        }
        return coverageHeight + Math.min(maxSamplesToDisplay, samplesCount) * getSampleHeight(state);
    },
    minHeight: (state) => coverageHeight + getSampleHeight(state),
    coverageHeight,
    sampleHeight,
    collapsedSampleHeight,
    sample: {
        divider: {
            stroke: 0xdddddd
        },
        label: {
            margin: 5,
            font: {
                fill: 0x666666,
                fontFamily: 'arial',
                fontSize: '7pt',
                fontWeight: 'bold'
            }
        }
    },
    collapsed: {
        bar: {
            height: collapsedSampleHeight - 1
        },
        hoveredAlpha: 1,
        nucleotide: {
            threshold: 10,
            font: {
                fill: 0xFFFFFF,
                fontFamily: 'arial',
                fontSize: '6pt',
                fontWeight: 'normal'
            }
        },
        variation: {
            label: {
                margin: 1,
                font: {
                    fill: 0x333333,
                    fontFamily: 'arial',
                    fontSize: '6pt',
                    fontWeight: 'bold'
                }
            },
            line: {
                stroke: 0x666666
            },
            cutout: {
                radius: 1,
                color: 0x666666
            },
            highlight: {
                lineHeight: 5
            }
        }
    },
    coverage: {
        color: 0x92AEE7,
        barsThreshold: 2,
        lineColor: 0x697EA6
    },
    scroll: {
        alpha: 0.5,
        fill: 0x92AEE7,
        hoveredAlpha: 1,
        margin: 2,
        minHeight: 25,
        width: 13
    }
};
