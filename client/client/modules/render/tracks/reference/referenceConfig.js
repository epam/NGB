export default {
    height: 100,
    minHeight: 20,
    nucleotidesHeight: 20,
    aminoAcidsHeight: 10,
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
    aminoacid: {
        labelDisplayAfterPixelsPerBp: 7,
        even: {
            fill: 0x6086D1,
            label: {
                fill: 0xFFFFFF
            }
        },
        label: {
            defaultStyle: {
                fill: 0x030E24,
                font: 'normal 7pt arial'
            },
            margin: 2
        },
        number: {
            fill: 0x030E24,
            font: 'normal 7pt arial'
        },
        odd: {
            fill: 0x3D62AC,
            label: {
                fill: 0xFFFFFF
            }
        },
        stop: {
            fill: 0xF47B82,
            oddFill: 0xED1C24,
            label: {
                fill: 0xFFFFFF,
                font: 'normal 10pt arial'
            }
        },
        start: {
            fill: 0xC8BFE7,
            oddFill: 0x8774CB,
            label: {
                fill: 0xFFFFFF,
            }
        }
    },
    centerLine:{
        dash: {
            fill: 0x000000,
            length: 5,
            thickness: 1
        }
    }
};
