import PIXI from 'pixi.js';
import {BaseViewport, drawingConfiguration} from '../../../../core';

const Math = window.Math;

export default class AminoacidsRenderer{

    _config = null;

    get config() { return this._config; }

    constructor (config){
        this._config = config;
    }

    renderAminoacids(aminoacidsData: Array, viewport: BaseViewport, container: PIXI.Container, topOffset, gap = null): PIXI.Graphics {
        if (gap === null){
            this.renderAminoacidsInRange({rangeStart : viewport.chromosome.start, rangeEnd : viewport.chromosome.end}, aminoacidsData, viewport, container, topOffset);
        }
        else {
            this.renderAminoacidsInRange({rangeStart : viewport.chromosome.start, rangeEnd : gap.start - 1}, aminoacidsData, viewport, container, topOffset);
            this.renderAminoacidsInRange({rangeStart : gap.end, rangeEnd : viewport.chromosome.end}, aminoacidsData, viewport, container, topOffset);
        }
    }

    renderAminoacidsInRange(range, aminoacidsData: Array, viewport: BaseViewport, container: PIXI.Container, topOffset): PIXI.Graphics {
        if (aminoacidsData === null || aminoacidsData === undefined || aminoacidsData.aminoacids === null || aminoacidsData.aminoacids === undefined || aminoacidsData.aminoacids.length === 0)
            return;
        const {rangeStart, rangeEnd} = range;

        const graphics = new PIXI.Graphics();
        container.addChild(graphics);

        const bpLength = viewport.factor - 2 * this.config.nucleotide.margin.x;
        const height = this.config.aminoacids.height;

        for (let n = 0; n < aminoacidsData.aminoacids.length; n++) {
            const aminoacid = aminoacidsData.aminoacids[n];
            if (aminoacid.aminoacid === null || aminoacid.endIndex < rangeStart || aminoacid.startIndex > rangeEnd)
                continue;
            let style = this.config.aminoacids.label.normal;
            if (aminoacid.modified){
                style = this.config.aminoacids.label.modified;
            }
            const width = viewport.convert.brushBP2pixel(aminoacid.endIndex - aminoacid.startIndex + 1);
            const x = viewport.project.brushBP2pixel((aminoacid.startIndex + aminoacid.endIndex) / 2);
            const visibleRange = {
                start: Math.max(viewport.project.brushBP2pixel(aminoacid.startIndex) - bpLength / 2, viewport.project.brushBP2pixel(rangeStart) - bpLength / 2, viewport.canvas.start),
                end: Math.min(viewport.project.brushBP2pixel(aminoacid.endIndex) + bpLength / 2, viewport.project.brushBP2pixel(rangeEnd) + bpLength / 2, viewport.canvas.end)
            };
            const lineVisibleRange = {
                start: Math.max(viewport.project.brushBP2pixel(aminoacid.startIndex) - bpLength / 2, viewport.project.brushBP2pixel(rangeStart) - bpLength / 2),
                end: Math.min(viewport.project.brushBP2pixel(aminoacid.endIndex) + bpLength / 2, viewport.project.brushBP2pixel(rangeEnd) + bpLength / 2)
            };
            const lineLeftClipped = viewport.project.brushBP2pixel(aminoacid.startIndex) < lineVisibleRange.start;
            const lineRightClipped = viewport.project.brushBP2pixel(aminoacid.endIndex) > lineVisibleRange.end;
            if (x + width / 2 >= viewport.canvas.start && x - width / 2 <= viewport.canvas.end) {

                graphics.lineStyle(1, 0x000000, 0.25);
                if (lineLeftClipped){
                    graphics
                        .moveTo(lineVisibleRange.start + 1, topOffset + this.config.aminoacids.margin + this.config.aminoacids.height);
                }
                else{
                    graphics
                        .moveTo(lineVisibleRange.start + 1, topOffset + 2 * this.config.aminoacids.margin + this.config.aminoacids.height)
                        .lineTo(lineVisibleRange.start + 1, topOffset + this.config.aminoacids.margin + this.config.aminoacids.height);
                }
                if (lineRightClipped){
                    graphics
                        .lineTo(lineVisibleRange.end - 1, topOffset + this.config.aminoacids.margin + this.config.aminoacids.height);
                }
                else{
                    graphics
                        .lineTo(lineVisibleRange.end - 1, topOffset + this.config.aminoacids.margin + this.config.aminoacids.height)
                        .lineTo(lineVisibleRange.end - 1, topOffset + 2 * this.config.aminoacids.margin + this.config.aminoacids.height);
                }

                const label = new PIXI.Text(aminoacid.aminoacid, style);
                label.resolution = drawingConfiguration.resolution;
                label.x = Math.round((visibleRange.start + visibleRange.end) / 2 - label.width / 2);
                label.y = Math.round(topOffset + this.config.aminoacids.margin + height / 2 - label.height / 2);
                container.addChild(label);
            }
        }
    }
}
