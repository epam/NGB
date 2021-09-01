import GeneConfig from '../gene/geneConfig';

export default {
    ...GeneConfig,
    barChart: {
        autoScaleGroupIndicator: {
            width: 5
        },
        background: 0xf0f0f0,
        minimumHeight: 100,
        title: {
            fill: 0x273F70,
            font: 'bold 8pt arial',
            margin: 2
        },
        margin: {
            top: 0,
            bottom: 0
        },
        bar: 0xFF0000,
        hoveredItemInfo: {
            background: {
                fill: 0xffffff,
                stroke: 0x666666,
                opacity: 0.95
            },
            label: {
                fill: 0x333333,
                font: 'normal 7pt arial',
            },
            padding: 5,
            margin: 5
        }
    }
};
