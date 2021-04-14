import PIXI from 'pixi.js';

import {
    drawingConfiguration
} from '../../../core';
import config from '../../whole-genome-config';

export class ChromosomeColumnRenderer {

    constructor({
        container,
        canvasSize,
        chromosomes,
        range: maxChrSize,
        hits,
        labelWidth
    }) {

        Object.assign(this, {
            container,
            canvasSize,
            chromosomes,
            maxChrSize,
            hits,
            labelWidth
        });
    }

    get height() {
        return this.canvasSize ? this.canvasSize.height : 0;
    }

    get containerHeight() {
        return this.height - 2 * config.start.topMargin;
    }

    get topMargin() {
        return config.start.topMargin;
    }

    get columnWidth() {
        return config.chromosomeColumn.width;
    }

    get hitsLimit() {
        return Math.floor((config.chromosomeColumn.spaceBetween - 2 * config.chromosomeColumn.margin) / config.gridSize);
    }

    init() {
        const container = new PIXI.Container();
        this.buildColumns(container);
        container.x = config.start.x + config.axis.width + this.labelWidth;
        container.y = config.start.topMargin;
        this.container.addChild(container);
        return container;
    }

    getPixelLength(start, end, chrSize, chrPixelValue) {
        return Math.round(this.convertToPixels(end, chrSize, chrPixelValue) - this.convertToPixels(start, chrSize, chrPixelValue));
    }
    getStartPx(nucleotide, chrSize, chrPixelValue) {
        return this.getGridStart(nucleotide, chrSize, chrPixelValue) * config.gridSize;
    }
    getEndPx(nucleotide, chrSize, chrPixelValue) {
        return this.getGridEnd(nucleotide, chrSize, chrPixelValue) * config.gridSize;
    }
    getGridStart(nucleotide, chrSize, chrPixelValue) {
        return (Math.round(this.convertToPixels(nucleotide, chrSize, chrPixelValue) / config.gridSize));
    }
    getGridEnd(nucleotide, chrSize, chrPixelValue) {
        return (Math.ceil(this.convertToPixels(nucleotide, chrSize, chrPixelValue) / config.gridSize));
    }

    createColumn(container, position, chrPixelValue, chromosome, hits) {

        let pixelGrid = Array(Math.floor(chrPixelValue / config.gridSize)).fill(0);

        const column = new PIXI.Graphics();
        column.x = 0;
        column.y = 0;
        container.addChild(column);

        column
            .beginFill(config.chromosomeColumn.fill, 1)
            .drawRect(
                position,
                0,
                this.columnWidth,
                chrPixelValue)
            .endFill()
            .lineStyle(config.chromosomeColumn.thickness, config.chromosomeColumn.lineColor, 1)
            .moveTo(position, 0)
            .lineTo(position + this.columnWidth, 0)
            .lineTo(position + this.columnWidth, chrPixelValue)
            .lineTo(position, chrPixelValue)
            .lineTo(position, 0);
        const initialMargin = position + this.columnWidth + config.chromosomeColumn.margin;
        hits.forEach((hit) => {
            const start = this.getGridStart(hit.startIndex, chromosome.size, chrPixelValue);
            const lengthPx = this.getPixelLength(hit.startIndex, hit.endIndex, chromosome.size, chrPixelValue);
            const lengthInGridCells = (lengthPx >= config.gridSize) ? (Math.ceil(lengthPx / config.gridSize)) : 1;
            const end = start + lengthInGridCells;
            const currentLevel = Math.max(...pixelGrid.slice(start, end)) + 1;
            for (let i = start; i <= end; i++) {
                if (
                    currentLevel <= this.hitsLimit &&
                    end * config.gridSize <= chrPixelValue
                ) {
                    pixelGrid[i] = currentLevel;
                    column
                        .lineStyle(config.chromosomeColumn.thickness, config.chromosomeColumn.lineColor, 0)
                        .beginFill(config.hit.lineColor, 1)
                        .drawRect(
                            initialMargin + (currentLevel - 1) * config.gridSize - 1,
                            this.getStartPx(hit.startIndex, chromosome.size, chrPixelValue) + 1,
                            config.gridSize - 1,
                            lengthInGridCells * config.gridSize - 1
                        )
                        .endFill();
                }
            }
        });
        pixelGrid = [];

        const label = this.createLabel(`chr ${chromosome.id}`, position);
        container.addChild(label);
    }

    buildColumns(container) {
        const sortedChromosomes = this.chromosomes.sort((chr1, chr2) => chr2.size - chr1.size);
        for (let i = 0; i < this.chromosomes.length; i++) {
            const position = (this.hitsLimit * config.gridSize + 2 * config.chromosomeColumn.margin + this.columnWidth) * i;
            const chr = sortedChromosomes[i];
            const chrHits = this.hits.filter(hit => hit.chromosome === chr.name);

            const pixelSize = this.convertToPixels(chr.size, this.maxChrSize, this.containerHeight);
            const sortedHits = this.sortHitsByLength(chrHits);
            this.createColumn(container, position, pixelSize, chr, sortedHits);

        }
    }

    convertToPixels(size, realRange, pixelRange) {
        return (size / realRange) * pixelRange;
    }

    createLabel(text, position) {

        const label = new PIXI.Text(text, config.tick.label);
        label.resolution = drawingConfiguration.resolution;
        label.y = 0 - this.topMargin / 2 - label.height / 2;
        label.x = position;
        return label;
    }

    sortHitsByLength(hits) {
        return hits.sort((hit1, hit2) => (hit2.endIndex - hit2.startIndex) - (hit1.endIndex - hit1.startIndex));
    }
}
