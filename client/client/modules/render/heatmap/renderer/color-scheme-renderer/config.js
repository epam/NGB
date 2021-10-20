export default {
    margin: 5,
    discrete: {
        width: 0,
        margin: 10,
        gradientStop: {
            colorIndicator: {
                width: 20,
                height: 15,
                margin: 2,
                stroke: 0x333333
            },
            height: 20,
            margin: 2
        }
    },
    continuous: {
        width: 10,
        maxHeight: 200,
        stroke: 0x333333,
        sectionSize: 1
    },
    label: {
        font: {
            fill: 0x333333,
            fontFamily: 'arial',
            fontSize: '8pt',
            fontWeight: 'normal',
            align: 'center'
        },
        margin: 2,
        background: {
            fill: 0xfafafa,
            alpha: 0.75,
            offset: 4
        }
    }
};
