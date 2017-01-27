import * as  geneTypes  from '../../../../../modules/render/tracks/gene/geneTypes';
import {colorModes, groupModes, readsViewTypes} from '../../../../../modules/render/tracks/bam/modes/';
import {variantsView} from '../../../../../modules/render/tracks/vcf/modes/';

export default [
    {
        label: 'LAYOUT',
        name: 'layout',
        subItems: [
            {
                hotkey: '',
                label: 'Variants Panel',
                name: 'layout>variants',
                type: 'item',
            },
            {
                type: 'item',
                name: 'layout>filter',
                label: 'Filter Panel',
                hotkey: ''
            },
            {
                type: 'item',
                name: 'layout>browser',
                label: 'Browser',
                hotkey: ''
            },
            {
                type: 'item',
                name: 'layout>dataSets',
                label: 'Datasets',
                hotkey: ''
            },
            {
                type: 'item',
                name: 'layout>bookmark',
                label: 'Sessions',
                hotkey: ''
            },
            {
                type: 'item',
                name: 'layout>molecularViewer',
                label: 'Molecular Viewer',
                hotkey: ''
            }
        ],
        type: 'category'
    },
    {
        label: 'BAM',
        name: 'bam',
        subItems: [
            {
                type: 'item',
                name: 'bam>showMismatchedBases',
                label: 'Show mismatched bases',
                hotkey: '',
                byDefault: {
                    type: 'checkbox',
                    name: 'mismatches',
                    model: true
                }
            },
            {
                type: 'item',
                name: 'bam>showCoverage',
                label: 'Show coverage',
                hotkey: '',
                byDefault: {
                    type: 'checkbox',
                    name: 'coverage',
                    model: true
                }
            },
            {
                type: 'item',
                name: 'bam>showSpliceJunctions',
                label: 'Show splice junctions',
                hotkey: '',
                byDefault: {
                    type: 'checkbox',
                    name: 'spliceJunctions',
                    model: true
                }
            },
            {
                type: 'group',
                name: 'bam>color',
                label: 'Color mode',
                subItems: [
                    {
                        type: 'item',
                        name: 'bam>color>noColor',
                        label: 'No color',
                        hotkey: '',
                        byDefault: {
                            type: 'radio',
                            name: colorModes.noColor,
                            group: 'colorMode',
                            model: {
                                value: colorModes.noColor
                            }
                        }
                    },
                    {
                        type: 'item',
                        name: 'bam>color>pairOrientation',
                        label: 'By pair orientation',
                        hotkey: '',
                        colors: [
                            {
                                name: 'pairOrientationAndInsertSize.LL',
                                label: 'LL'
                            },
                            {
                                name: 'pairOrientationAndInsertSize.RR',
                                label: 'RR'
                            },
                            {
                                name: 'pairOrientationAndInsertSize.RL',
                                label: 'RL'
                            }
                        ],
                        byDefault: {
                            type: 'radio',
                            name: 'pairOrientation',
                            group: 'colorMode',
                            model: {
                                value: colorModes.noColor
                            }
                        }

                    },
                    {
                        type: 'item',
                        name: 'bam>color>insertSize',
                        label: 'By insert size',
                        hotkey: '',
                        colors: [
                            {
                                name: 'pairOrientationAndInsertSize.long',
                                label: 'More'
                            },
                            {
                                name: 'pairOrientationAndInsertSize.short',
                                label: 'Less'
                            },
                            {
                                name: 'pairOrientationAndInsertSize.otherChr',
                                label: 'Chr'
                            }
                        ],
                        byDefault: {
                            type: 'radio',
                            name: colorModes.byInsertSize,
                            group: 'colorMode',
                            model: {
                                value: colorModes.noColor
                            }
                        }
                    },
                    {
                        type: 'item',
                        name: 'bam>color>insertSizeAndPairOrientation',
                        label: 'By insert size and pair orientation',
                        hotkey: '',
                        byDefault: {
                            type: 'radio',
                            name: colorModes.byInsertSizeAndPairOrientation,
                            group: 'colorMode',
                            model: {
                                value: colorModes.noColor
                            }
                        }
                    },
                    {
                        type: 'item',
                        name: 'bam>color>readStrand',
                        label: 'By read strand',
                        hotkey: '',
                        colors: [
                            {
                                name: 'strandDirection.l',
                                label: 'Forward'
                            },
                            {
                                name: 'strandDirection.r',
                                label: 'Reverse'
                            }
                        ],
                        byDefault: {
                            type: 'radio',
                            name: colorModes.byReadStrand,
                            group: 'colorMode',
                            model: {
                                value: colorModes.noColor
                            }
                        }
                    },
                    {
                        type: 'item',
                        name: 'bam>color>firstInPairStrand',
                        label: 'By first in pair strand',
                        hotkey: '',
                        byDefault: {
                            type: 'radio',
                            name: colorModes.byFirstInPairStrand,
                            group: 'colorMode',
                            model: {
                                value: colorModes.noColor
                            }
                        }
                    },
                    {
                        type: 'item',
                        name: 'bam>color>shadeByQuality',
                        label: 'Shade by quality',
                        hotkey: '',
                        byDefault: {
                            type: 'checkbox',
                            name: 'shadeByQuality',
                            model: false
                        }
                    }
                ]
            },
            {
                type: 'group',
                name: 'bam>sort',
                label: 'Sort',
                subItems: [
                    {
                        type: 'item',
                        name: 'bam>sort>default',
                        label: 'Default',
                        hotkey: ''
                    },
                    {
                        type: 'item',
                        name: 'bam>sort>strandLocation',
                        label: 'By start location',
                        hotkey: ''
                    },
                    {
                        type: 'item',
                        name: 'bam>sort>base',
                        label: 'By base',
                        hotkey: ''
                    },
                    {
                        type: 'item',
                        name: 'bam>sort>strand',
                        label: 'By strand',
                        hotkey: ''
                    },
                    {
                        type: 'item',
                        name: 'bam>sort>mappingQuality',
                        label: 'By mapping quality',
                        hotkey: ''
                    },
                    {
                        type: 'item',
                        name: 'bam>sort>insertSize',
                        label: 'By insert size',
                        hotkey: ''
                    }
                ]
            },
            {
                type: 'group',
                name: 'bam>group',
                label: 'Group',
                subItems: [
                    {
                        type: 'item',
                        name: 'bam>group>default',
                        label: 'Default',
                        hotkey: '',
                        byDefault: {
                            type: 'radio',
                            group: 'groupMode',
                            name: groupModes.defaultGroupingMode,
                            model: {
                                value: groupModes.defaultGroupingMode
                            }
                        }
                    },
                    {
                        type: 'item',
                        name: 'bam>group>firstInPairStrand',
                        label: 'By first in pair strand',
                        hotkey: '',
                        byDefault: {
                            type: 'radio',
                            group: 'groupMode',
                            name: groupModes.groupByFirstInPairMode,
                            model: {
                                value: groupModes.defaultGroupingMode
                            }
                        }
                    },
                    {
                        type: 'item',
                        name: 'bam>group>pairOrientation',
                        label: 'By pair orientation',
                        hotkey: '',
                        byDefault: {
                            type: 'radio',
                            group: 'groupMode',
                            name: groupModes.groupByPairOrientationMode,
                            model: {
                                value: groupModes.defaultGroupingMode
                            }
                        }
                    },
                    {
                        type: 'item',
                        name: 'bam>group>chromosomeOfMate',
                        label: 'By chromosome of mate',
                        hotkey: '',
                        byDefault: {
                            type: 'radio',
                            group: 'groupMode',
                            name: groupModes.groupByChromosomeOfMateMode,
                            model: {
                                value: groupModes.defaultGroupingMode
                            }
                        }
                    },
                    {
                        type: 'item',
                        name: 'bam>group>readStrand',
                        label: 'By read strand',
                        hotkey: '',
                        byDefault: {
                            type: 'radio',
                            group: 'groupMode',
                            name: groupModes.groupByReadStrandMode,
                            model: {
                                value: groupModes.defaultGroupingMode
                            },
                            conflicts: ['bam>readsView>pairs']
                        }
                    }
                ]
            },
            {
                type: 'group',
                name: 'bam>readsView',
                label: 'Reads view',
                subItems: [
                    {
                        type: 'item',
                        name: 'bam>readsView>pairs',
                        label: 'View as pairs',
                        hotkey: '',
                        byDefault: {
                            type: 'checkbox',
                            name: 'viewAsPairs',
                            model: true,
                            conflicts: ['bam>group>readStrand']
                        }
                    },
                    {
                        type: 'item',
                        name: 'bam>readsView>collapsed',
                        label: 'Collapsed',
                        hotkey: '',
                        byDefault: {
                            type: 'radio',
                            name: readsViewTypes.readsViewCollapsed.toString(),
                            group: 'readsViewMode',
                            model: {
                                value: 'expanded'
                            }
                        }
                    },
                    {
                        type: 'item',
                        name: 'bam>readsView>expanded',
                        label: 'Expanded',
                        hotkey: '',
                        byDefault: {
                            type: 'radio',
                            name: readsViewTypes.readsViewExpanded.toString(),
                            group: 'readsViewMode',
                            model: {
                                value: 'expanded'
                            }
                        }
                    },
                    {
                        type: 'item',
                        name: 'bam>readsView>automatic',
                        label: 'Automatic',
                        hotkey: '',
                        byDefault: {
                            type: 'radio',
                            name: readsViewTypes.readsViewAutomatic.toString(),
                            group: 'readsViewMode',
                            model: {
                                value: 'expanded'
                            }
                        }
                    }
                ]
            },
            {
                type: 'group',
                name: 'bam>other',
                label: 'Other',
                subItems: [
                    {
                        type: 'item',
                        name: 'general>repeatLastOperation',
                        label: 'Repeat last operation',
                        hotkey: ''
                    }
                ]
            }

        ],
        type: 'category'
    },
    {
        label: 'GENE',
        name: 'gene',
        subItems: [
            {
                type: 'group',
                name: 'gene>transcript',
                label: 'Transcript view',
                subItems: [
                    {
                        type: 'item',
                        name: 'gene>transcript>expanded',
                        label: 'Expanded',
                        hotkey: '',
                        byDefault: {
                            type: 'radio',
                            name: geneTypes.transcriptViewTypes.expanded,
                            group: 'geneTranscript',
                            model: {}
                        }
                    },
                    {
                        type: 'item',
                        name: 'gene>transcript>collapsed',
                        label: 'Collapsed',
                        hotkey: '',
                        byDefault: {
                            type: 'radio',
                            name: geneTypes.transcriptViewTypes.collapsed,
                            group: 'geneTranscript',
                            model: {}
                        }
                    }
                ]
            }
        ],
        type: 'category'
    },
    {
        label: 'VCF',
        name: 'vcf',
        subItems: [
            {
                type: 'item',
                name: 'vcf>nextVariation',
                label: 'Next variation',
                hotkey: ''
            },
            {
                type: 'item',
                name: 'vcf>previousVariation',
                label: 'Previous variation',
                hotkey: ''
            },
            {
                type: 'group',
                name: 'vcf>variantsView',
                label: 'Variants view',
                subItems: [
                    {
                        type: 'item',
                        name: 'vcf>variantsView>expanded',
                        label: 'Expanded',
                        hotkey: '',
                        byDefault: {
                            type: 'radio',
                            name: variantsView.variantsViewExpanded,
                            group: 'variantsView',
                            model: {}
                        }
                    },
                    {
                        type: 'item',
                        name: 'vcf>variantsView>collapsed',
                        label: 'Collapsed',
                        hotkey: '',
                        byDefault: {
                            type: 'radio',
                            name: variantsView.variantsViewCollapsed,
                            group: 'variantsView',
                            model: {}
                        }
                    }
                ]
            }
        ],
        type: 'category'
    }
];









