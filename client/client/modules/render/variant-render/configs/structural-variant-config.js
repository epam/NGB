export default {
    chromosome: {
        height: 20,
        margin: 2,
        label: {
            fill: 0x000000,
            fontFamily: 'arial',
            fontSize: '8pt',
            fontWeight: 'bold'
        }
    },
    gene: {
        height: 20,
        margin: 0,
        label: {
            fill: 0x273F70,
            fontFamily: 'arial',
            fontSize: '8pt',
            fontWeight: 'bold'
        }
    },
    breakpoint: {
        width: 15,
        offset: 6,
        thickness: 2,
        color: 0xff0000
    },
    domains: {
        mainColors: [
            0xf48fb1,
            0x829BCD,
            0xDF905C,
            0x9ED2AA,
            0xc9d6f0,
            0xf3ceb6,
            0xd8efdd,
            0xfff9c4,
            0xdce775
        ],
        additionalColors: [
            0xDF6868,
            0xA468E0,
            0x68E09C,
            0x68ACE0
        ],
        empty: {
            color: 0xdddddd,
            space: 3
        }
    },
    breaks: {
        margin: {
            x: 0,
            y: 0
        },
        amplitude: {
            x: 1.5,
            y: 10
        },
        thickness: 1,
        bubble: {
            radius: {
                outer: 2,
                inner: 1
            },
            fill: 0xFFFFFF
        }
    },
    exon: {
        height: 12,
        margin: 2,
        label: {
            fill: 0x000000,
            fontFamily: 'arial',
            fontSize: '6pt',
            fontWeight: 'normal'
        }
    },
    strand: {
        margin: 2,
        height: 5,
        fill: 0x000000,
        arrow: {
            height: 5,
            margin: 1,
            mode: 'stroke',
            thickness: 2
        }
    },
    edges: {
        height: 3,
        margin: 0
    },
    transcript: {
        height: 10,
        margin: 2,
        label: {
            fill: 0x000000,
            fontFamily: 'arial',
            fontSize: '6pt',
            fontWeight: 'normal'
        },
        mainLabel: {
            fill: 0x777777,
            fontFamily: 'arial',
            fontSize: '8pt',
            fontWeight: 'normal'
        },
        radio: {
            margin: 10,
            radius: 6
        }
    },
    transcriptName: {
        height: 12,
        label: {
            fill: 0x273F70,
            fontFamily: 'arial',
            fontSize: '6pt',
            fontWeight: 'normal'
        }
    },
    transcriptLabel: {
        height: 20
    },
    legend: {
        height: 15,
        bar: {
            width: 10,
            height: 10
        },
        margin: 2,
        label: {
            fill: 0x000000,
            fontFamily: 'arial',
            fontSize: '8pt',
            fontWeight: 'normal'
        },
        mainLabel: {
            fill: 0x777777,
            fontFamily: 'arial',
            fontSize: '8pt',
            fontWeight: 'normal'
        }
    },
    legendsLabel: {
        height: 20
    },
    zones: {
        margin: 0
    }
};
