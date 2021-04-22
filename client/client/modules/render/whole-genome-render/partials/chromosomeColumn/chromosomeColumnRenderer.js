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
    /**
     * `scrollPosition` is a current scroll offset for the canvas in pixels,
     * i.e. `scrollOffset` == 10px means that we scrolled canvas to the right at 10px -position
     * @type {number}
     */
    scrollPosition = 0;

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
    // get scrollBarWidth() {
    //     return Math.pow((this.canvasSize.width - config.scrollBar.slider.margin), 2) / this.actualDrawingWidth;
    // }
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
    // get scrollFactor() {
    //     return this.actualDrawingWidth / (this.canvasSize.width - config.scrollBar.slider.margin);
    // }

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
            this.mask.drawRect(0, 0, this.canvasSize.width + 1, this.height);
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

    /**
     * `buildColumns` renders chromosomes graphics
     */
    buildColumns() {
        // First, we need to clear graphics
        this.columns.clear();
        // Then, we should translate our graphics by `this.scrollPosition` offset
        // to the left: if we scrolled to the 100px position, then
        // graphics "moves" to the left by 100px;
        this.columns.x = -this.scrollPosition;
        this.columns.y = 0;
        console.log('build columns');
        for (let i = 0; i < this.chromosomes.length; i++) {
            const position = this.chrBlockWidth * i;
            const chr = this.chromosomes[i];
            const pixelSize = this.convertToPixels(chr.size, this.maxChrSize, this.containerHeight);
            // Moved this.updateLabel outside of this.createColumn;
            // this.createColumn no longer accepts 4th parameter (pos)
            console.log(
                `we will render chromosome #${chr.id} at screen position`,
                `${this.columns.x + position} ... ${this.columns.x + position + this.chrBlockWidth}`
            );
            this.createColumn(position, pixelSize, chr);
            this.updateLabel(`chr ${chr.id}`, position);
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

    // We don't need `updateColumnsByScroll` anymore

    // updateColumnsByScroll(params, localBounds) {
    //     this.columns.clear();
    //     let startPoint;
    //     if (
    //         params.currentPosition > 0 &&
    //         localBounds.x < params.end &&
    //         params.currentPosition < params.end
    //     ) {
    //         startPoint = localBounds.x * this.scrollFactor;
    //     } else if (localBounds.x <= 0 || params.currentPosition < 0) {
    //         startPoint = 0;
    //     } else if (
    //         localBounds.x >= params.end ||
    //         params.currentPosition >= params.end
    //     ) {
    //         startPoint = params.end * this.scrollFactor;
    //     }
    //     this.buildColumns(startPoint);
    // }

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
        const label = new PIXI.Text(text, config.tick.label);
        label.name = text;
        label.resolution = drawingConfiguration.resolution;
        label.y = 0 - this.topMargin / 2 - label.height / 2;
        label.x = position - this.scrollPosition;
        return label;
    }

    updateLabel(text, position) {
        let label = this.mainContainer.getChildByName(text);
        if (label) {
            this.mainContainer.removeChild(label);
        }
        label = this.createLabel(text, position);
        this.mainContainer.addChild(label);
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
            // `scrollingStartPosition` is a x-coordinate of the mouse `mouse down` event
            const scrollingStartPosition = event.data.global.x;
            // We will remember scroll position at the `mouse down` event
            const currentScrollPosition = this.scrollPosition;
            subscription = pixiEvent$.subscribe((e) => {
                // We can calculate `mouse move` delta (x coordinate)
                // by subtracting current position (e.data.global.x) and
                // the initial `mouse down` position (scrollingStartPosition).
                // Moreover, we can use any position (e.data.originalEvent.clientX,
                // e.data.originalEvent.screenX etc.) when calculating `scrollingStartPosition`
                // and `deltaFromMouseDown`: it will result to the same delta.
                const deltaFromMouseDown = e.data.global.x - scrollingStartPosition;

                // Now, `deltaFromMouseDown` is a value of "movement" on scroller; we need
                // to convert it to the "actual" (drawing) value in pixels
                const delta = this.convertScrollCoordinateToDrawingCoordinate(deltaFromMouseDown);

                // Now we can calculate a new scroll position by adding `delta` to
                // the `currentScrollPosition`.
                // We must consider scrolling bounds, i.e. we can't scroll outside of
                // range from 0 pixels to (actualDrawingWidth - canvasWidth)
                this.scrollPosition = Math.max(
                    0,
                    Math.min(
                        currentScrollPosition + delta,
                        this.actualDrawingWidth - this.containerWidth
                    )
                );
                // `this.scrollBarMove` call is removed since we calculate scroll position here
                // this.scrollBarMove(e, scrollParams);

                // `updateScrollBar` renamed to `rerender` since it re-renders all graphics
                this.rerender();
            });
        });
        this.scrollContainer.on('mousemove', (e) => {
            if (subscription) {
                toStream(e);
            }
        });
    }

    // we don't need `scrollBarMove` anymore

    // scrollBarMove(e, scrollParams) {
    //     const localBounds = this.scrollBar.getLocalBounds();
    //     if (
    //         scrollParams.end !== undefined &&
    //         scrollParams.start !== undefined &&
    //         e && e.data && e.data.originalEvent &&
    //         e.data.originalEvent.movementX
    //     ) {
    //         const delta = e.data.originalEvent.movementX;
    //         const local = localBounds.x + delta;
    //         scrollParams.currentPosition = local;
    //         this.updateScrollBar(scrollParams, localBounds);
    //     }
    // }

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
            // if nothing to draw (actualDrawingWidth == 0) or
            // canvas is not initialized or something wrong occurred (canvas.width == 0)
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
            // if nothing to draw (actualDrawingWidth == 0) or
            // canvas is not initialized or something wrong occurred (canvas.width == 0)
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
            // canvas size is larger then drawing area for all chromosomes;
            // we don't need a scroll
            return;
        }
        const scrollBarWidth = this.convertDrawingCoordinateToScrollCoordinate(frame);
        const scrollBarX = this.convertDrawingCoordinateToScrollCoordinate(this.scrollPosition);
        // Let's draw the scroll area
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
        // Let's draw the scroll bar
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

    // We don't need reDrawScrollBar anymore

    // reDrawScrollBar(params, localBounds) {
    //     const {
    //         start,
    //         end,
    //         currentPosition
    //     } = params;
    //     if (
    //         currentPosition <= end &&
    //         localBounds.x <= end
    //     ) {
    //         this.drawScrollBar(currentPosition);
    //     } else if (localBounds.x <= start) {
    //         this.drawScrollBar(params.start);
    //     } else {
    //         this.drawScrollBar(end);
    //     }
    // }

    sortHitsByLength(hits) {
        return hits.sort((hit1, hit2) => (hit2.endIndex - hit2.startIndex) - (hit1.endIndex - hit1.startIndex));
    }
    resizeScroll(width) {
        this.canvasSize.width = width;
        this.scrollBar.clear();
        this.scrollContainer.removeAllListeners();
        if (this.canvasSize.width < this.actualDrawingWidth) {
            this.isScrollable = true;
            this.activateMask();
            this.createScrollBar();

        } else {
            this.isScrollable = false;
            requestAnimationFrame(() => this.renderer.render(this.container));
        }
    }
}
