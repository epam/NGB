import PIXI from 'pixi.js';
const Math = window.Math;

export default class DashLines{
    constructor(){

    }

    static drawDashLine(points: Array<PIXI.Point>, style, graphics: PIXI.Graphics = null): PIXI.Graphics{
        if (graphics === null){
            graphics = new PIXI.Graphics();
        }
        for (let i = 0; i < points.length - 1; i++){
            DashLines.__drawDashLine(points[i], points[i + 1], graphics, style);
        }
        return graphics;
    }

    static __drawDashLine(start: PIXI.Point, end: PIXI.Point, graphics: PIXI.Graphics, style){
        const {thickness, color, dash} = style;
        const length = Math.sqrt((end.y - start.y) ** 2 + (end.x - start.x) ** 2);
        const dashes = length / (2 * dash);
        const direction = {
            x : Math.sign(end.x - start.x),
            y : Math.sign(end.y - start.y)
        };
        const dashDelta = {
            x: Math.abs(end.x - start.x) / dashes / 2,
            y: Math.abs(end.y - start.y) / dashes / 2
        };
        graphics
            .beginFill(0xFFFFFF, 0)
            .lineStyle(thickness, color, 1);
        for (let i = 0; i < dashes; i ++){
            graphics
                .moveTo(start.x + dashDelta.x * direction.x * (2 * i), start.y + dashDelta.y * direction.y * (2 * i))
                .lineTo(start.x + dashDelta.x * direction.x * (2 * i + 1), start.y + dashDelta.y * direction.y * (2 * i + 1));
        }
    }
}
