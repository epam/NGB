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

    get width() {
        return this.canvasSize ? this.canvasSize.width : 0;
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
        return Math.floor(config.start.margin / (config.hit.width + config.hit.offset));
    }

    init() {
        const container = new PIXI.Container();
        container.x = this.labelWidth;
        container.y = this.topMargin;
        this.buildColumns(container);
        this.container.addChild(container);
        return container;
    }

    getStartPx(nucleotide, chrSize, chrPixelValue) {
        return (Math.floor(this.convertToPixels(nucleotide, chrSize, chrPixelValue) / config.gridSize)) * config.gridSize;
    }
    getEndPx(nucleotide, chrSize, chrPixelValue) {
        return (Math.ceil(this.convertToPixels(nucleotide, chrSize, chrPixelValue) / config.gridSize)) * config.gridSize;
    }
    getGridStart(nucleotide, chrSize, chrPixelValue) {
        return (Math.floor(this.convertToPixels(nucleotide, chrSize, chrPixelValue) / config.gridSize));
    }
    getGridEnd(nucleotide, chrSize, chrPixelValue) {
        return (Math.ceil(this.convertToPixels(nucleotide, chrSize, chrPixelValue) / config.gridSize));
    }

    createColumn(container, position, chrPixelValue, chromosome, hits) {

        let pixelGrid = Array(Math.floor(chrPixelValue / config.gridSize)).fill(0);

        const column = new PIXI.Graphics();
        column.x = position.x;
        column.y = 0;
        container.addChild(column);

        column
            .beginFill(config.chromosomeColumn.fill, 1)
            .drawRect(
                position.x,
                0,
                this.columnWidth,
                chrPixelValue)
            .endFill()
            .lineStyle(config.chromosomeColumn.thickness, config.chromosomeColumn.lineColor, 1)
            .moveTo(position.x, 0)
            .lineTo(position.x + this.columnWidth, 0)
            .lineTo(position.x + this.columnWidth, chrPixelValue)
            .lineTo(position.x, chrPixelValue)
            .lineTo(position.x, 0)
            .moveTo(position.x + config.hit.offset, 0);

        const initialMargin = position.x + 2 * this.columnWidth;
        hits.forEach((hit) => {
            const start = this.getGridStart(hit.startIndex, chromosome.size, chrPixelValue);
            const end = this.getGridEnd(hit.endIndex, chromosome.size, chrPixelValue);
            const currentLevel = Math.max(...pixelGrid.slice(start, end)) + 1;

            for (let i = start; i < end; i++) {
                if (
                    currentLevel <= this.hitsLimit &&
                    this.getEndPx(hit.endIndex, chromosome.size, chrPixelValue) < chrPixelValue
                ) {
                    pixelGrid[i] = currentLevel;
                    column
                        .lineStyle(config.chromosomeColumn.thickness / 2, config.chromosomeColumn.lineColor, 1)
                        .moveTo(initialMargin + (currentLevel - 1) * (2 * config.hit.width), start)
                        .beginFill(config.hit.lineColor, 1)
                        .drawRect(
                            initialMargin + (currentLevel - 1) * (2 * config.gridSize),
                            start * config.gridSize,
                            config.gridSize,
                            (end - start) * config.gridSize
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

        let position = {
            x: this.labelWidth,
            y: 0
        };
        const sortedChromosomes = this.chromosomes.sort((chr1, chr2) => chr2.size - chr1.size);
        for (let i = 0; i < this.chromosomes.length; i++) {

            const chr = sortedChromosomes[i];
            const chrHits = this.hits.filter(hit => hit.chromosome === chr.name);

            const pixelSize = this.convertToPixels(chr.size, this.maxChrSize, this.containerHeight);
            const sortedHits = this.sortHitsByLength(chrHits);
            this.createColumn(container, position, pixelSize, chr, sortedHits);

            position = {
                x: position.x + config.start.margin,
                y: 0
            };
        }
    }

    convertToPixels(size, realRange, pixelRange) {
        return (size / realRange) * pixelRange;
    }

    createLabel(text, position) {

        const label = new PIXI.Text(text, {
            fill: config.tick.label.fill,
            font: config.tick.label.font,
            margin: config.tick.label.margin,
        });

        label.resolution = drawingConfiguration.resolution;
        label.y = position.y - this.topMargin / 2 - label.height / 2;
        label.x = 2 * position.x;
        return label;
    }

    sortHitsByLength(hits) {
        return hits.sort((hit1, hit2) => (hit2.endIndex - hit2.startIndex) - (hit1.endIndex - hit1.startIndex));
    }
}
