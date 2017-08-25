export default {
    aminoacid: {
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
        start: {
            fill: 0x19A60F,
            label: {
                fill: 0xFFFFFF
            }
        },
        stop: {
            fill: 0xA60F0F,
            label: {
                fill: 0xFFFFFF
            }
        }
    },
    gene: {
        bar: {
            callout: 0x273F70,
            fill: 0x92AEE7,
            height: 10
        },
        displayDetailsThreshold: 5,
        expanded: {
            margin: 5
        },
        label: {
            fill: 0x273F70,
            font: 'bold 8pt arial'
        },
        strand: {
            arrow: {
                height: 6,
                margin: 2,
                mode: 'fill',
                thickness: 1
            }
        }
    },
    height: 100,
    fitHeightFactor: 1.5,
    histogram: {
        fill: 0x92AEE7,
        height: 30,
        thresholdGenes: 100,
        thresholdWidth: 20
    },
    levels: {
        margin: 5
    },
    maxHeight: 400,
    minHeight: 30,
    scroll: {
        alpha: 0.5,
        fill: 0x92AEE7,
        hoveredAlpha: 1,
        margin: 2,
        width: 7
    },
    transcript: {
        features: {
            border: {
                color: 0x92AEE7,
                thickness: 1
            },
            fill: {
                cds: 0x92AEE7,
                other: 0xFFFFFF,
                hoveredOther: 0xFFFFFF
            },
            strand: {
                arrow: {
                    height: 6,
                    margin: 1,
                    mode: 'stroke',
                    thickness: 2
                },
                fill: {
                    cds: 0xFFFFFF,
                    other: 0x92AEE7,
                    hoveredCds: 0xFFFFFF
                }
            }
        },
        fill: 0x92AEE7,
        height: 10,
        marginTop: 5,
        label: {
            fill: 0x273F70,
            font: 'normal 7pt arial',
            marginTop: 5
        },
        strand: {
            arrow: {
                height: 6,
                margin: 1,
                mode: 'stroke',
                thickness: 2
            },
            fill: 0x92AEE7
        },
        thickness: 1
    },
    centerLine:{
        dash: {
            fill: 0x000000,
            length: 5,
            thickness: 1
        }
    }
};