const Math = window.Math;

const COS_60_SQRT_PARAMETER = 3; // eslint
const COS_60_DEGREES = Math.sqrt(COS_60_SQRT_PARAMETER) / 2;

export default function drawStrandDirection(strand, boundaries, graphics, color, arrow, alpha = 1, renderCallback = null) {
    if (!strand || (strand.toLowerCase() !== 'positive' && strand.toLowerCase() !== 'negative'))
        return;
    const strandArrowBoxWidth = boundaries.height * 2;
    const margin = arrow.margin;
    const strandArrowSize = {height: arrow.height, width: 0};
    strandArrowSize.width = strandArrowSize.height * COS_60_DEGREES;
    const direction = strand.toLowerCase() === 'positive' ? 1 : -1;
    if (boundaries.width - 2 * margin > strandArrowSize.width){
        const arrowsCount = Math.max(1, Math.floor(boundaries.width / strandArrowBoxWidth));
        const diff = (boundaries.width - strandArrowBoxWidth * arrowsCount) / 2;
        const iDiff = 0.5;
        for (let i = 0; i < arrowsCount; i++){
            const center = {
                x: Math.round(boundaries.x + diff + (i + iDiff) * strandArrowBoxWidth),
                y: boundaries.centerY
            };
            const heightOffset = 0.5;
            if (arrow.mode === 'fill') {
                fillTriangle(
                    center,
                    strandArrowSize.height + 1,
                    direction,
                    graphics,
                    color,
                    alpha / 2,
                    renderCallback
                );
                fillTriangle(
                    center,
                    strandArrowSize.height + heightOffset,
                    direction,
                    graphics,
                    color,
                    alpha / 2,
                    renderCallback
                );
                fillTriangle(
                    center,
                    strandArrowSize.height,
                    direction,
                    graphics,
                    color,
                    alpha,
                    renderCallback
                );
            } else {
                strokeTriangle(
                    center,
                    strandArrowSize.height,
                    direction,
                    graphics,
                    color,
                    alpha,
                    arrow.thickness,
                    renderCallback
                );
            }
        }
    }
}

function fillTriangle(center, sideLength, direction, graphics, color, alpha, renderCallback) {
    const size = {
        height: sideLength,
        width: sideLength * COS_60_DEGREES
    };
    graphics
        .beginFill(color, alpha)
        .lineStyle(0, color, alpha)
        .moveTo(
            center.x - direction * size.width / 2,
            center.y - size.height / 2
        )
        .lineTo(
            center.x + direction * size.width / 2,
            center.y
        )
        .lineTo(
            center.x - direction * size.width / 2,
            center.y + size.height / 2
        )
        .lineTo(
            center.x - direction * size.width / 2,
            center.y - size.height / 2
        )
        .endFill();
    if (typeof renderCallback === 'function') {
        renderCallback(
            {
                x: Math.min(center.x - direction * size.width / 2, center.x + direction * size.width / 2),
                y: center.y - size.height / 2
            });
    }
}

function strokeTriangle(center, sideLength, direction, graphics, color, alpha, thickness, renderCallback) {
    const size = {
        height: sideLength,
        width: sideLength / 2
    };
    graphics
        .lineStyle(thickness, color, alpha)
        .moveTo(
            center.x - direction * size.width / 2,
            center.y - size.height / 2
        )
        .lineTo(
            center.x + direction * size.width / 2,
            center.y
        )
        .lineTo(
            center.x - direction * size.width / 2,
            center.y + size.height / 2
        );
    graphics
        .lineStyle(0, color, alpha);
    if (typeof renderCallback === 'function') {
        renderCallback(
            {
                x: Math.min(center.x - direction * size.width / 2, center.x + direction * size.width / 2),
                y: center.y - size.height / 2 - thickness / Math.sqrt(2)
            });
    }
}