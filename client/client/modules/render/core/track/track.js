import BaseTrack from './baseTrack';
import PIXI from 'pixi.js';
import {Subject} from 'rx';
import {getRenderer} from '../configuration';
import menuFactory from './menu';
import {scaleModes} from '../../tracks/common/scaleModes';
import {displayModes} from '../../tracks/wig/modes';
import tooltipFactory from './tooltip';

const DEBOUNCE_TIMEOUT = 100;
const Math = window.Math;

const getFlags = () => ({
    brushChanged: false,
    dataChanged: false,
    dragFinished: false,
    heightChanged: false,
    heightChanging: false,
    hoverChanged: false,
    renderReset: false,
    settingsChanged: false,
    widthChanged: false,
});

function refreshRender(render, size) {
    if (render) {
        render.resize(size.width, size.height);
        Object.assign(render.view.style, {
            height: `${size.height  }px`,
            width: `${size.width  }px`
        });
    }
}

export class Track extends BaseTrack {

    refreshDataSubject = new Subject();

    shouldDisplayTooltips = true;

    domElement = document.createElement('div');
    tooltip = tooltipFactory(this.domElement);
    menuElement = menuFactory(this.domElement);
    container: PIXI.Container = new PIXI.Container();
    _pixiRenderer: PIXI.CanvasRenderer;
    _flags = getFlags();
    trackDataLoadingStatusChanged = null;

    _settings = null;
    projectContext = null;
    silentInteractions = false; // true, if `track` should not send events (like apply state) to global project context

    _disposables = [
        this.viewport.shortenedIntronsChangeSubject.subscribe(() => {
            this._flags.renderReset = true;
            this.invalidateCache();
        }),
        this.viewport.blatRegionChangeSubject.subscribe(() => {
            this._flags.blatRegionChanged = true;
            requestAnimationFrame(::this.tick);
        }),
        this.viewport.blastRegionChangeSubject.subscribe(() => {
            this._flags.blastRegionChanged = true;
            requestAnimationFrame(::this.tick);
        }),
        this.viewport.brushChangeSubject.subscribe(() =>
            this._flags.brushChanged = true),
        this.viewport.brushChangeSubject.subscribe((opts) => {
            this._flags.dragFinished = !opts || opts.reload;
            requestAnimationFrame(::this.tick);
            if (!opts || opts.reload) {
                this.refreshDataSubject.onNext(this);
            }
        }),
        this.viewport.canvasChangeSubject.subscribe(() => {
            this._refreshPixiRenderer();
            requestAnimationFrame(::this.tick);
        }),
        this.refreshDataSubject.debounce(DEBOUNCE_TIMEOUT).subscribe(() => {
            this._refreshCache();
        })
    ];

    _height = this.config.height;
    _isResizing = false;
    _maxHeight = this.trackConfig.maxHeight || this.config.maxHeight;
    _minHeight = this.trackConfig.minHeight || this.config.minHeight;

    _animating = false;
    _lastAnimationTime = null;

    _actions = null;

    moveBrush(params) {
        if (this.viewport.canTransform) {
            this.viewport.transform(params);
        }
    }

    get actions() {
        return this._actions;
    }

    get trackIsResizable() {
        return true;
    }

    get trackIsHidden() {
        return this._trackIsHidden;
    }

    getImageData() {
        if (this._pixiRenderer && this.container) {
            // this._pixiRenderer.render(this.container);
            return this._pixiRenderer.view;
        }
        return null;
    }

    tick(time) {
        if (this._pixiRenderer) {
            let somethingChanged = this.render(this._flags);
            somethingChanged = this.animate(time) || somethingChanged || this._flags.heightChanging;
            this._flags = getFlags();
            if (somethingChanged) {
                this._pixiRenderer.render(this.container);
            }
        }
    }

    requestRender() {
        requestAnimationFrame(::this.tick);
    }

    requestAnimation() {
        if (this._animating) {
            return;
        }
        requestAnimationFrame(::this.animationTrigger);
    }

