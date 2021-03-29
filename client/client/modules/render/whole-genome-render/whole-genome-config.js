const formatter = new Intl.NumberFormat('en-US');

export default {
    ticks:50,
    start: {
        x: 10,
        y: 30,
        margin: 50,
        topMargin:20,
    },
    axis: {
          color: 0x000000,
          thickness: 1,
          offsetX: 0
      },
      tick: {
          formatter: ::formatter.format,
          thickness: 1,
          offsetXOdd: 15,
          offsetXEven: 10,
          color: 0x777777,
          label: {
              fill: 0x000000,
              font: 'normal 7pt arial',
              margin:4
          }
    },
    chromosomeColumn:{
        topMargin: 20,
        width: 15,
        lineColor: 0x000000,
        fill: 0xadadad,
        thickness:1
    }
};

