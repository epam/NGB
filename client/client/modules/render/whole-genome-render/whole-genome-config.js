const formatter = new Intl.NumberFormat('en-US');

export default {
    ticks: 50,
    canvas:{
        margin:10
    },
    start: {
        x: 10,
        y: 30,
        margin: 50,
        topMargin: 30,
    },
    axis: {
        color: 0x000000,
        thickness: 1,
        offsetX: 0,
        width: 50,
        canvasWidth: 100
    },
    tick: {
        formatter: ::formatter.format,
        thickness: 1,
        offsetXOdd: 15,
        offsetXEven: 10,
        color: 0x777777,
        margin: 4,
        label: {
            fill: 0x000000,
            font: 'normal 7pt arial',
        }
    },
    chromosomeColumn: {
        spaceBetween: 70,
        margin:5,
        topMargin: 20,
        width: 15,
        lineColor: 0x000000,
        fill: 0xadadad,
        thickness: 1
    },
    gridSize: 5,
    hit: {
        width: 3,
        color: 0x323232,
        scores: [
            {
                color: 0x32cd32,
                max: 50
            },
            {
                color: 0xffc400,
                max: 90,
                min: 50
            },
            {
                color: 0xcd3232,
                min: 90
            }
        ],
        offset: 5,
    },
    scrollBar: {
        height: 15,
        fill: 0xfffff,
        border: 0x4285f4,
        margin:5,
        slider: {
            margin: 15,
            fill: 0x4285f4
        }
    }
};
