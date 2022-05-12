import * as PIXI from 'pixi.js-legacy';
import {CachedTrackRendererWithVerticalScroll, drawingConfiguration} from '../../../../core';
import {FeatureRenderer, GeneHistogram} from './features';
import {GeneTransformer} from '../data/geneTransformer';

export default class GeneRenderer extends CachedTrackRendererWithVerticalScroll {

    _featureRenderer: FeatureRenderer = null;
    _geneHistogram: GeneHistogram = null;
    _labelsContainer: PIXI.Container = null;
    _attachedElementsContainer: PIXI.Container = null;
    _dockableElementsContainer: PIXI.Container = null;
    _hoveredItemContainer: PIXI.Container = null;
    _mask: PIXI.Graphics = null;

    _transformer: GeneTransformer = null;

    _actualHeight = null;
    _gffColorByFeatureType = false;
    _gffShowNumbersAminoacid = false;
    _collapsedMode = false;
    _geneFeatures = [];
    _showCenterLine;

    _graphicsSprite: PIXI.Sprite = null;
    _hoveredGraphicsSprite: PIXI.Sprite = null;

    constructor(config, transformer: GeneTransformer, track) {
        super(track);
        this._config = {...config};
        this._featureRenderer = new FeatureRenderer(this._config, track);
        this._geneHistogram = new GeneHistogram(config);
        this._transformer = transformer;
        this._labelsContainer = new PIXI.Container();
        this._dockableElementsContainer = new PIXI.Container();
        this._attachedElementsContainer = new PIXI.Container();
        this._hoveredItemContainer = new PIXI.Container();
        this._mask = new PIXI.Graphics();
        this.container.removeChild(this.verticalScroll);
        this.container.addChild(this.geneHistogram);
        this.container.addChild(this.verticalScroll);
        this.container.addChild(this._attachedElementsContainer);
        this.container.addChild(this._dockableElementsContainer);
        this.container.addChild(this._labelsContainer);
        this.initializeCentralLine();
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

    render(
        viewport,
        cache,
        isRedraw,
        _showCenterLine,
        _gffColorByFeatureType = false,
        _gffShowNumbersAminoacid,
        _collapsedMode = false,
        _geneFeatures = []
    ) {
        this._gffColorByFeatureType = _gffColorByFeatureType;
        this._showCenterLine = _showCenterLine;
        this._gffShowNumbersAminoacid = _gffShowNumbersAminoacid;
        this._collapsedMode = _collapsedMode;
        this._geneFeatures = _geneFeatures;

        if (!isRedraw) {
            this.scroll(viewport, 0, cache);
        }
        super.render(viewport, cache, isRedraw, _showCenterLine);
    }

    get needConvertGraphicsToTexture() {
        return true;
    }

    rebuildContainer(viewport, cache) {
        if (!this.pixiRenderer) {
            return;
        }
        super.rebuildContainer(viewport, cache);
        const clearPreviousSprite = sprite => {
            if (sprite && sprite.texture && sprite.texture.baseTexture) {
                sprite.texture.destroy(true);
            }
        };
        this.dataContainer.removeChildren();
        this._dockableElementsContainer.removeChildren();
        this._attachedElementsContainer.removeChildren();
        this._labelsContainer.removeChildren();
        clearPreviousSprite(this._graphicsSprite);
        clearPreviousSprite(this._hoveredGraphicsSprite);

        this._dockableElementsContainer.x = this.dataContainer.x;
        this._dockableElementsContainer.y = this.dataContainer.y;
        this._dockableElementsContainer.scale = this.dataContainer.scale;
        const isHistogram = this._transformer.isHistogramDrawingModeForViewport(viewport, cache);
        if (isHistogram) {
            this.geneHistogram.totalHeight = this.height;
            this.geneHistogram.renderHistogram(viewport, GeneTransformer.transformPartialHistogramData(viewport, cache.histogramData));
            this._actualHeight = null;
        } else {
            this.geneHistogram.clear();
        }
        if (!isHistogram && cache.data !== null && cache.data !== undefined) {
            this.featureRenderer._opts = {
                gffColorByFeatureType: this._gffColorByFeatureType,
                gffShowNumbersAminoacid: this._gffShowNumbersAminoacid,
                collapsedMode: this._collapsedMode,
                geneFeatures: this._geneFeatures
            };
            this.featureRenderer.prepare();
            const {graphics, hoveredGraphics, highlightGraphics, hoveredHighlightGraphics} = this.featureRenderer.render(
                cache.data,
                viewport,
                this._labelsContainer,
                this._dockableElementsContainer,
                this._attachedElementsContainer
            );
            if (graphics !== null) {
                if (this.needConvertGraphicsToTexture) {
                    let temporaryContainer = new PIXI.Container();
                    if (highlightGraphics && highlightGraphics.children.length > 0) {
                        temporaryContainer.addChild(highlightGraphics);
                    }
                    temporaryContainer.addChild(graphics);
                    const coordinates = this.featureRenderer.textureCoordinates;
                    const texture = this.pixiRenderer.generateTexture(temporaryContainer, {
                        scaleMode: drawingConfiguration.scaleMode,
                        resolution: drawingConfiguration.resolution
                    });
                    if (this._graphicsSprite) {
                        this._graphicsSprite.texture = texture;
                    } else {
                        this._graphicsSprite = new PIXI.Sprite(texture);
                    }
                    this._graphicsSprite.position.x = coordinates.x;
                    this._graphicsSprite.position.y = coordinates.y;
                    this.dataContainer.addChild(this._graphicsSprite);
                    graphics.clear();
                    temporaryContainer = null;
                } else {
                    highlightGraphics && this.dataContainer.addChild(highlightGraphics);
                    this.dataContainer.addChild(graphics);
                }
            }
            if (hoveredGraphics) {
                this._hoveredItemContainer.removeChildren();
                if (this.needConvertGraphicsToTexture) {
                    let temporaryContainer = new PIXI.Container();
                    if (hoveredHighlightGraphics && hoveredHighlightGraphics.children.length > 0) {
                        temporaryContainer.addChild(hoveredHighlightGraphics);
                    }
                    temporaryContainer.addChild(hoveredGraphics);
                    const coordinates = this.featureRenderer.textureCoordinates;
                    const texture = this.pixiRenderer.generateTexture(temporaryContainer, {
                        scaleMode: drawingConfiguration.scaleMode,
                        resolution: drawingConfiguration.resolution
                    });
                    if (this._hoveredGraphicsSprite) {
                        this._hoveredGraphicsSprite.texture = texture;
                    } else {
                        this._hoveredGraphicsSprite = new PIXI.Sprite(texture);
                    }
                    this._hoveredGraphicsSprite.position.x = coordinates.x;
                    this._hoveredGraphicsSprite.position.y = coordinates.y;
                    this._hoveredItemContainer.addChild(this._hoveredGraphicsSprite);
                    hoveredGraphics.clear();
                    temporaryContainer = null;
                } else {
                    hoveredHighlightGraphics && this._hoveredItemContainer.addChild(hoveredHighlightGraphics);
                    this._hoveredItemContainer.addChild(hoveredGraphics);
                }
                this.hoverItem(null, viewport, false);
            }
            this._actualHeight = this.featureRenderer.getActualHeight();
            this.dataContainer.addChild(this._hoveredItemContainer);
        }
        this.scroll(viewport, null);
    }

    scroll(viewport, yDelta, cache) {
        this.hoverItem(null);
        if (cache && this._transformer.isHistogramDrawingModeForViewport(viewport, cache)) {
            this.geneHistogram.totalHeight = this.height;
            this.geneHistogram.renderHistogram(viewport, GeneTransformer.transformPartialHistogramData(viewport, cache.histogramData));
            this._actualHeight = null;
        }
        super.scroll(viewport, yDelta);
        this._labelsContainer.y = this.dataContainer.y;
        this._dockableElementsContainer.x = this.dataContainer.x;
        this._dockableElementsContainer.y = this.dataContainer.y;
        this._dockableElementsContainer.scale = this.dataContainer.scale;
        this.featureRenderer.manageLabels(viewport, this.height, this.dataContainer.y);
        this.featureRenderer.manageDockableElements(viewport);
        this.featureRenderer.manageAttachedElements(viewport, this.height, this.dataContainer.y);
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
            this.featureRenderer.manageLabels(viewport, this.height, this.dataContainer.y);
            this.featureRenderer.manageDockableElements(viewport);
            this.featureRenderer.manageAttachedElements(viewport, this.height, this.dataContainer.y);
            this.hoverItem(null);
        }
        this.manageMask(viewport);
    }

