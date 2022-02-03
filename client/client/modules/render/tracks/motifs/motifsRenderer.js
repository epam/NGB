import * as PIXI from 'pixi.js-legacy';
import {CachedTrackRendererWithVerticalScroll} from '../../core';
import {PlaceholderRenderer} from '../../utilities';
import {MotifsMatchesRenderer} from './motifsMatchesRenderer';

const MAXIMUM_RANGE = 500000;

export class MotifsRenderer extends CachedTrackRendererWithVerticalScroll {

    matchesGraphics = null;
    zoomInContainer = null;

    get maximumRange () {
        return MAXIMUM_RANGE;
    }

    constructor(track, config, options, strand) {
        super(track);
        this.track = track;
        this._config = config;
        this._height = config.height;
        this.options = options;
        this.strand = strand;
        this.container.addChild(this._verticalScroll);

        this.matchesGraphics = new MotifsMatchesRenderer(config, strand, track);
        this.dataContainer.addChild(this.matchesGraphics.graphics);

        this.zoomInContainer = new PIXI.Container();
        this._zoomInPlaceholderRenderer = new PlaceholderRenderer(this);
        this.zoomInContainer.addChild(this._zoomInPlaceholderRenderer.container);
        this.initZoomInPlaceholder();

        this.initializeCentralLine();
    }

    get config() {
        return this._config;
    }

    scroll(viewport, yDelta) {
        super.scroll(viewport, yDelta);
    }

    translateContainer(viewport, cache) {
        super.translateContainer(viewport, cache);
    }

    rebuildContainer(viewport, cache) {
        super.rebuildContainer(viewport, cache);
        this.matchesGraphics.initializeMatches(viewport, cache);
        this._actualHeight = this.matchesGraphics.actualHeight;
        this.scroll(viewport, null);
    }

    initZoomInPlaceholder() {
        const zoomInPlaceholderText = () => {
            const unitThreshold = 1000;
            const noReadText = {
                unit: this.maximumRange < unitThreshold ? 'BP' : 'kBP',
                value: this.maximumRange < unitThreshold
                    ? this.maximumRange : Math.ceil(this.maximumRange / unitThreshold)
            };
            return `Zoom in to see motifs.
Minimal zoom level is at ${noReadText.value}${noReadText.unit}`;
        };
        this._zoomInPlaceholderRenderer.init(zoomInPlaceholderText(), {
            height: this.pixiRenderer.height,
            width: this.pixiRenderer.width
        });
    }

    render(flags, viewport, cache, showCenterLine) {
        if (flags.widthChanged || flags.heightChanged) {
            this.initZoomInPlaceholder();
        }
        const reDraw = flags.heightChanged || flags.dataChanged;
        if (!reDraw) {
            this.scroll(viewport, 0, cache);
        }
        super.render(viewport, cache, reDraw, showCenterLine);
    }
}
