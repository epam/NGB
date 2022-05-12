import {NumberFormatter} from '../../utilities';

const formatter = new Intl.NumberFormat('en-US');

export default {
    brush: {
        area: {
            alpha: 0.2,
            color: 0x000000,
            frame: {
                thickness: 1
            },
            selection: {
                alpha: 0.25
            },
            stroke: 0x737373
        },
        cursor: {
            height: 18,
            notch: {
                color: 0xffffff,
                count: 2,
                height: 7,
                thickness: 1,
                xOffset: 2,
                yOffset: 8
            },
            width: 9
        },
        drag:{
            formatter: ::NumberFormatter.textWithPrefix,
            height: 10,
            label:{
                fill: 0x000000,
                fontFamily: 'arial',
                fontSize: '9pt',
                fontWeight: 'bold'
            },
            notch:{
                color: 0x6f6f6f,
                thickness: 1,
                width: 15,
            },
            notches: 4,
            region:{
                arrow:{
                    margin: 2,
                    width: 5
                },
                line: {
                    color: 0x000000,
                    thickness: 1
                }
            }
        },
        line: {
            thickness: 2
        }
    },
    brushColor: {
        normal: 0x2a9af5,
        shortenedIntrons: 0xE95C15,
        shortenedIntronsAlpha: 0.25
    },
    global:{
        body:{
            fill: 0xd7d7d7,
            height:12,
            stroke: {
                color: 0x8f8f8f,
                thickness: 1
            }
        },
        tickArea:{
            margin:0
        },
        tick:{
            formatter: ::NumberFormatter.textWithPrefix,
            height: 0,
            label: {
                fill: 0x000000,
                fontFamily: 'arial',
                fontSize: '7pt',
                fontWeight: 'normal'
            },
            margin: 0,
            thickness: 1
        },
        ticks: 5,
        ticksMinMargin: 10
    },
    local:{
        body:{
            fill: 0xd7d7d7,
            height: 17,
            stroke: {
                color: 0x8f8f8f,
                thickness: 1
            }
        },
        tickArea:{
            margin:17
        },
        centerTick: {
            background: {
                alpha: 1,
                padding: {
                    x: 3,
                    y: 1
                }
            },
            formatter: ::formatter.format,
            height: 5,
            label: {
                fill: 0xffffff,
                fontFamily: 'arial',
                fontSize: '7pt',
                fontWeight: 'normal'
            },
            margin: 2,
            thickness: 1
        },
        tick:{
            formatter: ::formatter.format,
            height: 5,
            label: {
                fill: 0x000000,
                fontFamily: 'arial',
                fontSize: '7pt',
                fontWeight: 'normal'
            },
            margin: 2,
            thickness: 1
        },
        ticks: 10,
        ticksMinMargin: 10
    },
    rulersVerticalMargin: -5
};
