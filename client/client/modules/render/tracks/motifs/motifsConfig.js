export default {
    height: 100,
    minHeight: 20,
    centerLine:{
        dash: {
            fill: 0x000000,
            length: 5,
            thickness: 1
        }
    },
    matches: {
        defaultColor: {
            positive: 0x3C99C4,
            negative: 0xDD4C46
        },
        height: 10,
        margin: 5,
        detailsThresholdPx: 5,
        strand: {
            arrow: {
                height: 6,
                margin: 2,
                mode: 'stroke',
                thickness: 2
            },
        }
    }
};
