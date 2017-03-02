import {CachedTrackRenderer, drawingConfiguration} from '../../../../core';
import {FeatureRenderer, GeneHistogram} from './features';
import {GeneTransformer} from '../data/geneTransformer';
import PIXI from 'pixi.js';

export default class GeneRenderer extends CachedTrackRenderer {

    _config = null;

    _featureRenderer: FeatureRenderer = null;
    _geneHistogram: GeneHistogram = null;
    _labelsContainer: PIXI.Container = null;
    _attachedElementsContainer: PIXI.Container = null;
    _dockableElementsContainer: PIXI.Container = null;
    _mask: PIXI.Graphics = null;

    _transformer: GeneTransformer = null;

    _verticalScroll: PIXI.Graphics = null;
    _height = 0;
    _actualHeight = null;
    _gffColorByFeatureType = false;
    _gffShowNumbersAminoacid = false;

    constructor(config, transformer: GeneTransformer, pixiRenderer) {
        super();
        this._config = config;
        this._pixiRenderer = pixiRenderer;
        this._featureRenderer = new FeatureRenderer(config);
        this._geneHistogram = new GeneHistogram(config);
        this._transformer = transformer;
        this._labelsContainer = new PIXI.Container();
        this._dockableElementsContainer = new PIXI.Container();
        this._attachedElementsContainer = new PIXI.Container();
        this._verticalScroll = new PIXI.Graphics();
        this._mask = new PIXI.Graphics();
        this.container.addChild(this.geneHistogram);
        this.container.addChild(this._verticalScroll);
        this.container.addChild(this._attachedElementsContainer);
        this.container.addChild(this._dockableElementsContainer);
        this.container.addChild(this._labelsContainer);
    }

    get config() {
        return this._config;
    }

    get featureRenderer(): FeatureRenderer {
        return this._featureRenderer;
    }

    get geneHistogram(): GeneHistogram {
        return this._geneHistogram;
    }

    get verticalScroll(): PIXI.Graphics {
        return this._verticalScroll;
    }

    get height() {
        return this._height;
    }

    set height(value) {
        this._height = value;
    }

    get actualHeight() {
        return this._actualHeight;
    }

    render(viewport, cache, heightChanged, _gffColorByFeatureType = false, _gffShowNumbersAminoacid) {
        const gffColorByFeatureTypeChanged = this._gffColorByFeatureType !== _gffColorByFeatureType;
        this._gffColorByFeatureType = _gffColorByFeatureType;

        const gffShowNumbersAminoacidChanged = this._gffShowNumbersAminoacid !== _gffShowNumbersAminoacid;
        this._gffShowNumbersAminoacid = _gffShowNumbersAminoacid;
        const isRedraw = gffColorByFeatureTypeChanged||gffShowNumbersAminoacidChanged;
        if (!isRedraw && heightChanged) {
            this.scroll(viewport, 0);
        }
        else {
            super.render(viewport, cache, isRedraw);
        }
    }

    get needConvertGraphicsToTexture() {
        return true;
    }

    rebuildContainer(viewport, cache) {
        super.rebuildContainer(viewport, cache);

        this.dataContainer.removeChildren();
        this._dockableElementsContainer.removeChildren();
        this._attachedElementsContainer.removeChildren();
        this._labelsContainer.removeChildren();

        this._dockableElementsContainer.x = this.dataContainer.x;
        this._dockableElementsContainer.y = this.dataContainer.y;
        this._dockableElementsContainer.scale = this.dataContainer.scale;

        if (this._transformer.isHistogramDrawingModeForViewport(viewport, cache)) {
            this.geneHistogram.renderHistogram(viewport, GeneTransformer.transformPartialHistogramData(viewport, cache.histogramData));
            this._actualHeight = null;
        }
        else if (cache.data !== null && cache.data !== undefined) {
            this.geneHistogram.clear();
            this.featureRenderer._opts = {
                gffColorByFeatureType: this._gffColorByFeatureType,
                gffShowNumbersAminoacid: this._gffShowNumbersAminoacid
            };
            this.featureRenderer.prepare();
            const graphics = this.featureRenderer.render(cache.data, viewport, this._labelsContainer, this._dockableElementsContainer, this._attachedElementsContainer);
            if (graphics !== null) {
                if (this.needConvertGraphicsToTexture) {
                    let temporaryContainer = new PIXI.Container();
                    temporaryContainer.addChild(graphics);
                    const coordinates = this.featureRenderer.textureCoordinates;
                    const texture = temporaryContainer.generateTexture(this._pixiRenderer, drawingConfiguration.resolution, drawingConfiguration.scale);
                    const sprite = new PIXI.Sprite(texture);
                    sprite.position.x = coordinates.x;
                    sprite.position.y = coordinates.y;
                    this.dataContainer.addChild(sprite);
                    graphics.clear();
                    temporaryContainer = null;
                } else {
                    this.dataContainer.addChild(graphics);
                }
            }
            this.featureRenderer.manageLabels(viewport);
            this.featureRenderer.manageDockableElements(viewport);
            this.featureRenderer.manageAttachedElements(viewport);
            this._actualHeight = this.featureRenderer.getActualHeight();
        }
        this.scroll(viewport, null);
    }

