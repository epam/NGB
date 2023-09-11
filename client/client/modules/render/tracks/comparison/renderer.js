import * as PIXI from 'pixi.js-legacy';
import {CachedTrackRendererWithVerticalScroll} from '../../core';
import {ColorProcessor, PixiTextSize} from '../../utilities';

class ComparisonAlignmentRenderer extends CachedTrackRendererWithVerticalScroll {
    get config() {
        return this._config;
    }

    constructor(config, options, targetContext, track) {
        super(track);
        this._config = config;
        this.options = options;
        this._hoveringGraphics = new PIXI.Graphics();
        this.targetContext = targetContext;
        this.container.addChild(this._verticalScroll);
        this.dataContainer.addChild(this._hoveringGraphics);
        this.initializeCentralLine();
    }

    scroll(viewport, yDelta) {
        this.hoverItem(null);
        super.scroll(viewport, yDelta);
    }

    translateContainer(viewport, cache) {
        super.translateContainer(viewport, cache);
        this.hoverItem(null);
    }

    hoverItem(hoveredItem, viewport) {
        const changed = this.hoveredItem !== hoveredItem;
        if (changed && this._hoveringGraphics) {
            this._hoveringGraphics.clear();
            if (hoveredItem) {
                this.renderHoveredAlignment(
                    viewport,
                    hoveredItem.alignment,
                    hoveredItem.level,
                );
            }
        }
        this.hoveredItem = hoveredItem;
        return changed;
    }

    checkPosition(viewport, cache, position) {
        if (
            !!this.dataContainer &&
            this.renderingInfo &&
            this.renderingInfo.length > 0
        ) {
            const {x, y} = position;
            const [item] = this.renderingInfo.filter(info => info.start <= x &&
                info.end >= x &&
                info.y1 <= y &&
                info.y2 >= y
            );
            if (item) {
                return item;
            }
        }
        return null;
    }

    rebuildContainer(viewport, cache) {
        super.rebuildContainer(viewport, cache);
        this.dataContainer.removeChildren();
        this._hoveringGraphics.clear();
        this._hoveringGraphics = new PIXI.Graphics();
        if (
            cache.data !== null &&
            cache.data !== undefined &&
            cache.data.length > 0
        ) {
            const alignments = cache.data;
            const featureCoords = this.targetContext.featureCoords;
            const levels = [];
            let maxLevels = 0;
            const height = this.config.sequence.height;
            const levelHeight = height + 2 * this.config.sequence.margin;
            for (let i = 0; i < alignments.length; i++) {
                const alignment = alignments[i];
                const { queryStart, queryEnd } = alignment;
                const { start, end } = featureCoords;
                let startPx = viewport.project.brushBP2pixel(start);
                let endPx = viewport.project.brushBP2pixel(end);
                if (queryStart > 0) {
                    startPx -= (
                        this.config.sequence.notAligned.width +
                        this.config.sequence.notAligned.margin +
                        PixiTextSize.getTextSize(
                            `${queryStart}`,
                            this.config.sequence.notAligned.label
                        ).width
                    );
                }
                if (queryEnd > 0) {
                    endPx += (
                        this.config.sequence.notAligned.width +
                        this.config.sequence.notAligned.margin +
                        PixiTextSize.getTextSize(
                            `${queryEnd}`,
                            this.config.sequence.notAligned.label
                        ).width
                    );
                }
                endPx += this.config.strandMarker.width;
                startPx -= this.config.sequence.margin;
                endPx += this.config.sequence.margin;
                const size = endPx - startPx;
                const intersections = levels.filter(level => {
                    const {
                        start: levelStart,
                        end: levelEnd
                    } = level;
                    const levelSize = levelEnd - levelStart;
                    const s = Math.min(levelStart, startPx);
                    const e = Math.max(levelEnd, endPx);
                    return (e - s) < levelSize + size;
                });
                const minimumLevel = Math.min(
                    ...intersections.map(intersection => intersection.level),
                    -1
                );
                const maximumLevel = Math.max(
                    ...intersections.map(intersection => intersection.level),
                    -1
                );
                let level = maximumLevel + 1;
                if (minimumLevel > 0) {
                    level = minimumLevel - 1;
                }
                levels.push({
                    start: startPx,
                    end: endPx,
                    level,
                    y1: level * levelHeight,
                    y2: (level + 1) * levelHeight,
                    alignment
                });
                maxLevels = Math.max(maxLevels, level);
                this.renderAlignment(viewport, alignment, level);
            }
            this.renderingInfo = levels.slice();
            this._actualHeight = levelHeight * (maxLevels + 1);
        } else {
            this.renderingInfo = [];
            this._actualHeight = this._height;
        }
        this.dataContainer.addChild(this._hoveringGraphics);
        this.scroll(viewport, null);
    }

