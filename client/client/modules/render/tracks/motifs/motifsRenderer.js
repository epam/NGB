import * as PIXI from 'pixi.js-legacy';
import {CachedTrackRenderer} from '../../core';
import drawStrandDirection from '../gene/internal/renderer/features/drawing/strandDrawing';

const WHITE = 0xFFFFFF;

export default class MotifsMatchesRenderer extends CachedTrackRenderer {
    constructor(track, config, options, strand) {
        super(track);
        this.track = track;
        this._config = config;
        this._height = config.height;
        this.options = options;
        this.strand = strand;
        this.graphics = new PIXI.Graphics();
        this.dataContainer.addChild(this.graphics);
        this.initializeCentralLine();
    }

    get config() {
        return this._config;
    }

    get height() {
        return this._height;
    }

    set height(value) {
        this._height = value;
    }

    getMatchColor(strand, state) {
        if (state && state.color && state.color[strand]) {
            return state.color[strand];
        }
        return this.config.matches.defaultColor[strand];
    }

    rebuildContainer(viewport, cache) {
        super.rebuildContainer(viewport, cache);
        this.initializeMatches(viewport, cache);
    }

    initializeMatches(viewport, cache) {
        this.graphics.clear();
        if (cache.data && cache.data.length > 0) {
            cache.data.forEach(match => {
                this.initializeMatch(viewport, match);
            });
        }
    }

    initializeMatch (viewport, match) {
        if (this.strand === match.strand) {
            const pixelsInBp = viewport.factor;
            const {startIndex, endIndex, levelY} = match;
            const correct = x => Math.max(
                -viewport.canvasSize,
                Math.min(
                    x,
                    2 * viewport.canvasSize
                )
            );
            const startX = correct(viewport.project.brushBP2pixel(startIndex) - (pixelsInBp / 2));
            const endX = correct(viewport.project.brushBP2pixel(endIndex) + (pixelsInBp / 2));
            const height = this.config.matches.height;
            const color = this.getMatchColor(this.strand, this.track ? this.track.state : undefined);
            const centerY = levelY * (this.config.matches.margin + height) - height / 2.0;
            const width = endX - startX;
            if (width <= this.config.matches.detailsThresholdPx) {
                const arrowWidth = 2.0 * height;
                drawStrandDirection(
                    this.strand,
                    {
                        x: startX + width / 2.0 - arrowWidth / 2.0,
                        width: arrowWidth,
                        height,
                        centerY
                    },
                    this.graphics,
                    color,
                    {
                        ...this.config.matches.strand.arrow,
                        mode: 'fill',
                        margin: 0,
                        height
                    }
                );
            } else {
                this.graphics
                    .beginFill(color, 1)
                    .drawRect(
                        startX,
                        Math.round(centerY - height / 2.0),
                        width,
                        height
                    )
                    .endFill();
                drawStrandDirection(
                    this.strand,
                    {
                        x: startX,
                        width,
                        height,
                        centerY
                    },
                    this.graphics,
                    WHITE,
                    this.config.matches.strand.arrow
                );
            }
        }
    }

    render (viewport, cache, isRedraw, showCenterLine) {
        super.render(viewport, cache, isRedraw, showCenterLine);
    }
}
