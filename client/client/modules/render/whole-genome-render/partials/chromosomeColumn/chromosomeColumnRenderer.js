import PIXI from 'pixi.js';
import {
    Subject
} from 'rx';

import {
    drawingConfiguration
} from '../../../core';
import config from '../../whole-genome-config';


export class ChromosomeColumnRenderer {

    scrollBar = new PIXI.Graphics();
    columnsMask = new PIXI.Graphics();
    columns = new PIXI.Graphics();
    scrollContainer = new PIXI.Container();
    mainContainer = new PIXI.Container();
    isScrollable = false;
    gridContent;

    constructor({
        container,
        canvasSize,
        chromosomes,
        range: maxChrSize,
        hits,
        renderer
    }) {

        Object.assign(this, {
            container,
            canvasSize,
            chromosomes,
            maxChrSize,
            hits,
            renderer
        });
    }
    get mask() {
        return this.isScrollable ? this.columnsMask : null;
    }
    get height() {
        return this.canvasSize ? this.canvasSize.height : 0;
    }
    get containerWidth() {
        return this.canvasSize ? this.canvasSize.width : 0;
    }
    get actualDrawingWidth() {
        return this.chromosomes.length * (config.chromosomeColumn.width + config.chromosomeColumn.spaceBetween);
    }
    get containerHeight() {
        return this.height - 2 * config.start.topMargin;
    }
    get scrollBarWidth() {
        return Math.pow((this.canvasSize.width - config.scrollBar.slider.margin), 2) / this.actualDrawingWidth;
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
    get chrBlockWidth() {
        return this.hitsLimit * config.gridSize + 2 * config.chromosomeColumn.margin + this.columnWidth;
    }
    get scrollFactor() {
        return this.actualDrawingWidth / (this.canvasSize.width - config.scrollBar.slider.margin);
    }
    get sortedChromosomes() {
        return this.chromosomes.sort((chr1, chr2) => chr2.size - chr1.size);
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
    activateMask() {
        if (this.mask) {
            this.mask.x = 0;
            this.mask.y = -this.topMargin;
            this.mask.drawRect(0, 0, this.canvasSize.width - config.scrollBar.slider.margin, this.height);
            this.mainContainer.addChild(this.mask);
            this.mainContainer.mask = this.mask;
        }
    }

    init() {
        this.calculateGridContent();
        this.mainContainer.addChild(this.columns);
        this.buildColumns();
        if (this.containerWidth < this.actualDrawingWidth) {
            this.isScrollable = true;
            this.createScrollBar();
            this.activateMask();
        } else {
            this.isScrollable = false;
        }
        this.mainContainer.addChild(this.scrollContainer);
        this.mainContainer.x = config.axis.canvasWidth;
        this.mainContainer.y = config.start.topMargin;
        this.container.addChild(this.mainContainer);
    }

    buildColumns(pos = 0) {
        const start = pos ? pos * (-1) : 0;
        this.columns.x = start;
        this.columns.y = 0;

        for (let i = 0; i < this.chromosomes.length; i++) {
            const position = this.chrBlockWidth * i;
            const chr = this.sortedChromosomes[i];
            const pixelSize = this.convertToPixels(chr.size, this.maxChrSize, this.containerHeight);
            this.createColumn(position, pixelSize, chr, pos);
        }
    }

    calculateGridContent() {
        const sortedHits = this.sortHitsByLength(this.hits);
        const groupedHitsByChromosome = sortedHits.reduce((acc, hit) => {
            acc[hit.chromosome] = (acc[hit.chromosome] || []).concat(hit);
            return acc;
        }, {});
        const gridContent = {};

        for (const chrName in groupedHitsByChromosome) {
            if (groupedHitsByChromosome.hasOwnProperty(chrName)) {
                gridContent[chrName] = [];
                const chr = this.chromosomes.filter((chr) => chr.name === chrName)[0];
                const pixelSize = this.convertToPixels(chr.size, this.maxChrSize, this.containerHeight);
                let pixelGrid = Array(Math.floor(pixelSize / config.gridSize)).fill(0);

                groupedHitsByChromosome[chrName].forEach((hit) => {
                    const start = this.getGridStart(hit.startIndex, chr.size, pixelSize);
                    const lengthPx = this.getPixelLength(hit.startIndex, hit.endIndex, chr.size, pixelSize);
                    const lengthInGridCells = (lengthPx >= config.gridSize) ? (Math.ceil(lengthPx / config.gridSize)) : 1;
                    const end = start + lengthInGridCells;
                    const currentLevel = Math.max(...pixelGrid.slice(start, end)) + 1;
                    gridContent[chrName].push({
                        start,
                        end,
                        x: currentLevel
                    });
                    for (let i = start; i <= end; i++) {
                        pixelGrid[i] = currentLevel;
                    }
                });
                pixelGrid = [];
            }
        }
        this.gridContent = gridContent;
    }

    updateColumnsByScroll(params, localBounds) {
        this.columns.clear();
        let startPoint;
        if (
            params.currentPosition > 0 &&
            localBounds.x < params.end &&
            params.currentPosition < params.end
        ) {
            startPoint = localBounds.x * this.scrollFactor;
        } else if (localBounds.x <= 0 || params.currentPosition < 0) {
            startPoint = 0;
        } else if (
            localBounds.x >= params.end ||
            params.currentPosition >= params.end
        ) {
            startPoint = params.end * this.scrollFactor;
        }
        this.buildColumns(startPoint);
    }

    createColumn(position, chrPixelValue, chromosome, scrollOffset = 0) {
        this.columns
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
        if (
            this.gridContent &&
            this.gridContent[chromosome.name]) {
            const initialMargin = position + this.columnWidth + config.chromosomeColumn.margin;
            this.gridContent[chromosome.name].forEach(hit => {
                const {
                    start,
                    end,
                    x
                } = hit;
                if (
                    x <= this.hitsLimit &&
                    end * config.gridSize <= chrPixelValue) {
                    this.columns
                        .lineStyle(config.chromosomeColumn.thickness, config.chromosomeColumn.lineColor, 0)
                        .beginFill(config.hit.lineColor, 1)
                        .drawRect(
                            initialMargin + (x - 1) * config.gridSize - 1,
                            start * config.gridSize + 1,
                            config.gridSize - 1,
                            (end - start) * config.gridSize - 1
                        )
                        .endFill();
                }
            });
        }

        this.updateLabel(`chr ${chromosome.id}`, position, scrollOffset);
    }

    convertToPixels(size, realRange, pixelRange) {
        return (size / realRange) * pixelRange;
    }

    createLabel(text, position, scrollOffset) {
        const label = new PIXI.Text(text, config.tick.label);
        label.name = text;
        label.resolution = drawingConfiguration.resolution;
        label.y = 0 - this.topMargin / 2 - label.height / 2;
        label.x = position - scrollOffset;
        return label;
    }

    updateLabel(text, position, scrollOffset) {
        let label = this.mainContainer.getChildByName(text);
        if (label) {
            this.mainContainer.removeChild(label);
        }
        label = this.createLabel(text, position, scrollOffset);
        this.mainContainer.addChild(label);
    }

    createScrollBar(scrollParams = {}) {
        const pixiEvent$ = new Subject();
        const toStream = (e) => pixiEvent$.onNext(e);
        let subscription;

        this.scrollContainer.x = 0;
        this.scrollContainer.y = this.containerHeight + config.scrollBar.margin;
        this.scrollContainer.interactive = true;
        this.scrollContainer.buttonMode = true;
        this.scrollContainer.addChild(this.scrollBar);
        this.drawScrollBar(0);

        if (!scrollParams.start && !scrollParams.end) {
            scrollParams.start = 0;
            scrollParams.currentPosition = 0;
            scrollParams.end = this.canvasSize.width - config.scrollBar.slider.margin - this.scrollBarWidth;
        }
        this.scrollContainer.on('mouseup', () => {
            if (subscription) {
                subscription.dispose();
            }
        });
        this.scrollContainer.on('mouseupoutside', () => {
            if (subscription) {
                subscription.dispose();
            }
        });
        this.scrollContainer.on('mousedown', () => {
            subscription = pixiEvent$.subscribe((e) => {
                this.scrollBarMove(e, scrollParams);
            });
        });
        this.scrollContainer.on('mousemove', (e) => {
            if (subscription) {
                toStream(e);
            }
        });
    }

    scrollBarMove(e, scrollParams) {
        const localBounds = this.scrollBar.getLocalBounds();
        if (
            scrollParams.end !== undefined &&
            scrollParams.start !== undefined &&
            e && e.data && e.data.originalEvent &&
            e.data.originalEvent.movementX
        ) {
            const delta = e.data.originalEvent.movementX;
            const local = localBounds.x + delta;
            scrollParams.currentPosition = local;
            this.updateScrollBar(scrollParams, localBounds);
        }
    }

    updateScrollBar(params, localBounds) {
        this.scrollBar.clear();

        this.reDrawScrollBar(params, localBounds);
        this.updateColumnsByScroll(params, localBounds);
        requestAnimationFrame(() => this.renderer.render(this.container));
    }


    drawScrollBar(currentScrollPosition) {
        this.scrollBar
            .beginFill(config.scrollBar.slider.fill, 0.5)
            .drawRect(
                currentScrollPosition > 0 ? currentScrollPosition : 0,
                0,
                this.scrollBarWidth,
                config.scrollBar.height
            )
            .endFill();
    }

    reDrawScrollBar(params, localBounds) {
        const {
            start,
            end,
            currentPosition
        } = params;
        if (
            currentPosition <= end &&
            localBounds.x <= end
        ) {
            this.drawScrollBar(currentPosition);
        } else if (localBounds.x <= start) {
            this.drawScrollBar(params.start);
        } else {
            this.drawScrollBar(end);
        }
    }

    sortHitsByLength(hits) {
        return hits.sort((hit1, hit2) => (hit2.endIndex - hit2.startIndex) - (hit1.endIndex - hit1.startIndex));
    }
    resizeScroll(width) {
        this.canvasSize.width = width;
        this.scrollBar.clear();
        this.scrollContainer.removeAllListeners();
        if (this.canvasSize.width < this.actualDrawingWidth) {
            this.isScrollable = true;
            const scrollParams = {
                start: 0,
                currentPosition: 0,
                end: width - config.scrollBar.slider.margin - this.scrollBarWidth
            };
            this.activateMask();
            this.createScrollBar(scrollParams);

        } else {
            this.isScrollable = false;
            requestAnimationFrame(() => this.renderer.render(this.container));
        }
    }
}
