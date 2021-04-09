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
        hits
    }) {

        Object.assign(this, {
            container,
            canvasSize,
            chromosomes,
            maxChrSize,
            hits
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
        container.x = config.start.margin;
        container.y = this.topMargin;
        this.buildColumns(container);
        this.container.addChild(container);
        return container;
    }

    createColumn(container, position, chrPixelValue, chromosome, hits) {

        let pixelArray = Array(Math.floor(chrPixelValue)).fill(0);

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


        hits.forEach((hit) => {
            const start = hit.startIndex;
            const end = hit.endIndex;
            const offset = end - start < 3 ? 3 : 0;
            const gap = 2;
            const currentLevel = Math.max(pixelArray[start], pixelArray[end + offset]) + 1;
            for (let i = start; i <= end + offset; i++) {
                if (currentLevel <= this.hitsLimit) {
                    if (pixelArray[start - 1] !== currentLevel) {
                        pixelArray[i] = currentLevel;
                        column
                            .lineStyle(config.hit.width, config.hit.lineColor, 1)
                            .moveTo(position.x + 2 * this.columnWidth + (currentLevel - 1) * (2 * config.hit.width), start)
                            .lineTo(position.x + 2 * this.columnWidth + (currentLevel - 1) * (2 * config.hit.width), end + offset);
                    } else {
                        pixelArray[i + gap] = currentLevel;
                        column
                            .lineStyle(config.hit.width, config.hit.lineColor, 1)
                            .moveTo(position.x + 2 * this.columnWidth + (currentLevel - 1) * (2 * config.hit.width), start + gap)
                            .lineTo(position.x + 2 * this.columnWidth + (currentLevel - 1) * (2 * config.hit.width), end + offset + gap);
                    }
                }

            }
        });
        pixelArray = [];

        const label = this.createLabel(`chr ${chromosome.id}`, position);
        container.addChild(label);
    }

    buildColumns(container) {

        let position = {
            x: config.start.x,
            y: 0
        };

        for (let i = 0; i < this.chromosomes.length; i++) {

            const chr = this.chromosomes[i];
            const chrHits = this.hits.filter(hit => hit.chromosome === chr.name);

            const pixelSize = this.convertToPixels(chr.size, this.maxChrSize, this.containerHeight);
            const sortedHits = this.sortHitsByLength(chrHits);
            const self = this;
            const pixeledHits = sortedHits.map(function(hit) {
                return {
                    startIndex: Math.floor(self.convertToPixels(hit.startIndex, chr.size, pixelSize)),
                    endIndex: Math.ceil(self.convertToPixels(hit.endIndex, chr.size, pixelSize)),
                };
            });
            this.createColumn(container, position, pixelSize, chr, pixeledHits);

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