    animationTrigger(time) {
        this._animating = true;
        if (this._pixiRenderer) {
            this._pixiRenderer.render(this.container);
        }
        if (this._lastAnimationTime) {
            if (this.animate(time - this._lastAnimationTime)) {
                requestAnimationFrame(::this.animationTrigger);
            }
            else {
                this._animating = false;
                this._lastAnimationTime = null;
            }
        }
        else {
            this._lastAnimationTime = time;
            requestAnimationFrame(::this.animationTrigger);
        }
    }

    constructor(opts) {
        super(opts);
        this.viewport.shortenedIntronsViewport.intronLength = opts.shortenedIntronLength;
        this.viewport.shortenedIntronsViewport.maximumRange = opts.shortenedIntronsMaximumRange;
        if (opts) {
            if (opts.restoredHeight) {
                this.height = opts.restoredHeight;
            }
            this.shouldDisplayTooltips = opts.displayTooltips;
            this.hoveringEffects = opts.hoveringEffects;
            this.projectContext = opts.projectContext;
            this.groupAutoScaleManager = opts.groupAutoScaleManager;
            this.silentInteractions = !!opts.silentInteractions;
            this.browserId = opts.browserId;
            this.reportTrackState();
        }
        this._refreshPixiRenderer();
        this._refreshCache();
        this._showCenterLine = opts.showCenterLine;
        requestAnimationFrame(::this.tick);
        this._pixiRenderer.plugins.interaction.autoPreventDefault = false;
    }

    reportTrackState(silent = false) {
        if (!this.projectContext || this.silentInteractions || this.config.format.toLowerCase() === 'ruler') {
            return;
        }
        const track = {
            bioDataItemId: this.config.name,
            duplicateId: this.config.duplicateId,
            format: this.config.format,
            height: this.height,
            index: this.config.indexPath,
            isLocal: this.config.isLocal,
            projectId: this.config.projectId,
            projectIdNumber: this.config.project ? this.config.project.id : undefined,
            state: this.state,
        };
        this.projectContext.applyTrackState(track, silent);
    }

    _getCurrentSize() {
        return this._pixiRenderer ? { height: this._pixiRenderer.height, width: this._pixiRenderer.width} : {height: 0, width: 0};
    }

    _getExpectedSize() {
        return {
            height: this.height,
            width: this.viewport.canvasSize
        };
    }

    requestRenderRefresh() {
        requestAnimationFrame(::this.tick);
    }

    updateScene() {
        this._pixiRenderer.render(this.container);
    }

    _refreshPixiRenderer(force = false) {
        const size = this._getCurrentSize();
        const newSize = this._getExpectedSize();

        if (this.isResizing) {
            this._flags.heightChanging = true;
        }

        if (!force && newSize.width === size.width && newSize.height === size.height)
            return;

        if (this._pixiRenderer) {
            refreshRender(this._pixiRenderer, newSize);
            if (!this.isResizing) {
                if (size.width !== newSize.width)
                    this._flags.widthChanged = true;
                if (size.height !== newSize.height)
                    this._flags.heightChanged = true;
            }
        } else {
            this._pixiRenderer = getRenderer(newSize);
            this.domElement.appendChild(this._pixiRenderer.view);
            refreshRender(this._pixiRenderer, newSize);
            this._flags.widthChanged = true;
            this._flags.heightChanged = true;
        }
        if (!this.isResizing) {
            this._resetRender();
        }
        requestAnimationFrame(::this.tick);
    }

    _resetRender() {
        this.container.removeChildren();
        this._flags.renderReset = true;
    }

    destructor() {
        super.destructor();
        if (this.tooltip) {
            this.tooltip.hide();
        }
        if (this._pixiRenderer && this._pixiRenderer.view) {
            this.container.removeChildren();
            this.domElement.removeChild(this._pixiRenderer.view);
            this._pixiRenderer.destroy(true);
            this._pixiRenderer = null;
        }
        this.clearData();
        for (const disposable of this._disposables)
            disposable.dispose();
    }

