import * as PIXI from 'pixi.js-legacy';
import drawStrandDirection from '../gene/internal/renderer/features/drawing/strandDrawing';

const WHITE = 0xFFFFFF;

export class MotifsMatchesRenderer {

    _graphics = null;
    _actualHeight = 0;
    _matchesCount = 0;

    constructor(config, strand, track) {
        this.config = config;
        this.strand = strand;
        this.track = track;
        this._graphics = new PIXI.Graphics();
    }

    get graphics() {
        return this._graphics;
    }

    get actualHeight() {
        return this._actualHeight;
    }

    initializeMatches(viewport, cache) {
        this._graphics.clear();
        if (cache.data && cache.data.length > 0) {
            this._matchesCount = 0;
            this._actualHeight = this.config.minHeight;
            cache.data.forEach(match => {
                this.initializeMatch(viewport, match);
            });
        } else {
            this._matchesCount = 0;
            this._actualHeight = this.config.height;
        }
    }

    initializeMatch (viewport, match) {
        if (this.strand === match.strand) {
            const color = this.getMatchColor(this.strand, this.track ? this.track.state : undefined);
            const pixelsInBp = viewport.factor;
            const {startIndex, endIndex, levelY} = match;
            const correct = x => {
                const size = viewport.canvasSize;
                return ( Math.max( -size, Math.min( x, 2 * size ) ) );
            };
            const startX = correct(viewport.project.brushBP2pixel(startIndex) - (pixelsInBp / 2));
            const endX = correct(viewport.project.brushBP2pixel(endIndex) + (pixelsInBp / 2));
            const {height, margin} = this.config.matches;
            
            const matchHeight = height + margin;
            const centerY = levelY * matchHeight - height / 2.0;
            const actualHeight = centerY + (margin * 2);
            const width = endX - startX;

            this._actualHeight = Math.max(actualHeight, this._actualHeight);
            this._matchesCount = Math.max(levelY, this._matchesCount);
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
                    this._graphics,
                    color,
                    {
                        ...this.config.matches.strand.arrow,
                        mode: 'fill',
                        margin: 0,
                        height
                    }
                );
            } else {
                this._graphics
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
                    this._graphics,
                    WHITE,
                    this.config.matches.strand.arrow
                );
            }
        }
    }

    getMatchColor(strand, state) {
        if (state && state.color && state.color[strand]) {
            return state.color[strand];
        }
        return this.config.matches.defaultColor[strand];
    }
}
