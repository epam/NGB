import PIXI from 'pixi.js';
import {PixiTextSize} from '../../../utilities';
import  {Subject} from 'rx';

import ExpandedState from './expandedState';
import config from '../../whole-genome-config';
import {ColorProcessor} from '../../../utilities';
import {
    drawingConfiguration
} from '../../../core';

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

function getChromosomeLabelText(chromosome) {
    if (!chromosome) {
        return '';
    }
    return `Chr ${chromosome.name}`;
}

export class ChromosomeColumnRenderer {
    actualDrawingWidth = 0;
    scrollContainer = new PIXI.Container();
    mainContainer = new PIXI.Container();
    scrollBar = new PIXI.Graphics();
    columnsMask = new PIXI.Graphics();
    chromosomesContainer = new PIXI.Container();
    labelsMap = new Map();
    expandButton = {};
    expandButtonHovered = {};
    hoveredHit = null;
    currentHitView = null;
    /**
     * `scrollPosition` is a current scroll offset for the canvas in pixels,
     * i.e. `scrollOffset` == 10px means that we scrolled canvas to the right at 10px -position
     * @type {number}
     */
    scrollPosition = 0;
    gridContent = {};
    gridSizes = {};
    scoreConfigs = buildScoresConfigs();
    /**
     * Stores per-chromosome graphics
     * @type {{number: PIXI.Graphics}}
     */
    chromosomesGraphics = {};
    /**
     * Stores per-chromosome expanded/collapsed state
     * @type {{number: ExpandedState}}
     */
    chromosomesExpandedState = {};
    /**
     * Stores per-chromosome label width
     * @type {{number: number}}
     */
    chromosomesLabelsWidth = {};

    constructor({
        container,
        canvasSize,
        chromosomes,
        range: maxChrSize,
        hits,
        renderer,
        displayTooltipFn
    }) {

        Object.assign(this, {
            container,
            canvasSize,
            chromosomes,
            maxChrSize,
            hits,
            renderer,
            displayTooltipFn
        });
    }

    destroy () {
        Object.values(this.chromosomesExpandedState || {})
            .forEach(state => state.destroyAnimation());
    }

