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
    chrScrollSubject = new Subject();
    isScrollable = false;

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
    get maxScrollOffset() {
        return this.actualDrawingWidth - this.canvasSize.width - config.scrollBar.slider.margin;
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

    init() {
        this.mainContainer.addChild(this.columns);
        this.buildColumns();
        if (this.containerWidth < this.actualDrawingWidth) {
            this.isScrollable = true;
            this.createScrollBar();
            if (this.mask) {
                this.mask.x = 0;
                this.mask.y = -this.topMargin;
                this.mask.drawRect(0, 0, this.canvasSize.width - config.scrollBar.slider.margin, this.height);
                this.mainContainer.addChild(this.mask);
                this.mainContainer.mask = this.mask;
            }
        } else {
            this.isScrollable = false;
        }
        this.mainContainer.addChild(this.scrollContainer);
        this.mainContainer.x = config.axis.canvasWidth;
        this.mainContainer.y = config.start.topMargin;
        this.container.addChild(this.mainContainer);
        return this.mainContainer;
    }

    buildColumns(pos = 0) {
        const sortedChromosomes = this.chromosomes.sort((chr1, chr2) => chr2.size - chr1.size);
        const start = pos ? pos * (-1) : 0;
        this.columns.x = start;
        this.columns.y = 0;

        for (let i = 0; i < this.chromosomes.length; i++) {
            const position = this.chrBlockWidth * i;
            const chr = sortedChromosomes[i];
            const chrHits = this.hits.filter(hit => hit.chromosome === chr.name);

            const pixelSize = this.convertToPixels(chr.size, this.maxChrSize, this.containerHeight);
            const sortedHits = this.sortHitsByLength(chrHits);
            this.createColumn(position, pixelSize, chr, sortedHits, pos);
        }
    }

    updateColumnsByScroll(params) {
        const {
            start,
            currentPosition
        } = params;
        this.columns.clear();
        let startPoint;
        if (currentPosition <= start) {
            startPoint = 0;
        } else if (currentPosition > this.maxScrollOffset) {
            startPoint = 3 / 2 * this.maxScrollOffset;
        } else if (currentPosition <= this.maxScrollOffset) {
            startPoint = currentPosition;
        }
        this.buildColumns(startPoint);
    }

    createColumn(position, chrPixelValue, chromosome, hits, scrollOffset = 0) {
        let pixelGrid = Array(Math.floor(chrPixelValue / config.gridSize)).fill(0);
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

        const initialMargin = position + this.columnWidth + config.chromosomeColumn.margin;
        hits.forEach((hit) => {
            const start = this.getGridStart(hit.startIndex, chromosome.size, chrPixelValue);
            const lengthPx = this.getPixelLength(hit.startIndex, hit.endIndex, chromosome.size, chrPixelValue);
            const lengthInGridCells = (lengthPx >= config.gridSize) ? (Math.ceil(lengthPx / config.gridSize)) : 1;
            const end = start + lengthInGridCells;
            const currentLevel = Math.max(...pixelGrid.slice(start, end)) + 1;

            if (
                currentLevel <= this.hitsLimit &&
                end * config.gridSize <= chrPixelValue
            ) {
                this.columns
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
            for (let i = start; i <= end; i++) {
                pixelGrid[i] = currentLevel;
            }
        });
        pixelGrid = [];
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
        const pixiEventStream = new Subject();
        const toStream = (e) => pixiEventStream.onNext(e);
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
            scrollParams.end = this.canvasSize.width - config.scrollBar.slider.margin;
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
            subscription = pixiEventStream.subscribe((e) => {
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

        if (
            scrollParams.end !== undefined &&
            scrollParams.start !== undefined &&
            e && e.data && e.data.originalEvent &&
            e.data.originalEvent.movementX
        ) {
            const delta = e.data.originalEvent.movementX;
            const local = scrollParams.currentPosition + delta;
            scrollParams.currentPosition = local;
            this.updateScrollBar(scrollParams);
        }
    }

    updateScrollBar(params) {
        this.scrollBar.clear();
        this.reDrawScrollBar(params);
        this.updateColumnsByScroll(params);
        requestAnimationFrame(() => this.renderer.render(this.container));
    }


    drawScrollBar(currentScrollPosition) {
        this.scrollBar
            .beginFill(config.scrollBar.slider.fill, 0.5)
            .drawRect(
                currentScrollPosition,
                0,
                this.scrollBarWidth,
                config.scrollBar.height
            )
            .endFill();
    }

    reDrawScrollBar(params) {
        const localBounds = this.scrollBar.getLocalBounds();
        const {
            start,
            end,
            currentPosition
        } = params;
        if (
            localBounds.x + this.scrollBarWidth <= end &&
            currentPosition < this.maxScrollOffset &&
            currentPosition >= start &&
            localBounds.x >= start
        ) {
            this.drawScrollBar(currentPosition);
        } else if (currentPosition < start) {
            this.drawScrollBar(params.start);
        } else if (currentPosition >= this.maxScrollOffset) {
            this.drawScrollBar(end - this.scrollBarWidth);
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
                end: width - config.scrollBar.slider.margin
            };
            this.mask.drawRect(0, 0, this.canvasSize.width - config.scrollBar.slider.margin, this.height);
            this.createScrollBar(scrollParams);
        } else {
            this.isScrollable = false;
        }
    }
}
