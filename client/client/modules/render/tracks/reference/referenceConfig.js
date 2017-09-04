export default {
    height: 41,
    nucleotidesHeight: 20,
    fitHeightFactor: 1,
    largeScale: {
        'A': 0x8BC743,
        'C': 0xDD4C46,
        'G': 0x3C99C4,
        'N': 0x000000,
        'T': 0x9139C4,
        labelDisplayAfterPixelsPerBp: 10,
        labelStyle: {
            fill: 0xFFFFFF,
            font: '24px'
        },
        separateBarsAfterBp: 5
    },
    lowScale: {
        color1: 0x0080FF,
        color2: 0xFF8000,
        sensitiveValue: 0.5
    },
    noGCContent: {
        labelStyle: {
            fill: 0x666666,
            font: 'normal 10pt arial'
        },
        text: 'GC content is not provided.'
    },
    reference: {},
    centerLine:{
        dash: {
            fill: 0x000000,
            length: 5,
            thickness: 1
        }
    }
};
