import * as PIXI from 'pixi.js-legacy';

export default class VariantBaseRenderer {

    _container: PIXI.Container;
    _viewports: Map;
    _heightChanged: Function = null;
    _showTooltip: Function = null;
    _affectedGeneTranscriptChanged: Function = null;
    _height = 0;
    _dataChanged = false;
    _presentationChanged = false;

    _updateSceneFn: Function = null;
    _reRenderSceneFn: Function = null;
    _options = {};

    constructor(variant, heightChanged, showTooltip, affectedGeneTranscriptChanged, updateSceneFn, reRenderScene) {
        this._viewports = new Map();
        this._variant = variant;
        this._container = new PIXI.Container();
        this._heightChanged = heightChanged;
        this._showTooltip = showTooltip;
        this._affectedGeneTranscriptChanged = affectedGeneTranscriptChanged;
        this._updateSceneFn = updateSceneFn;
        this._reRenderSceneFn = reRenderScene;
    }

    get variant() {
        return this._variant;
    }

    get container(): PIXI.Container {
        return this._container;
    }

    get viewports(): Map {
        return this._viewports;
    }

    get height() {
        return 0;
    }

    displayTooltip(position, tooltipContent) {
        if (this._showTooltip) {
            this._showTooltip(position, tooltipContent);
        }
    }

    manageViewports() {
        this.viewports.clear();
    }

    renderObjects(flags, config) {
        if (this.variant !== null && this.variant !== undefined && this.variant.variantInfo !== null && this.variant.variantInfo !== undefined) {
            if (this._dataChanged  || (flags && (flags.widthChanged || flags.reset)))
                this.manageViewports(config);
            this.container.removeChildren();
            return true;
        }
        return false;
    }

    render(flags, config) {
        if (flags.widthChanged || this._dataChanged || this._presentationChanged) {
            this.renderObjects(flags, config);
            this._presentationChanged = false;
            this._dataChanged = false;
        }
    }
}
