export default {
    nucleotide:{
        size:{
            width: {
                maximum: 20,
                minimum: 15
            },
            height: 20
        },
        colors:{
            'A': 0x8BC743,
            'G': 0x3C99C4,
            'C': 0xDD4C46,
            'T': 0x9139C4,
            'N': 0x000000
        },
        label: {
            font: 'normal 8pt arial',
            fill: 0xFFFFFF
        },
        margin: {
            x: 1,
            y: 5
        }
    },
    sequences:{
        margin:{
            horizontal: 10,
            vertical: 10
        },
        modifiedRange:{
            width: 100,
            ruler: 0x2a9af5
        }
    },
    aminoacids:{
        height: 12,
        margin: 3,
        label:{
            normal: {
                fill: 0x000000,
                font: 'normal 8pt arial'
            },
            modified: {
                fill: 0xFF0000,
                font: 'bold 8pt arial'
            }
        }
    },
    strand: {
        margin: 2,
        height: 5,
        fill : 0x3D62AC,
        arrow: {
            height: 5,
            margin: 1,
            mode: 'stroke',
            thickness: 2
        }
    }
};