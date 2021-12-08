import * as PIXI from 'pixi.js-legacy';
import {CachedTrackRenderer} from '../../../../../core';
import {ColorProcessor} from '../../../../../utilities';
import CoordinateSystem from '../../../../common/coordinateSystemRenderer';

class VCFSampleCoverageRenderer extends CachedTrackRenderer {
    get height() {
        return this._height;
    }

    set height(value) {
        this._height = value;
    }

    constructor(config, track) {
        super(track);
        this._config = config;
        this._colors = track && track.config && track.config.colors
            ? track.config.colors
            : {};
        this.graphics = new PIXI.Graphics();
        this.hoveredGraphics = new PIXI.Graphics();
        this.dataContainer.addChild(this.graphics);
        this.dataContainer.addChild(this.hoveredGraphics);
        this.initializeCentralLine();
    }

    translateContainer(viewport, cache) {
        super.translateContainer(viewport, cache);
        this.hoveredGraphics.clear();
    }

    renderCoverageItem(item, options = {}) {
        const {
            x1,
            x2,
            viewport = this._track ? this._track.viewport : undefined,
            graphics = this.graphics,
            converter,
            base: baseValue,
            hovered = false
        } = options;
        if (!viewport || typeof converter !== 'function') {
            return;
        }
        const getYPosition = o => this.height - converter(o);
        const base = baseValue || getYPosition(0);
        const height = converter(item.coverage.total || 0);
        const hasNucleotideData = item.coverage.A ||
            item.coverage.C ||
            item.coverage.G ||
            item.coverage.T ||
            item.coverage.N;
        const config = this._config.coverage;
        if (hasNucleotideData) {
            const getNucleotideData = nucleotide => item.coverage[(nucleotide || '').toUpperCase()] || 0;
            const totalCorrected = item.coverage.total -
                getNucleotideData('A') -
                getNucleotideData('C') -
                getNucleotideData('G') -
                getNucleotideData('T') -
                getNucleotideData('N');
            const barInfo = [
                {
                    value: getNucleotideData('A'),
                    color: this._colors.A || config.color
                },
                {
                    value: getNucleotideData('C'),
                    color: this._colors.C || config.color
                },
                {
                    value: getNucleotideData('G'),
                    color: this._colors.G || config.color
                },
                {
                    value: getNucleotideData('T'),
                    color: this._colors.T || config.color
                },
                {
                    value: getNucleotideData('N'),
                    color: this._colors.N || 0x000000
                }
            ]
                .filter(o => o.value)
                .sort((a, b) => b.value - a.value);
            barInfo.splice(
                0,
                0,
                {
                    value: totalCorrected,
                    color: config.color
                }
            );
            let previous = base;
            for (const bar of barInfo) {
                const barHeight = converter(bar.value);
                graphics
                    .beginFill(
                        hovered ? ColorProcessor.darkenColor(bar.color) : bar.color,
                        1
                    )
                    .drawRect(
                        x1,
                        previous - barHeight,
                        x2 - x1,
                        barHeight
                    )
                    .endFill();
                previous = previous - barHeight;
            }
        } else {
            graphics
                .beginFill(
                    hovered ? ColorProcessor.darkenColor(config.color) : config.color,
                    1
                )
                .drawRect(
                    x1,
                    base - height,
                    x2 - x1,
                    height
                )
                .endFill();
        }
    }

