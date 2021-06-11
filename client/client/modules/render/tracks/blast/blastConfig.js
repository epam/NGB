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
    sequence: {
        height: 15,
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