    scrollIndicatorBoundaries(viewport) {
        if (this.actualHeight && this.height < this.actualHeight) {
            return {
                height: this.height * this.height / this.actualHeight,
                width: this.config.scroll.width,
                x: viewport.canvasSize - this.config.scroll.width - this.config.scroll.margin,
                y: -this.dataContainer.y / this.actualHeight * this.height
            };
        }
        return null;
    }

    drawVerticalScroll(viewport) {
        this.verticalScroll.clear();
        if (this.actualHeight && this.height < this.actualHeight) {
            const scrollHeight = this.height * this.height / this.actualHeight;
            this.verticalScroll
                .beginFill(this.config.scroll.fill, this._verticalScrollIsHovered ? this.config.scroll.hoveredAlpha : this.config.scroll.alpha)
                .drawRect(
                    viewport.canvasSize - this.config.scroll.width - this.config.scroll.margin,
                    -this.dataContainer.y / this.actualHeight * this.height,
                    this.config.scroll.width,
                    scrollHeight
                )
                .endFill();
        }
    }

    _verticalScrollIsHovered = false;

    hoverVerticalScroll(viewport) {
        if (!this._verticalScrollIsHovered) {
            this._verticalScrollIsHovered = true;
            this.drawVerticalScroll(viewport);
            return true;
        }
        return false;
    }

    unhoverVerticalScroll(viewport) {
        if (this._verticalScrollIsHovered) {
            this._verticalScrollIsHovered = false;
            this.drawVerticalScroll(viewport);
            return true;
        }
        return false;
    }

    isScrollable() {
        return this.actualHeight && this.height < this.actualHeight;
    }

    canScroll(yDelta) {
        if (this.actualHeight && this.height < this.actualHeight) {
            let __y = this.dataContainer.y;
            if (yDelta !== null) {
                __y += yDelta;
            }
            return __y <= 0 && __y >= this.height - this.actualHeight;
        }
        return false;
    }

    setScrollPosition(viewport, indicatorPosition) {
        this.scroll(viewport, - indicatorPosition * this.actualHeight / this.height - this.dataContainer.y);
    }

    scroll(viewport, yDelta) {
        if (this.actualHeight && this.height < this.actualHeight) {
            let __y = this.dataContainer.y;
            if (yDelta !== null) {
                __y += yDelta;
            }
            __y = Math.min(0, Math.max(this.height - this.actualHeight, __y));
            this.dataContainer.y = __y;
            this.drawVerticalScroll(viewport);
        }
        else {
            this.dataContainer.y = 0;
            this.drawVerticalScroll(null);
        }
        this._labelsContainer.y = this.dataContainer.y;
        this._dockableElementsContainer.x = this.dataContainer.x;
        this._dockableElementsContainer.y = this.dataContainer.y;
        this._dockableElementsContainer.scale = this.dataContainer.scale;
        this.featureRenderer.manageLabels(viewport);
        this.featureRenderer.manageDockableElements(viewport);
        this.featureRenderer.manageAttachedElements(viewport);
        this.manageMask(viewport);
    }

    translateContainer(viewport, cache) {
        if (this._transformer.isHistogramDrawingModeForViewport(viewport, cache)) {
            this.geneHistogram.renderHistogram(viewport, GeneTransformer.transformPartialHistogramData(viewport, cache.histogramData));
        }
        else {
            super.translateContainer(viewport, cache);
            this._dockableElementsContainer.x = this.dataContainer.x;
            this._dockableElementsContainer.y = this.dataContainer.y;
            this._dockableElementsContainer.scale = this.dataContainer.scale;
            this.featureRenderer.manageLabels(viewport);
            this.featureRenderer.manageDockableElements(viewport);
            this.featureRenderer.manageAttachedElements(viewport);
        }
        this.manageMask(viewport);
    }

    manageMask(viewport) {
        this.featureRenderer.manageMask(this._mask, viewport);
        this.dataContainer.mask = this._mask;
        this._labelsContainer.mask = this._mask;
    }

    checkPosition(viewport, cache, position, isHistogram) {
        if (isHistogram) {
            if (this.dataContainer !== null && this.dataContainer !== undefined)
                return this._geneHistogram.checkPosition(position, GeneTransformer
                        .transformPartialHistogramData(viewport, cache.histogramData), viewport,
                    this._geneHistogram._config.levels.margin,
                    this._geneHistogram._config.histogram.height);

        }
        else {
            if (this.dataContainer !== null && this.dataContainer !== undefined)
                return this.featureRenderer.checkPosition(position, this.dataContainer);
        }
        return null;
    }

}

