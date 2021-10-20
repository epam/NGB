import * as PIXI from 'pixi.js-legacy';
import {ColumnsDendrogramRenderer, RowsDendrogramRenderer} from './axis-dendrogram-renderers';
import InteractiveZone from '../../interactions/interactive-zone';
import events from '../../utilities/events';
import makeInitializable from '../../utilities/make-initializable';

class DendrogramRenderer extends InteractiveZone {
    /**
     *
     * @param {HeatmapInteractions} interactions
     * @param {HeatmapViewOptions} options
     * @param {HeatmapViewport} viewport
     */
    constructor(interactions, options, viewport) {
        super({
            priority: InteractiveZone.Priorities.dendrogram
        });
        makeInitializable(this);
        this.interactions = interactions;
        this.options = options;
        this.viewport = viewport;
        this.container = new PIXI.Container();
        this.columnsDendrogram = new ColumnsDendrogramRenderer(interactions, options, viewport);
        this.rowsDendrogram = new RowsDendrogramRenderer(interactions, options, viewport);
        const onInitialized = () => {
            this.initialized = this.columnsDendrogram.initialized && this.rowsDendrogram.initialized;
        };
        const onLayout = () => this.emit(events.layout);
        this.columnsDendrogram.onInitialized(onInitialized);
        this.rowsDendrogram.onInitialized(onInitialized);
        this.columnsDendrogram.onLayout(onLayout);
        this.rowsDendrogram.onLayout(onLayout);
        this.container.addChild(this.columnsDendrogram.container);
        this.container.addChild(this.rowsDendrogram.container);
        onInitialized();
    }

    destroy() {
        super.destroy();
        this.interactions = undefined;
        this.options = undefined;
        this.viewport = undefined;
        if (this.columnsDendrogram) {
            this.columnsDendrogram.destroy();
        }
        this.columnsDendrogram = undefined;
        if (this.rowsDendrogram) {
            this.rowsDendrogram.destroy();
        }
        this.rowsDendrogram = undefined;
        if (this.container) {
            this.container.removeChildren();
        }
        this.container = undefined;
    }

    /**
     * Columns dendrogram
     * @returns {ColumnsDendrogramRenderer}
     */
    get columns() {
        return this.columnsDendrogram;
    }

    /**
     * Rows dendrogram
     * @returns {RowsDendrogramRenderer}
     */
    get rows() {
        return this.rowsDendrogram;
    }

    initialize() {
        this.columnsDendrogram.initialize();
        this.rowsDendrogram.initialize();
    }

    onLayout(callback) {
        this.addEventListener(events.layout, callback);
    }

    updateWindowSize(width, height) {
        if (this.columnsDendrogram) {
            this.columnsDendrogram.windowSize = height;
        }
        if (this.rowsDendrogram) {
            this.rowsDendrogram.windowSize = width;
        }
    }

    render() {
        let somethingChanged = false;
        if (this.columnsDendrogram) {
            somethingChanged = this.columnsDendrogram.render() || somethingChanged;
        }
        if (this.rowsDendrogram) {
            somethingChanged = this.rowsDendrogram.render() || somethingChanged;
        }
        return somethingChanged;
    }
}

export default DendrogramRenderer;
