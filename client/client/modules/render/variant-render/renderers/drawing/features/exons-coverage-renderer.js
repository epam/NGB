import * as PIXI from 'pixi.js';
import {BaseViewport} from '../../../../core';

const Math = window.Math;

export default class ExonsCoverageRenderer{

    _config = null;

    get config() { return this._config; }

    constructor (config){
        this._config = config;
    }

    renderExonsConverage(gene, exons: Array, viewport: BaseViewport, container: PIXI.Container, yStart, yEnd, gap = null): PIXI.Graphics {
        if (gap === null){
            this.renderExonsConverageInRange({rangeStart : viewport.chromosome.start, rangeEnd : viewport.chromosome.end}, gene, exons, viewport, container, yStart, yEnd);
        }
        else {
            this.renderExonsConverageInRange({rangeStart : viewport.chromosome.start, rangeEnd : gap.start - 1}, gene, exons, viewport, container, yStart, yEnd);
            this.renderExonsConverageInRange({rangeStart : gap.end, rangeEnd : viewport.chromosome.end}, gene, exons, viewport, container, yStart, yEnd);
        }
    }

    renderExonsConverageInRange(range, gene, exons: Array, viewport: BaseViewport, container: PIXI.Container, yStart, yEnd): PIXI.Graphics {
        if (exons === null || exons === undefined || exons.length === 0)
            return;
        const {rangeStart, rangeEnd} = range;

        const graphics = new PIXI.Graphics();
        container.addChild(graphics);

        const bpLength = viewport.factor - 2 * this.config.nucleotide.margin.x;

        const visibleRange = {
            start: Math.max(viewport.canvas.start, viewport.project.brushBP2pixel(rangeStart) - bpLength / 2),
            end: Math.min(viewport.canvas.end, viewport.project.brushBP2pixel(rangeEnd) + bpLength / 2)
        };

        for (let n = 0; n < exons.length; n++) {
            const exon = exons[n];
            const exonStart = gene.startIndex + exon.start;
            const exonEnd = gene.startIndex + exon.end;
            const exonStartPx = viewport.project.brushBP2pixel(exonStart) - bpLength / 2;
            const exonEndPx = viewport.project.brushBP2pixel(exonEnd) + bpLength / 2;
            if (exonEnd < rangeStart || exonStart > rangeEnd || exonEndPx < visibleRange.start || exonStartPx > visibleRange.end)
                continue;
            const exonDrawingRegion = {
                start: Math.max(visibleRange.start, exonStartPx),
                end: Math.min(visibleRange.end, exonEndPx)
            };
            graphics
                .beginFill(0xd8efdd, 1)
                .drawRect(exonDrawingRegion.start, yStart, exonDrawingRegion.end - exonDrawingRegion.start, yEnd - yStart)
                .endFill();
        }
    }
}
