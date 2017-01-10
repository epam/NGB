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
        maxHeight: 50,
        minHeight: 50,
        resizable: false,
        wig: {
            color: 0xCCD8DD,
            detailedStyleStartingAtPixelsPerBP: 7.5,
            thresholdColor: 0xFAA3A3
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
    height: 500,
    maxHeight: Infinity,
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
        height: 75
    },
    yElementOffset: 1,
    yScale: 10
};