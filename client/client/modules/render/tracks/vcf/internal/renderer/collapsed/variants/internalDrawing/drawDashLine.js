import * as PIXI from 'pixi.js-legacy';
const Math = window.Math;
export function drawDashLine(graphics: PIXI.Graphics, dashSize, start, end, drawConfig) {
    const {xStart, yStart} = start;
    const {xEnd, yEnd} = end;
    const {stroke, thickness} = drawConfig;
    const dashes = Math.sqrt((xEnd - xStart) ** 2 + (yEnd - yStart) ** 2) / (dashSize * 2);
    const d = {x: (xEnd - xStart) / (dashes * 2), y: (yEnd - yStart) / (dashes * 2)};
    graphics
        .lineStyle(thickness, stroke, 1);
    for (let i = 0; i < dashes; i++) {
        const p1 = {x: Math.round(xStart + d.x * (i * 2)), y: Math.round(yStart + d.y * (i * 2))};
        const p2 = {x: Math.round(xStart + d.x * (i * 2 + 1)), y: Math.round(yStart + d.y * (i * 2 + 1))};
        graphics
            .moveTo(p1.x, p1.y)
            .lineTo(p2.x, p2.y);
    }
}
