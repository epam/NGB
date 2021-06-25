export default {
    bed: {
        height: 10,
        margin: 2,
        defaultColor: 0x92AEE7,
        strand: {
            arrow: {
                height: 6,
                margin: 1,
                mode: 'stroke',
                thickness: 2
            }
        },
        label: {
            fill: 0x273F70,
            font: 'bold 8pt arial'
        },
        description: {
            maximumDisplayLength: 100,
            label :{
                fill: 0x273F70,
                font: 'normal 7pt arial'
            },
            margin: 10
        }
    },
    renderStrandIndicatorOnLargeScale: true
};