    rebuildContainer(viewport, cache) {
        super.rebuildContainer(viewport, cache);
        this.graphics.clear();
        this.hoveredGraphics.clear();
        if (cache && cache.coverage) {
            const {
                items = [],
                rawItems = [],
                maximum = 0,
                minimum = 0
            } = cache.coverage;
            if (maximum > minimum) {
                this.data = rawItems;
                this.coordinateSystem = {maximum, minimum};
                const converter = CoordinateSystem.getConverter({maximum, minimum}, this.height);
                const config = this._config.coverage;
                const bpWidth = Math.max(2, viewport.factor);
                if (bpWidth > config.barsThreshold) {
                    this.graphics.lineStyle(1, config.lineColor, 1);
                } else {
                    this.graphics.lineStyle(0, config.lineColor, 0);
                }
                this.graphics.beginFill(config.color, 1);
                const getYPosition = o => this.height - converter(o);
                const getXPosition = o => viewport.project.brushBP2pixel(o);
                const base = getYPosition(0);
                const correctX = x => Math.max(
                    -viewport.canvasSize,
                    Math.min(2.0 * viewport.canvasSize, x)
                );
                for (const item of items) {
                    const x1 = correctX(
                        Math.floor(
                            getXPosition(item.startIndex - 0.5)
                        )
                    );
                    const x2 = correctX(
                        Math.ceil(
                            getXPosition(item.endIndex + 0.5)
                        )
                    );
                    if (Math.abs(x1 - x2) < 1) {
                        continue;
                    }
                    if (x2 < -viewport.canvasSize || x1 > 2.0 * viewport.canvasSize) {
                        continue;
                    }
                    this.renderCoverageItem(item, {x1, x2, base, converter, viewport});
                }
                this.graphics.endFill();
            }
        }
    }

    getItemUnderPosition(position) {
        if (
            this._track &&
            this._track.viewport &&
            position &&
            this.data &&
            this.data.length
        ) {
            const {x} = position;
            const hoverOffset = 2;
            const x1 = Math.floor(x - hoverOffset);
            const x2 = Math.ceil(x + hoverOffset);
            const bp1 = Math.floor(
                this._track.viewport.project.pixel2brushBP(x1)
            );
            const bp2 = Math.ceil(
                this._track.viewport.project.pixel2brushBP(x2)
            );
            const bp = (bp1 + bp2) / 2.0;
            const [best] = this.data
                .filter(item => bp1 <= item.startIndex && bp2 >= item.startIndex)
                .map(item => ({item, diff: Math.abs(bp - (item.startIndex + item.endIndex) / 2.0)}))
                .sort((a, b) => a.diff - b.diff);
            return best ? best.item : undefined;
        }
        return undefined;
    }

    onClick() {
        return undefined;
    }

    onMove(position) {
        const item = this.getItemUnderPosition(position);
        this.hoveredGraphics.clear();
        const viewport = this._track ? this._track.viewport : undefined;
        if (item && viewport && this.coordinateSystem) {
            const config = this._config.coverage;
            const bpWidth = Math.max(2, viewport.factor);
            if (bpWidth > config.barsThreshold) {
                this.hoveredGraphics.lineStyle(
                    1,
                    ColorProcessor.darkenColor(config.lineColor, 0.25),
                    1
                );
            } else {
                this.hoveredGraphics.lineStyle(0, config.lineColor, 0);
            }
            const converter = CoordinateSystem.getConverter(
                this.coordinateSystem,
                this.height
            );
            this.hoveredGraphics.beginFill(
                ColorProcessor.darkenColor(config.color, 0.25),
                1
            );
            const getYPosition = o => this.height - converter(o);
            const getXPosition = o => viewport.project.brushBP2pixel(o);
            const base = getYPosition(0);
            const correctX = x => Math.max(
                -viewport.canvasSize,
                Math.min(2.0 * viewport.canvasSize, x)
            );
            const x1 = Math.floor(
                correctX(
                    Math.floor(
                        getXPosition(item.startIndex - 0.5)
                    )
                )
            );
            const x2 = Math.ceil(
                correctX(
                    Math.ceil(
                        getXPosition(item.endIndex + 0.5)
                    )
                )
            );
            if (Math.abs(x1 - x2) < 1) {
                return;
            }
            if (x2 < -viewport.canvasSize || x1 > 2.0 * viewport.canvasSize) {
                return;
            }
            this.renderCoverageItem(
                item,
                {
                    x1,
                    x2,
                    base,
                    converter,
                    viewport,
                    graphics: this.hoveredGraphics,
                    hovered: true
                }
            );
            this.hoveredGraphics.endFill();
        }
        return item;
    }

    animate() {
        return false;
    }
}

export {VCFSampleCoverageRenderer};
