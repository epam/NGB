import BaseTrack from './baseTrack';
import PIXI from 'pixi.js';
import {Subject} from 'rx';
import {getRenderer} from '../configuration';
import menuFactory from './menu';
import tooltipFactory from './tooltip';
import scaleModes from '../../tracks/wig/modes';

const DEBOUNCE_TIMEOUT = 100;
const Math = window.Math;

const getFlags = () => ({
    brushChanged: false,
    dataChanged: false,
    heightChanged: false,
    renderReset: false,
    settingsChanged: false,
    widthChanged: false,
    dragFinished: false,
    hoverChanged: false
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

    _disposables = [
        this.viewport.shortenedIntronsChangeSubject.subscribe(() => {
            this._flags.renderReset = true;
            this.invalidateCache();
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
    _maxHeight = this.config.maxHeight;
    _minHeight = this.config.minHeight;

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

    tick(time) {
        if (this._pixiRenderer) {
            let somethingChanged = this.render(this._flags);
            somethingChanged = this.animate(time) || somethingChanged;
            this._flags = getFlags();
            if (somethingChanged) {
                this._pixiRenderer.render(this.container);
            }
        }
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
            this.projectContext = opts.projectContext;
            this.reportTrackState();
        }
        this._refreshPixiRenderer();
        this._refreshCache();
        requestAnimationFrame(::this.tick);
    }

    reportTrackState() {
        if (!this.projectContext || this.config.format.toLowerCase() === 'ruler') {
            return;
        }
        const track = {
            bioDataItemId: this.config.name,
            index: this.config.indexPath,
            projectId: this.config.projectId,
            height: this.height,
            state: this.state,
            format: this.config.format
        };
        this.projectContext.applyTrackState(track);
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

    _refreshPixiRenderer() {
        const size = this._getCurrentSize();
        const newSize = this._getExpectedSize();

        if (newSize.width === size.width && newSize.height === size.height)
            return;

        if (this._pixiRenderer) {
            refreshRender(this._pixiRenderer, newSize);

            if (size.width !== newSize.width)
                this._flags.widthChanged = true;
            if (size.height !== newSize.height)
                this._flags.heightChanged = true;
        } else {
            this._pixiRenderer = getRenderer(newSize);
            this.domElement.appendChild(this._pixiRenderer.view);
            refreshRender(this._pixiRenderer, newSize);
            this._flags.widthChanged = true;
            this._flags.heightChanged = true;
        }

        this._resetRender();
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

    //noinspection JSDuplicatedDeclaration
    set height(val) {
        const newHeight = Math.max(Math.min(val, this._maxHeight), this._minHeight);

        if (newHeight === this.height)
            return;

        this._height = newHeight;
        this._flags.heightChanged = true;
        this._refreshPixiRenderer();
    }

    globalSettingsChanged(state) {
        if (state) {
            this.shouldDisplayTooltips = state.displayTooltips;
            this.viewport.shortenedIntronsViewport.intronLength = state.shortenedIntronLength;
            this.viewport.shortenedIntronsViewport.maximumRange = state.shortenedIntronsMaximumRange;
        }
    }

    get trackHasCoverageSubTrack() {
        return false;
    }

    coverageScaleSettingsChanged(state) {
        if (this.trackHasCoverageSubTrack) {
            if (state.cancel) {
                this.state.coverageScaleFrom = undefined;
                this.state.coverageScaleTo = undefined;
                this.state.coverageScaleMode = scaleModes.defaultScaleMode;
            } else {
                this.state.coverageScaleFrom = state.data.from;
                this.state.coverageScaleTo = state.data.to;
                this.state.coverageScaleMode = scaleModes.manualScaleMode;
                this.state.coverageLogScale = state.data.isLogScale;
            }
            this._flags.dataChanged = true;
            this.reportTrackState();
            this.requestRenderRefresh();
        }
    }

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