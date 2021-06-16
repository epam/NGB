import {
    BTOPPartType,
    parseBtop
} from '../../../../app/shared/blastContext';
import {CachedTrackRenderer} from '../../core';
import {ColorProcessor, PixiTextSize} from '../../utilities';
import PIXI from 'pixi.js';
import {drawingConfiguration} from '../../core/configuration';

class BLASTAlignmentRenderer extends CachedTrackRenderer {
    get config() {
        return this._config;
    }

    get verticalScroll(): PIXI.Graphics {
        return this._verticalScroll;
    }

    get height() {
        return this._height;
    }

    set height(value) {
        this._height = value;
    }

    get actualHeight() {
        return this._actualHeight;
    }

    constructor(config, pixiRenderer, options, blastContext) {
        super();
        this._config = config;
        this.pixiRenderer = pixiRenderer;
        this.options = options;
        this._verticalScroll = new PIXI.Graphics();
        this._hoveringGraphics = new PIXI.Graphics();
        this.blastContext = blastContext;
        this.container.addChild(this._verticalScroll);
        this.dataContainer.addChild(this._hoveringGraphics);
        this.initializeCentralLine();
    }

    scrollIndicatorBoundaries(viewport) {
        if (this.actualHeight && this.height < this.actualHeight) {
            return {
                height: this.height * this.height / this.actualHeight,
                width: this.config.scroll.width,
                x: viewport.canvasSize - this.config.scroll.width - this.config.scroll.margin,
                y: -this.dataContainer.y / this.actualHeight * this.height
            };
        }
        return null;
    }

    drawVerticalScroll(viewport) {
        this.verticalScroll.clear();
        if (this.actualHeight && this.height < this.actualHeight) {
            const scrollHeight = this.height * this.height / this.actualHeight;
            this.verticalScroll
                .beginFill(this.config.scroll.fill, this._verticalScrollIsHovered ? this.config.scroll.hoveredAlpha : this.config.scroll.alpha)
                .drawRect(
                    viewport.canvasSize - this.config.scroll.width - this.config.scroll.margin,
                    -this.dataContainer.y / this.actualHeight * this.height,
                    this.config.scroll.width,
                    scrollHeight
                )
                .endFill();
        }
    }

    _verticalScrollIsHovered = false;

    hoverVerticalScroll(viewport) {
        if (!this._verticalScrollIsHovered) {
            this._verticalScrollIsHovered = true;
            this.drawVerticalScroll(viewport);
            return true;
        }
        return false;
    }

    unhoverVerticalScroll(viewport) {
        if (this._verticalScrollIsHovered) {
            this._verticalScrollIsHovered = false;
            this.drawVerticalScroll(viewport);
            return true;
        }
        return false;
    }

    isScrollable() {
        return this.actualHeight && this.height < this.actualHeight;
    }

    canScroll(yDelta) {
        if (this.actualHeight && this.height < this.actualHeight) {
            let __y = this.dataContainer.y;
            if (yDelta !== null) {
                __y += yDelta;
            }
            return __y <= 0 && __y >= this.height - this.actualHeight;
        }
        return false;
    }

    setScrollPosition(viewport, indicatorPosition) {
        this.scroll(viewport, - indicatorPosition * this.actualHeight / this.height - this.dataContainer.y);
    }

    scroll(viewport, yDelta) {
        this.hoverItem(null);
        if (this.actualHeight && this.height < this.actualHeight) {
            let __y = this.dataContainer.y;
            if (yDelta !== null) {
                __y += yDelta;
            }
            __y = Math.min(0, Math.max(this.height - this.actualHeight, __y));
            this.dataContainer.y = __y;
            this.drawVerticalScroll(viewport);
        } else {
            this.dataContainer.y = 0;
            this.drawVerticalScroll(null);
        }
    }

    translateContainer(viewport, cache) {
        super.translateContainer(viewport, cache);
        this.hoverItem(null);
    }

