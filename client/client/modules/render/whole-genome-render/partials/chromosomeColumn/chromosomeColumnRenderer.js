import PIXI from 'pixi.js';
import {
    Subject
} from 'rx';

import {
    drawingConfiguration
} from '../../../core';
import config from '../../whole-genome-config';

function buildScoresConfigs() {
    return ([
        {
            default: true,
            lineColor: config.hit.color
        },
        ...(config.hit.scores || [])
    ])
        .map(score => ({
            max: Infinity,
            min: -Infinity,
            test(value) {
                if (value === undefined) {
                    return this.default;
                }
                return Number(value) >= this.min && Number(value) <= this.max;
            },
            ...score
        }));
}

export class ChromosomeColumnRenderer {

    scrollContainer = new PIXI.Container();
    mainContainer = new PIXI.Container();
    scrollBar = new PIXI.Graphics();
    columnsMask = new PIXI.Graphics();
    columns = new PIXI.Graphics();
    isScrollable = false;
    isHover = false;
    labelsMap = new Map();
    /**
     * `scrollPosition` is a current scroll offset for the canvas in pixels,
     * i.e. `scrollOffset` == 10px means that we scrolled canvas to the right at 10px -position
     * @type {number}
     */
    scrollPosition = 0;
    gridContent = {};
    gridSizes = {};
    scoreConfigs = buildScoresConfigs();

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
    getGridStart(nucleotide, chrSize, chrPixelValue) {
        return (Math.round(this.convertToPixels(nucleotide, chrSize, chrPixelValue) / config.gridSize));
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
        this.preprocessHits();
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
                this.createColumn(position, pixelSize, chr, !(i % 2));
            }
            this.updateLabel(chr.id, position);

        }
    }

    preprocessHits() {
        (this.hits || []).forEach(hit => {
            const scoreConfig = this.scoreConfigs.filter(c => c.test(hit.score)).pop();
            hit.scoreIndex = Math.max(0, this.scoreConfigs.indexOf(scoreConfig));
        });
    }

    calculateGridContent() {
        const sortedHits = this.sortHits(this.hits);
        const groupedHitsByChromosome = sortedHits.reduce((acc, hit) => {
            acc[hit.chromosome] = (acc[hit.chromosome] || []).concat(hit);
            return acc;
        }, {});
        const gridContent = {};

        const getFirstNotOccupiedLevel = (grid, from, to) => {
            const occupiedLevels = new Set(
                grid
                .slice(from, to)
                .reduce((result, currentLevels) => ([...result, ...currentLevels]), [])
            );
            const max = Math.max(...occupiedLevels, 0);
            if (occupiedLevels.size === max) {
                return max + 1;
            }
            const occupiedLevelsArray = [...occupiedLevels];
            for (let idx = 0; idx < occupiedLevelsArray.length; idx++) {
                if (occupiedLevelsArray[idx] !== idx + 1) {
                    return idx + 1;
                }
            }
            return max + 1;
        };

        const insertLevel = (level, grid, from, to) => {
            const updateRow = row => [
                ...new Set([...(row || []), level])
            ].sort();
            for (let r = from; r < to; r++) {
                grid[r] = updateRow(grid[r]);
            }
        };

        for (const chrName in groupedHitsByChromosome) {
            if (groupedHitsByChromosome.hasOwnProperty(chrName)) {
                gridContent[chrName] = [];
                const [chr] = this.chromosomes.filter((chr) => chr.name === chrName);
                const pixelSize = this.convertToPixels(chr.size, this.maxChrSize, this.containerHeight);
                const rowsCount = Math.ceil(pixelSize / config.gridSize);
                const pixelGrid = Array(rowsCount).fill([]);
                groupedHitsByChromosome[chrName].forEach((hit) => {
                    let start = this.getGridStart(hit.startIndex, chr.size, pixelSize);
                    const lengthPx = this.getPixelLength(hit.startIndex, hit.endIndex, chr.size, pixelSize);
                    const lengthInGridCells = Math.max(1, Math.ceil(lengthPx / config.gridSize));
                    const end = Math.min(start + lengthInGridCells, rowsCount);
                    start = end - lengthInGridCells; // end-of-chromosome correction
                    const currentLevel = getFirstNotOccupiedLevel(pixelGrid, start, end);
                    insertLevel(currentLevel, pixelGrid, start, end);
                    gridContent[chrName].push({
                        ...hit,
                        start,
                        end,
                        x: currentLevel,
                    });
                });
            }
        }
        this.gridContent = gridContent;
        this.gridSizes = Object
            .entries(gridContent)
            .map(([chromosome, hits]) => ({
                [chromosome]: Math.max(...hits.map(hit => hit.x))
            }))
            .reduce((r, c) => ({...r, ...c}), {});
    }

    createColumn(position, chrPixelValue, chromosome, odd) {
        const chromosomeHeightCorrected = Math.ceil(chrPixelValue / config.gridSize) * config.gridSize;
        if (odd) {
            this.columns
                .beginFill(0xfafafa, 1)
                .drawRect(
                    position - config.chromosomeColumn.margin / 2.0,
                    0,
                    this.chrBlockWidth,
                    this.containerHeight)
                .endFill();
        }
        this.columns
            .beginFill(config.chromosomeColumn.fill, 1)
            .drawRect(
                position,
                0,
                this.columnWidth,
                chromosomeHeightCorrected)
            .endFill()
            .lineStyle(config.chromosomeColumn.thickness, config.chromosomeColumn.lineColor, 1)
            .moveTo(position, 0)
            .lineTo(position + this.columnWidth, 0)
            .lineTo(position + this.columnWidth, chromosomeHeightCorrected)
            .lineTo(position, chromosomeHeightCorrected)
            .lineTo(position, 0);
        if (this.gridContent && this.gridContent[chromosome.name]) {
            const initialMargin = position + this.columnWidth + config.chromosomeColumn.margin;
            for (let c = 0; c < this.scoreConfigs.length; c++) {
                const scoreConfig = this.scoreConfigs[c];
                const hits = this.gridContent[chromosome.name].filter(hit => hit.scoreIndex === c);
                if (hits.length > 0) {
                    this.columns
                        .lineStyle(config.chromosomeColumn.thickness, config.chromosomeColumn.lineColor, 0)
                        .beginFill(scoreConfig.color, 1);
                    hits.forEach((hit, hitIndex) => {
                        const {
                            start,
                            end,
                            x
                        } = hit;
                        if (x <= this.hitsLimit) {
                            const startDrawPosX = initialMargin + (x - 1) * config.gridSize;
                            const startDrawPosY = start * config.gridSize;
                            const hitLength = (end - start) * config.gridSize - 1;
                            this.columns
                                .drawRect(
                                    startDrawPosX,
                                    startDrawPosY,
                                    config.gridSize - 1,
                                    hitLength
                                );
                            this.gridContent[chromosome.name][hitIndex].x_area = {
                                from: startDrawPosX,
                                to: startDrawPosX + config.gridSize - 1
                            };
                            this.gridContent[chromosome.name][hitIndex].y_area = {
                                from: startDrawPosY,
                                to: startDrawPosY + hitLength
                            };
                        }
                    });
                    this.columns
                        .endFill();
                }
            }
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
            if (label) {
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
        const toggleHover = (isOn = true) => {
            this.isHover = !!isOn;
            this.drawScrollBar();
            requestAnimationFrame(() => this.renderer.render(this.container));
        };
        this.scrollBar.interactive = true;
        this.scrollBar.on('mouseover', toggleHover);
        this.scrollBar.on('pointerover', toggleHover);
        this.scrollBar.on('mouseout', () => toggleHover(false));
        this.scrollBar.on('pointerout', () => toggleHover(false));
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
            .beginFill(config.scrollBar.slider.fill, this.isHover ? 0.8 : 0.5)
            .drawRect(
                scrollBarX,
                0,
                scrollBarWidth,
                config.scrollBar.height
            )
            .endFill();
    }

    sortHits(hits) {
        function lengthSorter(a, b) {
            return (b.endIndex - b.startIndex) - (a.endIndex - b.startIndex);
        }
        function scoreSorter(a, b) {
            return a.scoreIndex - b.scoreIndex;
        }
        return hits.sort(lengthSorter).sort(scoreSorter);
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