    renderStrandMarker (graphics, x, direction, options) {
        const {
            y,
            height,
            minimum,
            maximum,
            baseColor,
            highlighted
        } = options;
        if (x < minimum || x > maximum) {
            return;
        }
        x = Math.min(maximum, Math.max(minimum, x));
        graphics
            .lineStyle(0, baseColor, 0)
            .beginFill(highlighted ? ColorProcessor.darkenColor(baseColor, 0.1) : baseColor, 1)
            .moveTo(
                x,
                y
            )
            .lineTo(
                x + direction * this.config.strandMarker.width,
                y + Math.round(height / 2.0),
            )
            .lineTo(
                x,
                y + height
            )
            .endFill();
    }

    renderMatch (graphics, x1, x2, options) {
        const {
            y,
            height,
            minimum,
            maximum,
            baseColor,
            highlighted = false
        } = options;
        if (x2 < minimum || x1 > maximum) {
            return;
        }
        x1 = Math.min(maximum, Math.max(minimum, x1));
        x2 = Math.min(maximum, Math.max(minimum, x2));
        graphics
            .lineStyle(0, baseColor, 0)
            .beginFill(highlighted ? ColorProcessor.darkenColor(baseColor, 0.1) : baseColor, 1)
            .drawRect(
                x1,
                y,
                x2 - x1,
                height
            )
            .endFill();
    }

    getAlignmentRenderingInfo (viewport, alignment, level) {
        if (!alignment) {
            return undefined;
        }
        const {
            diff: btop,
            queryLength
        } = alignment;
        const featureCoords = this.targetContext.featureCoords;
        let {
            targetStart,
            targetEnd,
            queryStart,
            queryEnd,
        } = alignment;
        if (featureCoords) {
            targetStart = featureCoords.start;
            targetEnd = featureCoords.end;
        }
        if (!targetStart || !targetEnd) {
            return undefined;
        }
        const height = this.config.sequence.height;
        const levelHeight = height + 2 * this.config.sequence.margin;
        const y = level * levelHeight + this.config.sequence.margin;
        const start = Math.min(targetStart, targetEnd);
        const end = Math.max(targetStart, targetEnd);
        const startPx = viewport.project.brushBP2pixel(start);
        const width = viewport.project.brushBP2pixel(end) - startPx;
        const bpWidth = viewport.factor;
        let baseColor = this.config.colors && this.config.colors.base
            ? this.config.colors.base
            : this.config.sequence.color;
        const renderMarkers = this.config.sequence.markersThresholdWidth < width;
        if (!renderMarkers && this.config.sequence.collapsedColor) {
            baseColor = this.config.sequence.collapsedColor;
        }
        const minimum = -viewport.canvasSize;
        const maximum = 2 * viewport.canvasSize;
        return {
            y,
            height,
            levelHeight,
            minimum,
            maximum,
            bpWidth,
            baseColor,
            start,
            startPx,
            width: Math.max(width, 1),
            queryStart,
            queryEnd,
            btop,
            queryLength,
            renderMarkers
        };
    }

    renderAlignment (viewport, alignment, level) {
        const options = this.getAlignmentRenderingInfo(viewport, alignment, level);
        if (!options) {
            return false;
        }
        const {
            bpWidth,
            startPx,
            width,
        } = options;
        const graphics = new PIXI.Graphics();
        this.dataContainer.addChild(graphics);
        this.renderStrandMarker(
            graphics,
            (startPx + width + bpWidth / 2.0),
            1,
            options
        );
        this.renderMatch(
            graphics,
            startPx - bpWidth / 2.0,
            startPx + width + bpWidth / 2.0,
            options
        );
        return true;
    }

    renderHoveredAlignment (viewport, alignment, level) {
        const options = this.getAlignmentRenderingInfo(viewport, alignment, level);
        if (!options || !this._hoveringGraphics) {
            return false;
        }
        const {
            bpWidth,
            startPx,
            width
        } = options;
        options.highlighted = true;
        this._hoveringGraphics.clear();
        this.renderStrandMarker(
            this._hoveringGraphics,
            (startPx + width + bpWidth / 2.0),
            1,
            options
        );
        this.renderMatch(
            this._hoveringGraphics,
            startPx - bpWidth / 2.0,
            startPx + width + bpWidth / 2.0,
            options
        );
        return true;
    }

    render (viewport, cache, isRedraw, showCenterLine) {
        if (!isRedraw) {
            this.scroll(viewport, 0, cache);
        }
        super.render(viewport, cache, isRedraw, showCenterLine);
    }
}

export default ComparisonAlignmentRenderer;
