import PIXI from 'pixi.js';
import {
    Subject
} from 'rx';

import {
    drawingConfiguration
} from '../../../core';
import config from '../../whole-genome-config';


export class ChromosomeColumnRenderer {
    
    scrollContainer = new PIXI.Container();
    mainContainer = new PIXI.Container();
    scrollBar = new PIXI.Graphics();
    columnsMask = new PIXI.Graphics();
    columns = new PIXI.Graphics();
    isScrollable = false;
    labelsMap = new Map();
    scrollPosition = 0;
    /**
     * `scrollPosition` is a current scroll offset for the canvas in pixels,
     * i.e. `scrollOffset` == 10px means that we scrolled canvas to the right at 10px -position
     * @type {number}
     */
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
        return this.canvasSize ? this.canvasSize.width - 2 * config.canvas.margin : 0;
    }
    get actualDrawingWidth() {
        return this.chromosomes.length * (config.chromosomeColumn.width + config.chromosomeColumn.spaceBetween);
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
    get chrBlockWidth() {
        return this.hitsLimit * config.gridSize + 2 * config.chromosomeColumn.margin + this.columnWidth;
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
            this.mask.clear();
            this.mask.x = 0;
            this.mask.y = -this.topMargin;
            this.mask.drawRect(0, 0, this.containerWidth, this.height);
            this.mainContainer.addChild(this.mask);
            this.mainContainer.mask = this.mask;
        }
    }
    isInFrame(x) {
        return x >= (this.scrollPosition - this.chrBlockWidth) && x <= (this.scrollPosition + this.containerWidth);
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

    /**
     * `buildColumns` renders chromosomes graphics
     */
    buildColumns() {
        this.columns.clear();
        this.columns.x = -this.scrollPosition;
        this.columns.y = 0;
        for (let i = 0; i < this.chromosomes.length; i++) {
            const position = this.chrBlockWidth * i;
            const chr = this.chromosomes[i];
            if (this.isInFrame(position)) {
                const pixelSize = this.convertToPixels(chr.size, this.maxChrSize, this.containerHeight);
                this.createColumn(position, pixelSize, chr);
            }
            this.updateLabel(chr.id, position);

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
                const [chr] = this.chromosomes.filter((chr) => chr.name === chrName);
                const pixelSize = this.convertToPixels(chr.size, this.maxChrSize, this.containerHeight);
                let pixelGrid = Array(Math.floor(pixelSize / config.gridSize)).fill(0);

                groupedHitsByChromosome[chrName].forEach((hit) => {
                    const start = this.getGridStart(hit.startIndex, chr.size, pixelSize);
                    const lengthPx = this.getPixelLength(hit.startIndex, hit.endIndex, chr.size, pixelSize);
                    const lengthInGridCells = Math.ceil(lengthPx / config.gridSize) || 1;
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

    createColumn(position, chrPixelValue, chromosome) {
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
        this.columns
            .lineStyle(config.chromosomeColumn.thickness, config.chromosomeColumn.lineColor, 0)
            .beginFill(config.hit.lineColor, 1);
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
                        .drawRect(
                            initialMargin + (x - 1) * config.gridSize - 1,
                            start * config.gridSize + 1,
                            config.gridSize - 1,
                            (end - start) * config.gridSize - 1
                        );
                }
            });
            this.columns
                .endFill();
        }
    }

    convertToPixels(size, realRange, pixelRange) {
        return (size / realRange) * pixelRange;
    }

    createLabel(text, position) {
        const label = new PIXI.Text(`chr ${text}`, config.tick.label);
        this.labelsMap.set(text, label);
        label.resolution = drawingConfiguration.resolution;
        label.y = 0 - this.topMargin / 2 - label.height / 2;
        label.x = position - this.scrollPosition;
        return label;
    }

    updateLabel(text, position) {
        let label = this.labelsMap.get(text);
        if (this.isInFrame(position)) {
            if (label){
                this.mainContainer.removeChild(label);
            }
            label = this.createLabel(text, position);
            this.mainContainer.addChild(label);
        } else {
            this.mainContainer.removeChild(label);
        }
    }

    createScrollBar() {
        const pixiEvent$ = new Subject();
        const toStream = (e) => pixiEvent$.onNext(e);
        let subscription;

        this.scrollContainer.x = 0;
        this.scrollContainer.y = this.containerHeight + config.scrollBar.margin;
        this.scrollContainer.interactive = true;
        this.scrollContainer.buttonMode = true;
        this.scrollContainer.addChild(this.scrollBar);
        this.drawScrollBar();

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
        this.scrollContainer.on('mousedown', (event) => {
            const scrollingStartPosition = event.data.global.x;
            const currentScrollPosition = this.scrollPosition;
            subscription = pixiEvent$.subscribe((e) => {
                const deltaFromMouseDown = e.data.global.x - scrollingStartPosition;

                const delta = this.convertScrollCoordinateToDrawingCoordinate(deltaFromMouseDown);

                this.scrollPosition = Math.max(
                    0,
                    Math.min(
                        currentScrollPosition + delta,
                        this.actualDrawingWidth - this.containerWidth
                    )
                );
                this.rerender();
            });
        });
        this.scrollContainer.on('mousemove', (e) => {
            if (subscription) {
                toStream(e);
            }
        });
    }

    rerender() {
        this.drawScrollBar();
        this.buildColumns();
        requestAnimationFrame(() => this.renderer.render(this.container));
    }

    /**
     * Converts actual coordinate (in pixels) to the coordinate
     * on the scroller (in pixels)
     * @param x
     * @returns {number}
     */
    convertDrawingCoordinateToScrollCoordinate(x) {
        const total = this.actualDrawingWidth;
        const frame = this.containerWidth;
        if (total === 0 || frame === 0) {
            return 0;
        }
        const ratio = frame / total;
        return x * ratio;
    }

    /**
     * Converts coordinate on scroller (in pixels) to the actual
     * drawing coordinate (in pixels)
     * @param x
     * @returns {number}
     */
    convertScrollCoordinateToDrawingCoordinate(x) {
        const total = this.actualDrawingWidth;
        const frame = this.containerWidth;
        if (total === 0 || frame === 0) {
            return 0;
        }
        const ratio = total / frame;
        return x * ratio;
    }

    /**
     * `drawScrollBar` renders scroll bar based on `this.scrollPosition`, `this.actualDrawingWidth` and
     * `this.containerWidth` values
     */
    drawScrollBar() {
        this.scrollBar.clear();
        const total = this.actualDrawingWidth;
        const frame = this.containerWidth;
        if (frame >= total) {
            return;
        }
        const scrollBarWidth = this.convertDrawingCoordinateToScrollCoordinate(frame);
        const scrollBarX = this.convertDrawingCoordinateToScrollCoordinate(this.scrollPosition);

        this.scrollBar
            .beginFill(config.scrollBar.slider.fill, 0)
            .lineStyle(1, config.scrollBar.slider.fill, 0.5)
            .drawRect(
                0,
                0,
                frame,
                config.scrollBar.height
            )
            .endFill();

        this.scrollBar
            .beginFill(config.scrollBar.slider.fill, 0.5)
            .drawRect(
                scrollBarX,
                0,
                scrollBarWidth,
                config.scrollBar.height
            )
            .endFill();
    }

    sortHitsByLength(hits) {
        return hits.sort((hit1, hit2) => (hit2.endIndex - hit2.startIndex) - (hit1.endIndex - hit1.startIndex));
    }
    resizeScroll(width) {
        this.canvasSize.width = width;
        this.scrollBar.clear();
        this.scrollContainer.removeAllListeners();
        if (this.containerWidth < this.actualDrawingWidth) {
            this.isScrollable = true;
            this.activateMask();
            this.createScrollBar();
            this.buildColumns();

        } else {
            this.isScrollable = false;
            requestAnimationFrame(() => this.renderer.render(this.container));
        }
    }
}
