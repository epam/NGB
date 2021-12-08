import GeneConfig from '../gene/geneConfig';

export default {
    ...GeneConfig,
    animation: {
        fade: {
            duration: 0.25,
            minimum: 0.1
        },
        hover: {
            bubble: {
                duration: 0.25,
                extraRadius: 2
            }
        }
    },
    chromosomeLine: {
        fill: 0x92AEE7,
        thickness: 0
    },
    height: 70,
    maxHeight: 200,
    minHeight: 50,
    fitHeightFactor: 1.5,
    statistics: {
        bubble: {
            fill: 0x92AEE7,
            hoveredExtraRadius: 2,
            margin: 5,
            padding: 4,
            stroke: {
                color: 0x92AEE7,
                thickness: 1
            }
        },
        height: 7,
        label: {
            fill: 0x000000,
            fontFamily: 'arial',
            fontSize: '10pt',
            fontWeight: 'bold'
        }
    },

    variant: {
        allele: {
            defaultLabel: {
                fill: 0x000000,
                fontFamily: 'arial',
                fontSize: '10pt',
                fontWeight: 'normal'
            },
            detailsTooltipLabel: {
                fill: 0x000000,
                fontFamily: 'arial',
                fontSize: '9pt',
                fontWeight: 'bold'
            },
            height: 10,
            intersection: {
                detailsHeight: 12,
                horizontalMargin: 1,
                upperBorder: 10,
                verticalMargin: 0
            },
            label: {
                fill: 0x000000,
                fontFamily: 'arial',
                fontSize: '8pt',
                fontWeight: 'normal'
            },
            margin: 0
        },
        height: 12,
        multipleNucleotideVariant: {
            alpha: 1,
            color: 0xaaaaaa,
            conflict: {
                alpha: 0.125
            },
            interChromosome: {
                line: {
                    color: 0xaaaaaa,
                    margin: 10
                },
                margin: 20
            },
            label: {
                bnd: {
                    fill: 0xFFF9C4,
                    font: {
                        fill: 0x000000,
                        fontFamily: 'arial',
                        fontSize: '7pt',
                        fontWeight: 'normal'
                    }
                },
                default: {
                    fill: 0x92AEE7,
                    font: {
                        fill: 0x000000,
                        fontFamily: 'arial',
                        fontSize: '7pt',
                        fontWeight: 'normal'
                    }
                },
                del: {
                    fill: 0xC9D6F0,
                    font: {
                        fill: 0x000000,
                        fontFamily: 'arial',
                        fontSize: '7pt',
                        fontWeight: 'normal'
                    }
                },
                inv: {
                    fill: 0x92AEE7,
                    font: {
                        fill: 0x000000,
                        fontFamily: 'arial',
                        fontSize: '7pt',
                        fontWeight: 'normal'
                    }
                }
            },
            offsetLength: 5,
            onAccent: {
                other: {
                    alpha: 0.05
                }
            },
            thickness: 1
        },
        stroke: 0x000000,
        thickness: 3,
        zygosity: {
            heterozygousColor: 0xff0000,
            homozygousColor: 0x0000ff,
            unknownColor: 0x888888
        }
    },
    zones: {
        tooltip: {
            height: 15
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
