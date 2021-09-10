const colorsCount = 6;
const grayScaleAmountFrom = 0.75;
const grayScaleAmountTo = 0.2;
const hoveredChannelShift = 0.1;
const borderChannelShift = 0.2;

const WHITE_CHANNEL = 0xff;
const CHANNEL_BASE = 8;

const grayScaleRange = Math.abs(grayScaleAmountTo - grayScaleAmountFrom);
const grayScaleDirection = Math.sign(grayScaleAmountTo - grayScaleAmountFrom);

const generateGrayScaleColor = (amount) => {
    const channel = Math.floor(WHITE_CHANNEL * Math.min(1, Math.max(0, amount)));
    return channel | (channel << CHANNEL_BASE) | (channel << (CHANNEL_BASE * 2));
};

const generateGrayScaleColorConfiguration = (amount) => ({
    color: generateGrayScaleColor(amount),
    hovered: generateGrayScaleColor(amount - hoveredChannelShift),
    border: generateGrayScaleColor(amount - borderChannelShift),
});

const grayScaleColors = (new Array(colorsCount))
    .fill(0)
    .map((o, index, array) => generateGrayScaleColorConfiguration(
        grayScaleAmountFrom + grayScaleDirection * (index / array.length) * grayScaleRange
    ));

export default grayScaleColors;