    hoverItem(hoveredItem, viewport, cache) {
        const changed = this.hoveredItem !== hoveredItem;
        if (changed && this._hoveringGraphics) {
            this._hoveringGraphics.clear();
            if (hoveredItem) {
                this.renderHoveredAlignment(
                    viewport,
                    hoveredItem.alignment,
                    hoveredItem.level,
                    cache.isProtein
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
            const levels = [];
            let maxLevels = 0;
            const height = this.config.sequence.height;
            const levelHeight = height + 2 * this.config.sequence.margin;
            for (let i = 0; i < alignments.length; i++) {
                const alignment = alignments[i];
                const {
                    sequenceStart,
                    sequenceEnd,
                    queryLength,
                    sequenceStrandView
                } = alignment;
                let {
                    queryStart,
                    queryEnd,
                } = alignment;
                if (!sequenceStart || !sequenceEnd) {
                    return false;
                }
                const positiveStrand = sequenceStrandView === '+';
                if (!positiveStrand) {
                    const temp = queryStart;
                    queryStart = queryLength - queryEnd + 1;
                    queryEnd = queryLength - temp + 1;
                }
                queryStart = queryStart - 1;
                queryEnd = queryLength - queryEnd;
                const start = Math.min(sequenceStart, sequenceEnd);
                const end = Math.max(sequenceStart, sequenceEnd);
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
                if (positiveStrand) {
                    endPx += this.config.strandMarker.width;
                } else {
                    startPx -= this.config.strandMarker.width;
                }
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
                this.renderAlignment(viewport, alignment, level, cache.isProtein);
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

    renderMisMatch (graphics, x1, x2, alt, options) {
        const {
            y,
            height,
            minimum,
            maximum,
            baseColor,
            renderLabel = true
        } = options;
        if (x2 < minimum || x1 > maximum) {
            return;
        }
        x1 = Math.min(maximum, Math.max(minimum, x1));
        x2 = Math.min(maximum, Math.max(minimum, x2));
        const colorCode = (alt || '').toUpperCase();
        const color = this.config.colors && this.config.colors.hasOwnProperty(colorCode)
            ? this.config.colors[colorCode]
            : ColorProcessor.darkenColor(baseColor);
        graphics
            .lineStyle(0, 0x0, 0)
            .beginFill(color, 1)
            .drawRect(
                x1,
                y,
                x2 - x1,
                height
            )
            .endFill();
        if (alt && renderLabel) {
            const label = new PIXI.Text(
                alt,
                this.config.sequence.mismatch.label,
                drawingConfiguration.resolution
            );
            if (label.width < x2 - x1) {
                label.x = Math.round((x1 + x2) / 2.0 - label.width / 2.0);
                label.y = Math.round(
                    y + height / 2.0 - label.height / 2.0
                );
                this.dataContainer.addChild(label);
            }
        }
    }

    renderInsertion (graphics, x1, alt, options) {
        const {
            y,
            height,
            minimum,
            maximum,
            bpWidth,
            baseColor,
            highlighted = false
        } = options;
        if (x1 < minimum || x1 > maximum) {
            return;
        }
        x1 = Math.min(maximum, Math.max(minimum, x1)) - 0.5;
        const colorCode = (alt || '').toUpperCase();
        const color = this.config.colors && this.config.colors.hasOwnProperty(colorCode)
            ? this.config.colors[colorCode]
            : ColorProcessor.darkenColor(baseColor);
        graphics
            .lineStyle(1, highlighted ? ColorProcessor.darkenColor(color) : color, 1)
            .moveTo(
                x1,
                y - 0.5
            )
            .lineTo(
                x1,
                y + height + 0.5
            )
            .moveTo(
                x1 - bpWidth / 3.0,
                y - 0.5
            )
            .lineTo(
                x1 + bpWidth / 3.0,
                y - 0.5
            )
            .moveTo(
                x1 - bpWidth / 3.0,
                y + height + 0.5
            )
            .lineTo(
                x1 + bpWidth / 3.0,
                y + height + 0.5
            );
    }

    renderDeletion (graphics, x1, x2, options) {
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
        x1 = Math.min(maximum, Math.max(minimum, x1)) + 0.5;
        x2 = Math.min(maximum, Math.max(minimum, x2)) - 0.5;
        const color = this.config.colors && this.config.colors.del
            ? this.config.colors.del
            : ColorProcessor.darkenColor(baseColor);
        graphics
            .lineStyle(1, highlighted ? ColorProcessor.darkenColor(color) : color, 1)
            .moveTo(
                x1,
                y
            )
            .lineTo(
                x1,
                y + height
            )
            .moveTo(
                x1,
                Math.floor(y + height / 2.0) - 0.5
            )
            .lineTo(
                x2,
                Math.floor(y + height / 2.0) - 0.5
            )
            .moveTo(
                x2,
                y
            )
            .lineTo(
                x2,
                y + height
            );
    }

    renderNotAlignedMarker (graphics, size, x, direction, options) {
        if (size > 0) {
            const {
                y,
                height,
                minimum,
                maximum,
                renderLabel = true
            } = options;
            if (x < minimum || x > maximum) {
                return;
            }
            x = Math.min(maximum, Math.max(minimum, x));
            const notAlignedMarkerWidth = this.config.sequence.notAligned.width;
            const margin = this.config.sequence.notAligned.margin;
            graphics
                .lineStyle(1, this.config.sequence.notAligned.color, 1)
                .drawRect(
                    direction < 0 ? (x - notAlignedMarkerWidth) : x,
                    y,
                    notAlignedMarkerWidth,
                    height
                );
            if (renderLabel) {
                const label = new PIXI.Text(
                    `${size}`,
                    this.config.sequence.notAligned.label,
                    drawingConfiguration.resolution
                );
                label.x = Math.round(
                    direction < 0
                        ? (x - notAlignedMarkerWidth - label.width - margin)
                        : (x + notAlignedMarkerWidth + margin)
                );
                label.y = Math.round(
                    y + height / 2.0 - label.height / 2.0
                );
                this.dataContainer.addChild(label);
            }
        }
    }

    getAlignmentRenderingInfo (viewport, alignment, level) {
        if (!alignment) {
            return undefined;
        }
        const {
            btop,
            sequenceStart,
            sequenceEnd,
            queryLength,
            sequenceStrandView
        } = alignment;
        let {
            queryStart,
            queryEnd,
        } = alignment;
        if (!sequenceStart || !sequenceEnd) {
            return undefined;
        }
        const positiveStrand = sequenceStrandView === '+';
        if (!positiveStrand) {
            const temp = queryStart;
            queryStart = queryLength - queryEnd + 1;
            queryEnd = queryLength - temp + 1;
        }
        const height = this.config.sequence.height;
        const levelHeight = height + 2 * this.config.sequence.margin;
        const y = level * levelHeight + this.config.sequence.margin;
        const start = Math.min(sequenceStart, sequenceEnd);
        const end = Math.max(sequenceStart, sequenceEnd);
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
            sequenceStart,
            sequenceEnd,
            queryLength,
            sequenceStrandView,
            positiveStrand,
            renderMarkers
        };
    }

    renderAlignment (viewport, alignment, level, isProtein) {
        const options = this.getAlignmentRenderingInfo(viewport, alignment, level);
        if (!options) {
            return false;
        }
        const {
            bpWidth,
            start,
            startPx,
            width,
            queryStart,
            queryEnd,
            btop,
            queryLength,
            positiveStrand,
            renderMarkers
        } = options;
        const graphics = new PIXI.Graphics();
        this.dataContainer.addChild(graphics);
        if (renderMarkers) {
            this.renderStrandMarker(
                graphics,
                positiveStrand
                    ? (startPx + width + bpWidth / 2.0)
                    : (startPx - bpWidth / 2.0),
                positiveStrand ? 1 : -1,
                options
            );
        }
        if (bpWidth < this.config.sequence.detailsThreshold || isProtein) {
            this.renderMatch(graphics, startPx, startPx + width, options);
        } else {
            const btopParsed = parseBtop(btop);
            let relativePosition = start;
            for (let p = 0; p < btopParsed.length; p++) {
                const part = positiveStrand ? btopParsed[p] : btopParsed[btopParsed.length - 1 - p];
                const x = viewport.project.brushBP2pixel(
                    relativePosition
                ) - bpWidth / 2.0;
                const x2 = viewport.project.brushBP2pixel(
                    relativePosition + (part.length || 0)
                ) - bpWidth / 2.0;
                switch (part.type) {
                    case BTOPPartType.match:
                        this.renderMatch(graphics, x, x2, options);
                        break;
                    case BTOPPartType.mismatch:
                        this.renderMisMatch(graphics, x, x2, part.alt, options);
                        break;
                    case BTOPPartType.insertion:
                        this.renderInsertion(graphics, x, part.alt, options);
                        break;
                    case BTOPPartType.deletion:
                        this.renderDeletion(graphics, x, x2, options);
                        break;
                }
                relativePosition += (part.length || 0);
            }
        }
        if (renderMarkers) {
            this.renderNotAlignedMarker(
                graphics,
                queryStart - 1,
                startPx - bpWidth / 2.0,
                -1,
                options
            );
            this.renderNotAlignedMarker(
                graphics,
                queryLength - queryEnd,
                startPx + width + bpWidth / 2.0,
                1,
                options
            );
        }
        return true;
    }

    renderHoveredAlignment (viewport, alignment, level, isProtein) {
        const options = this.getAlignmentRenderingInfo(viewport, alignment, level);
        if (!options || !this._hoveringGraphics) {
            return false;
        }
        const {
            bpWidth,
            start,
            startPx,
            width,
            btop,
            positiveStrand,
            renderMarkers
        } = options;
        options.highlighted = true;
        this._hoveringGraphics.clear();
        if (renderMarkers) {
            this.renderStrandMarker(
                this._hoveringGraphics,
                positiveStrand
                    ? (startPx + width + bpWidth / 2.0)
                    : (startPx - bpWidth / 2.0),
                positiveStrand ? 1 : -1,
                options
            );
        }
        if (bpWidth < this.config.sequence.detailsThreshold || isProtein) {
            this.renderMatch(this._hoveringGraphics, startPx, startPx + width, options);
        } else {
            const btopParsed = parseBtop(btop);
            let relativePosition = start;
            for (let p = 0; p < btopParsed.length; p++) {
                const part = positiveStrand ? btopParsed[p] : btopParsed[btopParsed.length - 1 - p];
                const x = viewport.project.brushBP2pixel(
                    relativePosition
                ) - bpWidth / 2.0;
                const x2 = viewport.project.brushBP2pixel(
                    relativePosition + (part.length || 0)
                ) - bpWidth / 2.0;
                switch (part.type) {
                    case BTOPPartType.match:
                        this.renderMatch(this._hoveringGraphics, x, x2, options);
                        break;
                    case BTOPPartType.mismatch:
                        this.renderMisMatch(this._hoveringGraphics, x, x2, part.alt, options);
                        break;
                    case BTOPPartType.insertion:
                        this.renderInsertion(this._hoveringGraphics, x, part.alt, options);
                        break;
                    case BTOPPartType.deletion:
                        this.renderDeletion(this._hoveringGraphics, x, x2, options);
                        break;
                }
                relativePosition += (part.length || 0);
            }
        }
        return true;
    }

    render (viewport, cache, isRedraw, showCenterLine) {
        if (!isRedraw) {
            this.scroll(viewport, 0, cache);
        }
        super.render(viewport, cache, isRedraw, null, showCenterLine);
    }
}

export default BLASTAlignmentRenderer;
