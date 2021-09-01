import PIXI from 'pixi.js';

PIXI.Graphics.prototype.drawDashLine = function (x1, y1, x2, y2, dash = 0) {
    if (dash <= 0 || Number.isNaN(Number(dash))) {
        return this
            .moveTo(x1, y1)
            .lineTo(x2, y2);
    }
    const length = Math.sqrt((x1 - x2) ** 2 + (y1 - y2) ** 2);
    if (length === 0) {
        return this;
    }
    const parts = Math.floor(length / (2.0 * dash));
    let notProcessedLength = length;
    let cX = x1;
    let cY = y1;
    const dx = (x2 - x1) / length;
    const dy = (y2 - y1) / length;
    for (let i = 0; i < parts; i++) {
        notProcessedLength -= (2.0 * dash);
        this
            .moveTo(cX, cY)
            .lineTo(cX + dash * dx, cY + dash * dy);
        cX += (2.0 * dash * dx);
        cY += (2.0 * dash * dy);
    }
    if (notProcessedLength > 0) {
        const size = Math.min(dash, notProcessedLength);
        this
            .moveTo(cX, cY)
            .lineTo(cX + size * dx, cY + size * dy);
    }
    return this;
};
