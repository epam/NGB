import PIXI from 'pixi.js';
import {BaseViewport} from '../../../../core';
import drawStrandDirection from '../../../../tracks/gene/internal/renderer/features/drawing/strandDrawing';

export default class StrandDirectionRenderer{

    _config = null;

    get config() { return this._config; }

    constructor (config){
        this._config = config;
    }

    renderStrandDirection(strand, viewport: BaseViewport, container: PIXI.Container, yStart, yEnd): PIXI.Graphics {

        const graphics = new PIXI.Graphics();
        container.addChild(graphics);

        drawStrandDirection(strand, {
            x: viewport.canvas.start,
            centerY: (yEnd + yStart) / 2,
            width: viewport.canvasSize,
            height: yEnd - yStart
        }, graphics, this.config.strand.fill, this.config.strand.arrow);

    }

    renderStrandDirectionForFeature(strand, boundaries, container: PIXI.Container, alpha = 1): PIXI.Graphics {
        const {xStart, xEnd, yStart, yEnd} = boundaries;
        const graphics = new PIXI.Graphics();
        container.addChild(graphics);

        drawStrandDirection(strand, {
            x: xStart,
            centerY: (yEnd + yStart) / 2,
            width: xEnd - xStart,
            height: yEnd - yStart
        }, graphics, this.config.strand.fill, this.config.strand.arrow, alpha);

    }
}
