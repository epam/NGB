import * as PIXI from 'pixi.js-legacy';
import {AlignmentsRenderer, BP_OFFSET} from './alignmentsRenderer';
import {Line} from '../../cache/line';

const Math = window.Math;

export {BP_OFFSET, CAN_SHOW_DETAILS_FACTOR} from './alignmentsRenderer';
export class AlignmentsRenderProcessor {

    _container = null;
    _renderPositionInfo = null;

    _alignmentHeight;

    set alignmentHeight(value) {
        this._alignmentHeight = value;
    }

    get renderedPosition() {
        return this._renderPositionInfo ? this._renderPositionInfo.y : null;
    }

    get container() {
        return this._container;
    }

    constructor(bisulfiteModeContext) {
        this._alignmentHeight = 1;
        this._container = new PIXI.Container();
        this.bisulfiteModeContext = bisulfiteModeContext;
    }

    clear() {
        this._container.removeChildren();
    }

    render(cache, viewport, flags, drawingConfig) {
        const {colors, config, currentY, features, height, labelsManager, renderer, topMargin} = drawingConfig;
        const visibleLinesCount = (height - topMargin) / this._alignmentHeight;
        const linesCount = cache.linesCount;
        const y = Math.max(Math.min(currentY, linesCount - visibleLinesCount), 0);

        const additionalDrawingSize = visibleLinesCount / 2;

        const startLine = Math.min(Math.ceil(Math.floor(y) - additionalDrawingSize), linesCount);
        const endLine = Math.min(Math.ceil(Math.floor(y) + visibleLinesCount + additionalDrawingSize), linesCount);

        const visibleStartLine = Math.floor(y);
        const visibleEndLine = Math.min(Math.ceil(Math.floor(y) + visibleLinesCount), linesCount);

        const shouldReRender = !this._renderPositionInfo ||
            this._renderPositionInfo.readsRenderRange.horizontal.start > viewport.brush.start ||
            this._renderPositionInfo.readsRenderRange.horizontal.end < viewport.brush.end ||
            this._renderPositionInfo.readsRenderRange.vertical.startLine > visibleStartLine ||
            this._renderPositionInfo.readsRenderRange.vertical.endLine < visibleEndLine;

        let readsRenderRange = null;
        if (!shouldReRender && this._renderPositionInfo &&
            this._renderPositionInfo.factor === viewport.factor &&
            !flags.dataChanged &&
            !flags.heightChanged &&
            !flags.widthChanged &&
            !flags.renderFeaturesChanged &&
            !flags.textureCacheUpdated) {
            this.moveLineSlice(viewport, drawingConfig);
        }
        else {
            this._container.x = 0;
            this._container.y = 0;
            readsRenderRange = this.renderLineSlice({
                cache,
                colors,
                config,
                currentY,
                endLine,
                features,
                height,
                labelsManager,
                renderer,
                startLine,
                topMargin,
                viewport
            });
        }
        if (readsRenderRange) {
            this._renderPositionInfo = {
                factor: viewport.factor,
                readsRenderRange,
                x: viewport.brush.start,
                y: currentY
            };
        }
    }

    moveLineSlice(viewport, drawingConfig) {
        const {currentY} = drawingConfig;
        const dX = viewport.convert.brushBP2pixel(this._renderPositionInfo.x - viewport.brush.start);
        const dY = (this._renderPositionInfo.y - currentY) * this._alignmentHeight;
        this._container.x = dX;
        this._container.y = dY;
    }

    renderLineSlice(opts) {
        const {
            cache,
            colors,
            config,
            currentY,
            endLine,
            features,
            height,
            labelsManager,
            renderer,
            startLine,
            topMargin,
            viewport
        } = opts;
        const start = viewport.brush.start - viewport.brushSize / 2;
        const end = viewport.brush.end + viewport.brushSize / 2;
        const alignmentsRenderer = new AlignmentsRenderer(
            viewport,
            config,
            colors,
            this._alignmentHeight,
            topMargin,
            currentY,
            features,
            labelsManager,
            height,
            this.bisulfiteModeContext
        );
        alignmentsRenderer.startRender();
        for (let line = startLine; line < endLine; line++) {
            if (!cache.getLine(line))
                continue;
            const renderers = cache.getLine(line).render(start, end, features);
            alignmentsRenderer.line = line;
            for (let j = 0, len = renderers.length; j < len; j++) {
                alignmentsRenderer.render(renderers[j]);
            }
        }
        const sprite = alignmentsRenderer.finishRender(renderer);
        this._container.removeChildren();
        this._container.addChild(sprite);
        return {
            horizontal: {end, start},
            vertical: {endLine, startLine}
        };
    }

    hoverRead(cache, viewport, drawingConfig, container, read, line) {
        const {colors, config, currentY, features, height, labelsManager, renderer, topMargin} = drawingConfig;
        if (!read) {
            container.removeChildren();
            return;
        }
        const alignmentsRenderer = new AlignmentsRenderer(
            viewport,
            config,
            colors,
            this._alignmentHeight,
            topMargin,
            currentY,
            features,
            labelsManager,
            height,
            this.bisulfiteModeContext
        );
        alignmentsRenderer.startRender(true);
        const renderers = Line.renderSimpleRead(read, features, this.bisulfiteModeContext);
        alignmentsRenderer.line = line;
        for (let j = 0, len = renderers.length; j < len; j++) {
            alignmentsRenderer.render(renderers[j]);
        }
        const sprite = alignmentsRenderer.finishRender(renderer);
        container.removeChildren();
        container.addChild(sprite);
    }

    static checkAlignment({x, line}, cache) {
        const target = cache.getLine(line >> 0);
        const targetElement = !target
            ? null
            : target.itemsSortedByStartIndex.findGreatestLessThanOrEqual({startIndex: x});
        if (!targetElement
            || (targetElement.value.endIndex < x)
            || (targetElement.value.startIndex > x)) {
            return null;
        } else {
            let targetRead = targetElement.value;
            if (targetElement.value.isPairedReads) {
                let checkReads = [];
                if (line - (line >> 0) >= BP_OFFSET) {
                    // we should check right read first
                    checkReads = [targetElement.value.rightPair, targetElement.value.leftPair];
                }
                else {
                    // we should check left read first
                    checkReads = [targetElement.value.leftPair, targetElement.value.rightPair];
                }
                let readFound = false;
                for (let i = 0; i < checkReads.length; i++) {
                    if (checkReads[i].startIndex <= x && checkReads[i].endIndex >= x) {
                        targetRead = checkReads[i];
                        targetRead.lineIndex = line >> 0;
                        readFound = true;
                        break;
                    }
                }
                if (!readFound) {
                    targetRead = null;
                }
            }
            return targetRead;
        }
    }
}