    clearData() {
    }

    hideTrack() {
        if (this.changeTrackVisibility) {
            this.changeTrackVisibility(false);
        }
    }

    showTrack() {
        if (this.changeTrackVisibility) {
            this.changeTrackVisibility(true);
        }
    }

    _refreshCache() {
        Promise.resolve().then(() => this.getNewCache())
            .then((somethingChanged) => {
                if (somethingChanged) {
                    this._flags.dataChanged = true;
                    requestAnimationFrame(::this.tick);
                }

            });
    }

    //noinspection JSDuplicatedDeclaration I know it, you stupid IDE
    get height() {
        return this._height;
    }

    get isResizing() {
        return this._isResizing;
    }

    set isResizing(value) {
        this._isResizing = value;
        if (!value) {
            this._flags.heightChanged = true;
            this._refreshPixiRenderer(true);
        }
    }

    //noinspection JSDuplicatedDeclaration
    set height(val) {
        const max = typeof this._maxHeight === 'function' ? this._maxHeight(this.state, this.trackConfig) : this._maxHeight;
        const min = typeof this._minHeight === 'function' ? this._minHeight(this.state, this.trackConfig) : this._minHeight;
        const newHeight = Math.floor(Math.max(Math.min(val, max), min));

        if (newHeight === this.height)
            return;

        this._height = newHeight;
        if (!this.isResizing) {
            this._flags.heightChanged = true;
        } else {
            this._flags.heightChanging = true;
        }
        this._refreshPixiRenderer();
    }

    globalSettingsChanged(state) {
        if (state) {
            this.shouldDisplayTooltips = state.displayTooltips;
            this.hoveringEffects = state.hoveringEffects;
            this.viewport.shortenedIntronsViewport.intronLength = state.shortenedIntronLength;
            this.viewport.shortenedIntronsViewport.maximumRange = state.shortenedIntronsMaximumRange;
            if(this._showCenterLine !== state.showCenterLine) {
                this._showCenterLine = state.showCenterLine;
                this._flags.dataChanged = true;
                this.requestRenderRefresh();
            }
        }
    }

    get trackHasCoverageSubTrack() {
        return false;
    }

    coverageScaleSettingsChanged(state) {
        if (this.trackHasCoverageSubTrack && !state.cancel) {
            if (this.state.coverageScaleMode === scaleModes.groupAutoScaleMode) {
                this.groupAutoScaleManager.unregisterTrack(this);
            }
            this.state.coverageScaleFrom = state.data.from;
            this.state.coverageScaleTo = state.data.to;
            this.state.coverageScaleMode = scaleModes.manualScaleMode;
            this.state.coverageLogScale = this.state.coverageDisplayMode === displayModes.defaultDisplayMode &&
                state.data.isLogScale;
            this._flags.dataChanged = true;
            this.reportTrackState();
            this.requestRenderRefresh();
        }
    }
    colorSettingsChanged(state) {
        if (this.config.format === 'BED') {
            state.cancel
                ? this.state.color = {}
                : this.state.color = state.data.settings;
        } else if (this.config.format === 'MOTIFS') {
            const color = this.state.color || {};
            color[state.strand] = state.data.settings;
            state.cancel
                ? this.state.color = {}
                : this.state.color = color;
        } else {
            state.cancel
                ? this.state.wigColors = {}
                : this.state.wigColors = state.data.settings;
        }
        this._flags.dataChanged = true;
        this.reportTrackState();
        this.requestRenderRefresh();
    }

    headerStyleSettingsChanged(state) {
        if (state.cancel) {
            this.state.header = {};
        } else {
            this.state.header = state.data.settings;
        }
        this._flags.dataChanged = true;
        this.reportTrackState();
        this.requestRenderRefresh();
    }

    groupAutoScaleChanged() {}

    get settings() {
        return this._settings;
    }

    /** @public */
    async getNewCache() {
    }

    invalidateCache() {
    }

    /** @public */
    render() {
        return true;
    }

    animate() {
        return false;
    }
}
