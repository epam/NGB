import {CachedTrack} from '../../core/track';
import HeatmapConfig from './config';
import {HeatmapView} from '../../heatmap';
import Menu from '../../core/menu';
import heatmapMenu from './menu';

export default class HeatmapTrack extends CachedTrack {
    static getTrackDefaultConfig () {
        return HeatmapConfig;
    }

    static Menu = Menu(heatmapMenu);

    get stateKeys() {
        return ['header', 'options'];
    }

    constructor(opts) {
        opts.preferWebGL = true;
        super({...opts, interactionDisabled: true});
        const dispatcher = this.config.dispatcher;
        this.heatmapView = new HeatmapView({
            projectContext: this.projectContext,
            width: this.viewport.canvasSize,
            height: this.height,
            padding: HeatmapConfig.padding,
            dataConfig: this.dataConfig,
            dispatcher,
            displayOptions: {
                pixiRenderer: this._pixiRenderer,
                domElement: this.domElement
            },
            state: this.state.options,
            showOptionsButtons: false
        });
        this.heatmapView.referenceId = this.config.referenceId;
        this.container.addChild(this.heatmapView.container);
        /**
         *
         * @type {HeatmapViewOptions}
         */
        this.state.heatmap = this.heatmapView.options;
        this.heatmapView.options.onChange(this.reportTrackState.bind(this, false));
    }

    getTrackStatePayload() {
        if (this.state.heatmap) {
            return {
                options: this.state.heatmap.serialize()
            };
        }
        return {};
    }

    destructor() {
        this.heatmapView.destroy();
        this.heatmapView = undefined;
        super.destructor();
    }

    getSettings() {
        if (!this._menu) {
            this._menu = this.constructor.Menu.attach(this, {browserId: this.browserId});
        }
        return this._menu;
    }

    render(flags) {
        let somethingChanged = false;
        if (flags.renderReset) {
            this.container.removeChildren();
            this.heatmapView.updateDisplayOptions({
                pixiRenderer: this._pixiRenderer,
                domElement: this.domElement
            });
            this.container.addChild(this.heatmapView.container);
            somethingChanged = true;
        }
        this.heatmapView.resize(this.viewport.canvasSize, this.height);
        return this.heatmapView.renderHeatmap() || somethingChanged;
    }
}
