import {CachedTrackRenderer} from '../../core';
import * as PIXI from 'pixi.js-legacy';

export default class MotifsMatchesRenderer extends CachedTrackRenderer {

    constructor(track, config, options, strand) {
        super(track);
        this.track = track;
        this._config = config;
        this._height = config.height;
        this.options = options;
        this.strand = strand;
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
        this.dataContainer.removeChildren();
        this.initializeMatches(viewport, cache);
    }

    initializeMatches(viewport, cache) {
        if (cache.data && cache.data.length > 0) {
            cache.data.forEach(match => {
                this.initializeMatch(viewport, match);
            });
        }
    }

    initializeMatch (viewport, match) {
        const strand = this.strand.toLowerCase();
        if (strand === match.strand.toLowerCase()) {
            const pixelsInBp = viewport.factor;
            const {startIndex, endIndex, levelY} = match;
            const startX = viewport.project.brushBP2pixel(startIndex) + (pixelsInBp / 2);
            const endX = viewport.project.brushBP2pixel(endIndex) + (pixelsInBp / 2);

            if (startX > -viewport.canvasSize && endX < 2 * viewport.canvasSize) {
                const height = this.config.matches.height;
                const color = this.getMatchColor(strand, this.track ? this.track.state : undefined);
                const block = new PIXI.Graphics();
                const y = levelY * (10 + height) - height;
                const width = endX - startX;
                block.beginFill(color, 1);
                block.drawRect(startX, y, width, height);
                block.endFill();

                for (
                    let i = 5;
                    i + pixelsInBp < width;
                    i += 25
                ) {
                    const strandLine = new PIXI.Graphics();
                    strandLine.lineStyle(2, 0xffffff, 1);
                    strandLine.position.x = startX;
                    strandLine.position.y = y;
                    if (strand === 'positive') {
                        strandLine.moveTo(i, -1);
                        strandLine.lineTo(i + pixelsInBp, height/2);
                        strandLine.lineTo(i, height+1);
                    }
                    if (strand === 'negative') {
                        strandLine.moveTo(i + pixelsInBp, -1);
                        strandLine.lineTo(i, height/2);
                        strandLine.lineTo(i + pixelsInBp, height+1);
                    }
                    block.addChild(strandLine);
                }
                this.dataContainer.addChild(block);
            }
        }
    }

    render (viewport, cache, isRedraw, showCenterLine) {
        super.render(viewport, cache, isRedraw, showCenterLine);
    }
}
