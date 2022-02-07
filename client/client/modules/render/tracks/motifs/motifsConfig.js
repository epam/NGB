const positive = 'POSITIVE';
const negative = 'NEGATIVE';

export default {
    height: 31,
    minHeight: 14,
    centerLine:{
        dash: {
            fill: 0x000000,
            length: 5,
            thickness: 1
        }
    },
    matches: {
        defaultColor: {
            [positive]: 0x3C99C4,
            [negative]: 0xDD4C46
        },
        height: 10,
        margin: 2,
        detailsThresholdPx: 5,
        strand: {
            arrow: {
                height: 6,
                margin: 2,
                mode: 'stroke',
                thickness: 2
            },
        }
    },
    scroll:{
        alpha: 0.5,
        fill: 0x92AEE7,
        hoveredAlpha: 1,
        margin: 2,
        width: 13
    }
};
