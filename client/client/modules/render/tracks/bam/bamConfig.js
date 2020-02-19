export default {
    arrowOffset: 4,
    baseOffset: 2,
    centerLine:{
        dash: {
            fill: 0x000000,
            length: 5,
            thickness: 1
        }
    },
    coverage: {
        area: {
            dividers: 2,
            maximum: null,
            minimum: 0,
            thresholdMax: null,
            thresholdMin: null
        },
        divider: {
            color: 0x777777
        },
        height: 50,
        logScaleIndicator: {
            alpha: 0.8,
            label: {
                fill: 0xFFFFFF,
                font: 'normal 8pt arial'
            },
            margin: 1,
            padding: 2,
            fill: 0xCCD8DD
        },
        maxHeight: 50,
        minHeight: 50,
        wig: {
            color: 0xCCD8DD,
            lineColor: 0xA2ABAF,
            detailedStyleStartingAtPixelsPerBP: 7.5,
            thresholdColor: 0xFAA3A3,
            lineThresholdColor: 0xF87272
        }
    },
    downSampling:{
        area: {
            height: 5
        },
        indicator: {
            color: 0x000000,
            height: 2
        }
    },
    groupNames: {
        background: {
            alpha: 0.75,
            fill: 0xFFFFFF,
            stroke: 0x000000,
            strokeThickness: 1
        },
        label: {
            fill: 0x000000,
            font: 'normal 7pt arial'
        },
        margin: {
            x: 5,
            y: 2
        },
        oddFill: 0xF3F3F3,
        offset: 10,
        separator: {
            hideThreshold: 2,
            stroke: 0x000000,
            strokeAlpha: .25,
            strokeThickness: 1
        }
    },
    height: (state, config) => {
        if (state.alignments) {
            return 250;
        }
        let height = 0;
        if (state.coverage) {
            height += config.coverage.height;
        }
        if (state.spliceJunctions) {
            if (state.sashimi) {
                height += config.spliceJunctions.sashimi.height;
            } else {
                height += config.spliceJunctions.height;
            }
        }
        return height;
    },
    fitHeightFactor: (state) => {
        if (state.alignments) {
            return 6;
        }
        let height = 0;
        if (state.coverage) {
            height += 1;
        }
        if (state.spliceJunctions) {
            height += 1;
        }
        return height;
    },
    defaultHeight: 250,
    maxHeight: (state, config) => {
        if (state.alignments) {
            return Infinity;
        }
        let maxHeight = 0;
        if (state.coverage) {
            maxHeight += config.coverage.height;
        }
        if (state.spliceJunctions) {
            if (state.sashimi) {
                maxHeight += config.spliceJunctions.sashimi.height;
            } else {
                maxHeight += config.spliceJunctions.height;
            }
        }
        return maxHeight;
    },
    minHeight: (state, config) => {
        if (state.alignments) {
            return config.coverage.height + config.spliceJunctions.height + 20;
        }
        let minHeight = 0;
        if (state.coverage) {
            minHeight += config.coverage.height;
        }
        if (state.spliceJunctions) {
            if (state.sashimi) {
                minHeight += config.spliceJunctions.sashimi.height;
            } else {
                minHeight += config.spliceJunctions.height;
            }
        }
        return minHeight;
    },
    regions: {
        height: 50,
        lines: {
            alpha: 0.5,
            alphaMax: 0.9,
            fill: 0x92AEE7,
            step: 5,
            thickness: 1
        }
    },
    requestDebounce: 0.5,
    requestPreCache: 1,
    scroll: {
        alpha: 0.5,
        fill: 0x92AEE7,
        hoveredAlpha: 1,
        margin: 2,
        minHeight: 25,
        width: 7
    },
    spliceJunctions: {
        arc: {
            offset: {
                bottom: 7,
                top: 7
            }
        },
        border: {
            stroke: 0x777777,
            thickness: 1
        },
        divider: {
            stroke: 0x444444,
            thickness: 1
        },
        height: 75,
        sashimi: {
            border: {
                stroke: 0x777777,
                thickness: 1
            },
            divider: {
                stroke: 0x444444,
                thickness: 1
            },
            coverage: {
                area: {
                    dividers: 2,
                    maximum: null,
                    minimum: 0,
                    thresholdMax: null,
                    thresholdMin: null
                },
                divider: {
                    color: 0x777777
                },
                height: 75,
                logScaleIndicator: {
                    alpha: 0.8,
                    label: {
                        fill: 0xFFFFFF,
                        font: 'normal 8pt arial'
                    },
                    margin: 1,
                    padding: 2,
                    fill: 0xCCD8DD
                },
                maxHeight: 75,
                minHeight: 75,
                wig: {
                    color: 0xe69696,
                    lineColor: 0xe69696,
                    detailedStyleStartingAtPixelsPerBP: Infinity,
                    thresholdColor: 0xe69696,
                    lineThresholdColor: 0xe69696
                }
            },
            height: 150,
            levelHeight: 20,
            color: 0xE21F27,
            label: {
                fill: 0xE21F27,
                font: '8pt arial'
            }
        }
    },
    yElementOffset: 1,
    yScale: 10
};
