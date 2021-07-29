import * as PIXI from 'pixi.js';
import {getRenderer} from '../core';
import {VariantBaseRenderer, ShortVariantRenderer, StructuralVariantRenderer} from './renderers';
import $ from 'jquery';

export default class VariantRenderer {

    _canvas: HTMLElement;
    _pixiRenderer: PIXI.SystemRenderer;
    _container: PIXI.Container;
    _destroyed = false;

    _width;
    _height;

    _variant;
    _variantFeatureRenderer: VariantBaseRenderer;

    _displayTooltipFn = null;
    _affectedGeneTranscriptChanged = null;
    _options = null;

    constructor(canvas: HTMLElement, displayTooltip = null, affectedGeneTranscriptChanged = null) {
        this._canvas = canvas;
        this._width = canvas.clientWidth;
        this._height = 0;
        this._displayTooltipFn = displayTooltip;
        this._affectedGeneTranscriptChanged = affectedGeneTranscriptChanged;
        this._pixiRenderer = getRenderer({
            width: this.width,
            height: this.height
        }, {backgroundColor: 0xffffff});

        this.canvasElement.appendChild(this.renderer.view);

        this._container = new PIXI.Container();
        this.resizeRenderer();
        $(window).on('resize', () => {
            this.resizeRenderer();
        });

        requestAnimationFrame(::this.render);
    }

    get canvasElement() {
        return this._canvas;
    }

    get container(): PIXI.Container {
        return this._container;
    }

    get renderer(): PIXI.SystemRenderer {
        return this._pixiRenderer;
    }

    get variant() {
        return this._variant;
    }

    set variant(v) {
        this.updateVariant(v, true);
    }

    get options() {
        return this._options;
    }

    set options(opts) {
        this._options = opts;
    }

    get variantFeatureRenderer(): VariantBaseRenderer {
        return this._variantFeatureRenderer;
    }

    get width() {
        return this._width;
    }

    get height() {
        return this._height;
    }

    updateVariant(variant, rebuildRenderer) {
        this._variant = variant;
        if (rebuildRenderer) {
            this.changeFeatureRenderer();
        } else {
            this.variantSubFeatureChanged();
        }
    }

    variantSubFeatureChanged() {
        if (this.variantFeatureRenderer !== null && this.variantFeatureRenderer !== undefined) {
            const flags = this.getFlags();
            if (flags.widthChanged) {
                this.resizeRenderer();
            }
            flags.reset = true;
            this.variantFeatureRenderer.renderObjects(flags, this.getConfig());
            this.updateScene();
        }
    }

    changeFeatureRenderer() {
        this.container.removeChildren();
        if (this.variant !== null && this.variant !== undefined) {
            if (!this.variant.variantInfo.structural) {
                switch (this.variant.variantInfo.type.toLowerCase()) {
                    case 'del':
                    case 'ins':
                    case 'snp':
                    case 'snv':
                        this._variantFeatureRenderer = new ShortVariantRenderer(this.variant, ::this.onRendererHeightChanged, ::this._displayTooltipFn, ::this._affectedGeneTranscriptChanged, ::this.updateScene, ::this.reRenderScene);
                        break;
                }
            }
            else {
                this._variantFeatureRenderer = new StructuralVariantRenderer(this.variant, ::this.onRendererHeightChanged, ::this._displayTooltipFn, ::this._affectedGeneTranscriptChanged, ::this.updateScene, ::this.reRenderScene);
            }
            if (this.variantFeatureRenderer !== null && this.variantFeatureRenderer !== undefined) {
                this.variantFeatureRenderer._options = this.options;
                const flags = this.getFlags();
                if (flags.widthChanged) {
                    this.resizeRenderer();
                }
                flags.reset = true;
                this.variantFeatureRenderer.renderObjects(flags, this.getConfig());
                this.container.addChild(this.variantFeatureRenderer.container);
                this.updateScene();
            }
        }
    }

    onRendererHeightChanged() {
        this._height = this.variantFeatureRenderer.height + 20;
        this.resizeRenderer();
    }

    getFlags() {
        const flags = {
            widthChanged: false,
            reset: false
        };
        if (this.canvasElement !== null && this.width !== $(this.canvasElement).width()) {
            this._width = $(this.canvasElement).width();
            flags.widthChanged = true;
        }
        return flags;
    }

    getConfig() {
        return {
            width: this.width
        };
    }

    render() {
        if (this.renderer !== null && this.renderer !== undefined) {
            const flags = this.getFlags();
            if (flags.widthChanged) {
                this.resizeRenderer();
            }
            if (this.variantFeatureRenderer !== null && this.variantFeatureRenderer !== undefined)
                this.variantFeatureRenderer.render(flags, this.getConfig());
            this.renderer.render(this.container);
        }
    }

    updateScene() {
        const fn = () => {
            if (this.renderer) {
                this.renderer.render(this.container);
            }
        };
        requestAnimationFrame(fn);
    }

    reRenderScene(force = false) {
        if (this.variantFeatureRenderer) {
            this.variantFeatureRenderer._options = this.options;
            if (force) {
                this.variantFeatureRenderer._presentationChanged = true;
            }
        }
        requestAnimationFrame(::this.render);
    }

    resizeRenderer() {
        if (this.renderer) {
            this.renderer.resize(this.width, this.height);

            Object.assign(this.renderer.view.style, {
                width: `${this.width}px`,
                height: `${this.height}px`
            });
            this.render();
        }
    }

    destroy() {
        this._destroyed = true;
        if (this._pixiRenderer && this._pixiRenderer.view) {
            this.container.removeChildren();
            this.canvasElement.removeChild(this._pixiRenderer.view);
            this._pixiRenderer.destroy(true);

            // Forcing WebGL to lose renderer's context.
            // This is known pixi.js issue (https://github.com/pixijs/pixi.js/issues/2233), which have been fixed at pixi.js:dev branch,
            // but haven't been merged at master branch.
            // Todo: remove code below which forces WebGL to lose context when pixi.js:dev branch will be merged (https://github.com/pixijs/pixi.js/branches)
            if (this._pixiRenderer.currentRenderTarget) {
                const extension = this._pixiRenderer.currentRenderTarget.gl.getExtension('WEBGL_lose_context');
                if (extension !== null && extension !== undefined) {
                    extension.loseContext();
                }
            }
            this._pixiRenderer = null;
        }
    }

}