    get mask() {
        return this.columnsMask;
    }
    get height() {
        return this.canvasSize ? this.canvasSize.height : 0;
    }
    get containerWidth() {
        return this.canvasSize ? this.canvasSize.width - 2 * config.canvas.margin : 0;
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

    isInFrame(x1, x2) {
        return x2 >= this.scrollPosition && x1 <= (this.scrollPosition + this.containerWidth);
    }

    init() {
        this.preprocessHits();
        this.preprocessChromosomes();
        this.calculateGridContent();
        this.reCalculateActualDrawingWidth();
        this.mainContainer.addChild(this.chromosomesContainer);
        this.renderChromosomes();
        this.createScrollBar();
        this.activateMask();
        this.mainContainer.addChild(this.scrollContainer);
        this.mainContainer.x = config.axis.canvasWidth;
        this.mainContainer.y = config.start.topMargin;
        this.container.addChild(this.mainContainer);
    }

    reCalculateActualDrawingWidth() {
        this.actualDrawingWidth = this.chromosomes.reduce((r, c) => r + this.getChromosomeAreaWidth(c), 0);
    }

    /**
     * Changes chromosome expanded state
     * @param chromosome
     * @return {boolean} true if state was changed, false otherwise
     */
    toggleChromosomeExpand(chromosome) {
        const state = this.chromosomesExpandedState[chromosome.id];
        if (state && state.expandable) {
            state.setExpandedAnimated(!state.expanded);
            this.reCalculateActualDrawingWidth();
            this.rerender({reRenderChromosomes: [chromosome.id]});
            return true;
        }
        return false;
    }

    getChromosomeAreaWidth(chromosome) {
        if (!chromosome) {
            return 0;
        }
        const expandedState = this.chromosomesExpandedState[chromosome.id];
        const expandFactor = expandedState && expandedState.expandFactor;
        const columns = (this.gridSizes[chromosome.id] || 0);
        const hitsSizeWithChromosome = columns * config.gridSize
            + 2 * config.chromosomeArea.margin
            + this.columnWidth;
        const minimumSize = config.chromosomeArea.minimum;
        const labelSize = this.chromosomesLabelsWidth[chromosome.id] || 0;
        const maximumSize = Math.min(
            config.chromosomeArea.maximum,
            Math.max(
                hitsSizeWithChromosome,
                labelSize,
                config.chromosomeArea.minimum
            )
        ) + config.chromosomeArea.expand.width;
        return minimumSize + expandFactor * (maximumSize - minimumSize);
    }
    /**
     * Renders chromosomes graphics
     * @param options {object}
     * @param options.reRenderChromosomes {number[]} chromosomes identifiers to re-render
     */
    renderChromosomes(options = {}) {
        const {
            reRenderChromosomes = []
        } = options;
        this.chromosomesContainer.x = -this.scrollPosition;
        this.chromosomesContainer.y = 0;
        let incrementedPosition = 0;
        for (let i = 0; i < this.chromosomes.length; i++) {
            const chr = this.chromosomes[i];
            const position = incrementedPosition;
            const chromosomeAreaWidth = this.getChromosomeAreaWidth(chr);
            incrementedPosition += chromosomeAreaWidth;
            if (this.isInFrame(position, position + chromosomeAreaWidth)) {
                let graphics = this.chromosomesGraphics[chr.id];
                let reRender = reRenderChromosomes.indexOf(chr.id) >= 0;
                if (!graphics) {
                    graphics = new PIXI.Graphics();
                    const chromosomeHeightPx = this.convertToPixels(chr.size, this.maxChrSize, this.containerHeight);
                    const actualChromosomeHeight = Math.ceil(chromosomeHeightPx / config.gridSize) * config.gridSize;
                    const onMousemove = (event) => {
                        const { x: mouseX, y: mouseY} = graphics.toLocal(event.data.global);
                             
                        if (
                            (mouseX >= 0 && mouseX <= graphics.width) &&
                            (mouseY >= 0 && mouseY <= graphics.height)
                        ) {
                            const hoveredHit = this.hoveredHit;
                            this.hoveredHit = null;
                            const chromosomeHits = this.gridContent[chr.id].filter(hit => hit.displayed) || [];
                            if (mouseY <= actualChromosomeHeight) {  
                                for (const hit of chromosomeHits) {
                                    if (
                                        (hit.x_area.from <= mouseX && hit.x_area.to >= mouseX) &&
                                        (hit.y_area.from <= mouseY && hit.y_area.to >= mouseY)
                                    ) {
                                        this.hoveredHit = hit;
                                        break;
                                    }
                                }
                            } else if (this.hoveredHit) {
                                this.clearSelection(chr);
                            }
                            if (hoveredHit !== this.hoveredHit) {
                                this.displayTooltipFn(event.data.global, this.hoveredHit);
                                this.renderChromosomes({
                                    reRenderChromosomes: [chr.id]
                                });
                                requestAnimationFrame(() => this.renderer.render(this.container));
                            }
                        }
                    };
                    graphics.interactive = true;
                    graphics.buttonMode = true;
                    graphics.on('mousemove', onMousemove);
                    graphics.on('mousedown', () => {
                        this.toggleChromosomeExpand(chr);
                    });
                    graphics.on('mouseout', () => {
                        if (this.expandButton[chr.id]) {
                            this.expandButtonHovered[chr.id] = false;
                        }
                        this.clearSelection(chr);
                    });
                    graphics.on('mouseover', () => {
                        if (this.expandButton[chr.id]) {
                            this.expandButtonHovered[chr.id] = true;
                            this.rerender({
                                reRenderChromosomes: [chr.id]
                            });
                        }
                    });
                    this.chromosomesGraphics[chr.id] = graphics;
                    this.chromosomesContainer.addChild(graphics);
                    reRender = true;
                }
                graphics.x = position;
                if (reRender) {
                    const options = {
                        areaWidth: chromosomeAreaWidth,
                        chromosomeHeightPx: this.convertToPixels(chr.size, this.maxChrSize, this.containerHeight),
                        odd: i % 2,
                        totalHeightPx: this.containerHeight
                    };
                    graphics.clear();
                    this.renderChromosome(graphics, chr, options);
                }           
            } else if (this.chromosomesGraphics[chr.id]) {
                const graphics = this.chromosomesGraphics[chr.id];
                graphics.removeAllListeners();
                this.chromosomesContainer.removeChild(graphics);
                delete this.chromosomesGraphics[chr.id];
            }
            this.renderChromosomeLabel(chr, position, chromosomeAreaWidth);
        }
    }

    preprocessHits() {
        (this.hits || []).forEach(hit => {
            const scoreConfig = this.scoreConfigs.filter(c => c.test(hit.score)).pop();
            hit.scoreIndex = Math.max(0, this.scoreConfigs.indexOf(scoreConfig));
        });
    }

    preprocessChromosomes() {
        this.chromosomesLabelsWidth = this.chromosomes
            .map(chromosome => ({
                [chromosome.id]: PixiTextSize
                    .getTextSize(getChromosomeLabelText(chromosome), config.chromosomeArea.label.style)
                    .width + config.chromosomeArea.label.margin
            }))
            .reduce((res, cur) => ({...res, ...cur}));
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
                const [chr] = this.chromosomes.filter((chr) => chr.name === chrName);
                gridContent[chr.id] = [];
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
                    gridContent[chr.id].push({
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
        this.chromosomesExpandedState = this.chromosomes
            .map((chromosome) => {
                const hitsOutOfMinimumArea = (
                    config.chromosomeColumn.width +
                    2 * config.chromosomeArea.margin +
                    (this.gridSizes[chromosome.id] || 0) * config.gridSize
                ) >= config.chromosomeArea.minimum;
                const chromosomeNameOutOfArea = this.chromosomesLabelsWidth[chromosome.id] >
                    config.chromosomeArea.minimum;
                const chromosomeLabelText = getChromosomeLabelText(chromosome);
                return {
                    [chromosome.id]: new ExpandedState(
                        hitsOutOfMinimumArea || chromosomeNameOutOfArea,
                        PixiTextSize.trimTextToFitWidth(
                            chromosomeLabelText,
                            config.chromosomeArea.minimum - config.chromosomeArea.label.margin,
                            config.chromosomeArea.label.style
                        ),
                        () => {
                            this.rerender({
                                reRenderChromosomes: [chromosome.id]
                            });
                        }
                    )
                };
            })
            .reduce((r, c) => ({...r, ...c}), {});
    }

    renderExpandChromosomeArea(graphics, chromosome, options = {}) {
        const {
            areaWidth = 0,
            totalHeightPx = 0
        } = options;
        if (!areaWidth || !totalHeightPx) {
            return;
        }
        const expandedState = this.chromosomesExpandedState[chromosome.id];
        const expanded = expandedState && expandedState.expanded;
        const expandable = expandedState && expandedState.expandable;
        const xLimit = expandable
            ? (areaWidth - config.chromosomeArea.expand.width)
            : areaWidth;
        if (expandable) {
            this.expandButton[chromosome.id] = {
                x1: xLimit,
                x2: xLimit + config.chromosomeArea.expand.width
            };
            const arrowY = Math.round(totalHeightPx / 2.0);
            const arrowX = Math.round(
                areaWidth
                - config.chromosomeArea.expand.width / 2.0
                - config.chromosomeArea.expand.arrow / 2.0
            );
            graphics
                .lineStyle(0, 0x0, 0)
                .beginFill(
                    this.expandButtonHovered[chromosome.id]
                    ? ColorProcessor.darkenColor(config.chromosomeArea.expand.fill, 0.05)
                    : config.chromosomeArea.expand.fill,
                    config.chromosomeArea.expand.alpha
                )
                .drawRect(
                    xLimit,
                    0,
                    config.chromosomeArea.expand.width,
                    totalHeightPx
                )
                .endFill();
            if (expanded) {
                graphics
                    .lineStyle(1, config.chromosomeArea.expand.color, 1)
                    .moveTo(arrowX, arrowY)
                    .lineTo(arrowX + config.chromosomeArea.expand.arrow, arrowY)
                    .moveTo(
                        arrowX + config.chromosomeArea.expand.arrow / 2.0,
                        arrowY - config.chromosomeArea.expand.arrow / 2.0
                    )
                    .lineTo(
                        arrowX,
                        arrowY
                    )
                    .lineTo(
                        arrowX + config.chromosomeArea.expand.arrow / 2.0,
                        arrowY + config.chromosomeArea.expand.arrow / 2.0
                    );
            } else {
                graphics
                    .lineStyle(1, config.chromosomeArea.expand.color, 1)
                    .moveTo(arrowX, arrowY)
                    .lineTo(arrowX + config.chromosomeArea.expand.arrow, arrowY)
                    .moveTo(
                        arrowX + config.chromosomeArea.expand.arrow / 2.0,
                        arrowY - config.chromosomeArea.expand.arrow / 2.0
                    )
                    .lineTo(
                        arrowX + config.chromosomeArea.expand.arrow,
                        arrowY
                    )
                    .lineTo(
                        arrowX + config.chromosomeArea.expand.arrow / 2.0,
                        arrowY + config.chromosomeArea.expand.arrow / 2.0
                    );
            }
        }
    }

    renderChromosome(graphics, chromosome, options = {}) {
        const {
            areaWidth,
            chromosomeHeightPx,
            odd
        } = options;
        const chromosomeHeightCorrected = Math.ceil(chromosomeHeightPx / config.gridSize) * config.gridSize;
        if (odd) {
            graphics
                .beginFill(config.grid.oddChromosomeBackground, 1)
                .drawRect(
                    0,
                    0,
                    areaWidth - 1,
                    this.containerHeight)
                .endFill();
        } else {
            graphics
                .beginFill(config.grid.oddChromosomeBackground, 0)
                .drawRect(
                    0,
                    0,
                    areaWidth - 1,
                    this.containerHeight)
                .endFill();
        }
        graphics
            .beginFill(config.chromosomeColumn.fill, 1)
            .drawRect(
                0,
                0,
                this.columnWidth,
                chromosomeHeightCorrected)
            .endFill()
            .lineStyle(config.chromosomeColumn.thickness, config.chromosomeColumn.lineColor, 1)
            .moveTo(0, 0)
            .lineTo(this.columnWidth, 0)
            .lineTo(this.columnWidth, chromosomeHeightCorrected)
            .lineTo(0, chromosomeHeightCorrected)
            .lineTo(0, 0);
        if (this.gridContent && this.gridContent[chromosome.id]) {
            const expandedState = this.chromosomesExpandedState[chromosome.id];
            const expandable = expandedState && expandedState.expandable;
            const initialMargin = this.columnWidth + config.chromosomeArea.margin;
            const xLimit = expandable
                ? (areaWidth - config.chromosomeArea.expand.width)
                : areaWidth;
            this.renderExpandChromosomeArea(graphics, chromosome, options);
            for (let c = 0; c < this.scoreConfigs.length; c++) {
                const scoreConfig = this.scoreConfigs[c];
                const hits = this.gridContent[chromosome.id].filter(hit => hit.scoreIndex === c);
                if (hits.length > 0) {
                    graphics
                        .lineStyle(config.chromosomeColumn.thickness, config.chromosomeColumn.lineColor, 0)
                        .beginFill(scoreConfig.color, 1);
                    hits.forEach(hit => {
                        const {
                            start,
                            end,
                            x
                        } = hit;
                        const x1 = initialMargin + (x - 1) * config.gridSize;
                        const x2 = x1 + config.gridSize - 1;
                        hit.displayed = x2 < xLimit;
                        if (hit.displayed) {
                            graphics
                                .lineStyle(1, config.hit.onHover.lineColor, hit === this.hoveredHit ? 1 : 0)
                                .drawRect(
                                    x1,
                                    start * config.gridSize,
                                    x2 - x1,
                                    (end - start) * config.gridSize - 1
                                );

                            hit.x_area = {
                                from: x1,
                                to: x2 + 1
                            };
                            hit.y_area = {
                                from: start * config.gridSize,
                                to: end * config.gridSize
                            };
                        }
                    });
                    graphics
                        .endFill();
                }
            }
        }
    }

    convertToPixels(size, realRange, pixelRange) {
        return (size / realRange) * pixelRange;
    }

    renderChromosomeLabel(chromosome, position, width) {
        let label = this.labelsMap.get(chromosome.id);
        if (this.isInFrame(position, position + width)) {
            const expandedConfig = this.chromosomesExpandedState[chromosome.id];
            let chromosomeName = getChromosomeLabelText(chromosome);
            if (expandedConfig && !expandedConfig.expanded && expandedConfig.trimmedLabelText) {
                chromosomeName = expandedConfig.trimmedLabelText;
            }
            if (!label) {
                label = new PIXI.Text(chromosomeName, config.chromosomeArea.label.style);
                label.resolution = drawingConfiguration.resolution;
                label.y = 0 - this.topMargin / 2 - label.height / 2;
                this.labelsMap.set(chromosome.id, label);
                this.mainContainer.addChild(label);
            }
            label.text = chromosomeName;
            label.x = position - this.scrollPosition;
        } else {
            this.mainContainer.removeChild(label);
            this.labelsMap.delete(chromosome.id);
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
        this.renderScrollBar();

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
            const scrollingStartPosition = event.data.getLocalPosition(event.target).x;
            const scrollerX1 = this.convertDrawingCoordinateToScrollCoordinate(this.scrollPosition);
            const scrollerX2 = this.convertDrawingCoordinateToScrollCoordinate(this.scrollPosition + this.containerWidth);
            if (scrollingStartPosition < scrollerX1 || scrollingStartPosition > scrollerX2) {
                // user clicked outside of scroller;
                // we should scroll to clicked position
                const newScrollPosition = this.convertScrollCoordinateToDrawingCoordinate(
                    scrollingStartPosition - (scrollerX2 - scrollerX1) / 2.0
                );
                this.scrollPosition = Math.max(
                    0,
                    Math.min(
                        newScrollPosition,
                        this.actualDrawingWidth - this.containerWidth
                    )
                );
                this.rerender();
            }
            const currentScrollPosition = this.scrollPosition;
            subscription = pixiEvent$.subscribe((e) => {
                const deltaFromMouseDown = e.data.getLocalPosition(event.target).x - scrollingStartPosition;

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
            this.renderScrollBar();
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

    /**
        * Re-renders graphics
        * @param options {object}
        * @param options.reRenderChromosomes {number[]} chromosomes identifiers to re-render
        */
    rerender(options) {
        this.renderScrollBar();
        this.renderChromosomes(options);
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
        * `renderScrollBar` renders scroll bar based on `this.scrollPosition`, `this.actualDrawingWidth` and
        * `this.containerWidth` values
        */
    renderScrollBar() {
        this.scrollBar.clear();
        const total = this.actualDrawingWidth;
        const frame = this.containerWidth;
        if (frame >= total) {
            return;
        }
        const scrollBarWidth = this.convertDrawingCoordinateToScrollCoordinate(frame);
        const scrollBarX = this.convertDrawingCoordinateToScrollCoordinate(this.scrollPosition);

        this.scrollBar
            .beginFill(config.scrollBar.slider.fill, 0.1)
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
        this.activateMask();
        this.createScrollBar();
        this.rerender();
    }
    hideHitInfo() {
        this.displayTooltipFn(null, null);
    }

    /**
    * `clearSelection` clear all hover effects and tooltips from selected chromosome area
    */
    clearSelection(chr){
        this.hideHitInfo();
        this.hoveredHit = null;
        this.renderChromosomes({
            reRenderChromosomes: [chr.id]
        });
        requestAnimationFrame(() => this.renderer.render(this.container));
    }
}