    manageMask(viewport) {
        this.featureRenderer.manageMask(this._mask, viewport);
        this.dataContainer.mask = this._mask;
        this._labelsContainer.mask = this._mask;
    }

    checkPosition(viewport, cache, position, isHistogram, isCollapsedMode = false) {
        if (isHistogram) {
            if (this.dataContainer !== null && this.dataContainer !== undefined)
                return this._geneHistogram.checkPosition(position, GeneTransformer
                        .transformPartialHistogramData(viewport, cache.histogramData), viewport);

        }
        else {
            if (this.dataContainer !== null && this.dataContainer !== undefined)
                return this.featureRenderer.checkPosition(position, this.dataContainer, isCollapsedMode);
        }
        return null;
    }

    hoverItem(hoveredItem, viewport, isHistogram, cache) {
        if (!hoveredItem && this._geneHistogram.hoverItem) {
            this._geneHistogram.hoverItem(null, null, null);
        }
        if (this.featureRenderer.hoverItem) {
            this.featureRenderer.hoverItem(null, null, this._hoveredItemContainer);
        }
        if (hoveredItem) {
            if (isHistogram && this._geneHistogram.hoverItem && cache) {
                return this._geneHistogram.hoverItem(hoveredItem, viewport, GeneTransformer
                    .transformPartialHistogramData(viewport, cache.histogramData));
            } else if (!isHistogram && this.featureRenderer.hoverItem) {
                return this.featureRenderer.hoverItem(hoveredItem, viewport, this._hoveredItemContainer);
            }
        }
        return true;
    }
}
