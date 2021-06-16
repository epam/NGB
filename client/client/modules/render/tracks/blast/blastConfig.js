const MIN_HEIGHT = 50;
const MAX_HEIGHT = 100;

export default {
    height: MIN_HEIGHT,
    maxHeight: () => MAX_HEIGHT,
    minHeight: () => MIN_HEIGHT,
    centerLine:{
        dash: {
            fill: 0x000000,
            length: 5,
            thickness: 1
        }
    },
    strandMarker: {
        width: 10,
    },
    scroll: {
        alpha: 0.5,
        fill: 0x92AEE7,
        hoveredAlpha: 1,
        margin: 2,
        width: 13
    },
    sequence: {
        detailsThreshold: 0.5,
        height: 15,
        margin: 5,
        color: 0xCCD8DD,
        mismatch: {
            label: {
                fill: 0xFFFFFF,
                font: 'normal 7pt arial',
                align: 'center'
            }
        },
        notAligned: {
            color: 0xFF0000,
            width: 20,
            margin: 5,
            label: {
                fill: 0xFF0000,
                font: 'normal 8pt arial',
                align: 'center'
            }
        }
    }
};
