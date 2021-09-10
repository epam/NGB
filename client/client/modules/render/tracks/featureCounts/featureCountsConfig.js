import GeneConfig from '../gene/geneConfig';

export default {
    ...GeneConfig,
    barChart: {
        autoScaleGroupIndicator: {
            width: 5
        },
        background: 0xf0f0f0,
        bottomBorder: 0x333333,
        minimumHeight: 100,
        title: {
            font: {
                fill: 0x273F70,
                fontFamily: 'arial',
                fontSize: '8pt',
                fontWeight: 'bold',
            },
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
                fontFamily: 'arial',
                fontSize: '7pt',
                fontWeight: 'normal'
            },
            padding: 5,
            margin: 5
        }
    }
};
