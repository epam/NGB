import {StatisticsContainer, initializeContainer} from './variants';
import {CachedTrackRenderer} from '../../../../../core';
import PIXI from 'pixi.js';
import {ZonesManager} from '../../../../../utilities';

export default class VcfRenderer extends CachedTrackRenderer {

    _manager = null;
    _variantContainers = [];
    _mask = null;

    constructor(config) {
        super();
        this._config = config;
        this._height = config.height;
        this._manager = new ZonesManager();
        this._chromosomeLine = new PIXI.Graphics();
        this._linesArea = new PIXI.Container();
        this._variantsArea = new PIXI.Container();
        this._tooltipArea = new PIXI.Container();
        this._bubblesArea = new PIXI.Container();

        this.container.addChild(this._chromosomeLine);
        this.container.addChild(this._variantsArea);
        this.container.addChild(this._linesArea);
        this.container.addChild(this._bubblesArea);
        this.container.addChild(this._tooltipArea);
        this.initializeCentralLine();
    }

    get height() {
        return this._height;
    }

    set height(value) {
        this._height = value;
    }

    rebuildContainer(viewport, cache) {
        super.rebuildContainer(viewport, cache);
        this._manager.configureZone('tooltip', {y1: 0, y2: this._config.zones.tooltip.height});
        this._manager.configureZone('default', {
            y1: this._config.zones.tooltip.height,
            y2: this.height - this._config.chromosomeLine.thickness
        });
        this._manager.configureZone('ends_containers', {
            y1: this._config.zones.tooltip.height,
            y2: this.height - this._config.chromosomeLine.thickness
        });
        cache.visualData = cache.data;
        this._drawChromosomeLine(viewport);
        this._drawVariants(viewport, cache.visualData);
        this._mask = this._manager.getZoneMask('default', {
            x1: -viewport.canvasSize,
            x2: 2 * viewport.canvasSize,
            y1: 0,
            y2: this.height
        });
        this._rebuildMask();
    }

    _rebuildMask() {
        const mask = new PIXI.Graphics();
        for (let i = 0; i < this._mask.length; i++) {
            const rect = this._mask[i];
            mask.beginFill(0x000000, 1);
            mask.drawRect(rect.x1 + this._drawScope.translateFactor, rect.y1 + this.container.parent.y, rect.x2 - rect.x1, rect.y2 - rect.y1);
            mask.endFill();
        }
        this._linesArea.mask = mask;
    }

    _drawChromosomeLine(viewport) {
        this._chromosomeLine.clear();
        this._chromosomeLine
            .lineStyle(this._config.chromosomeLine.thickness, this._config.chromosomeLine.fill, 1)
            .moveTo(0, this.height - this._config.chromosomeLine.thickness / 2)
            .lineTo(viewport.canvasSize, this.height - this._config.chromosomeLine.thickness / 2);
    }

    _drawVariants(viewport, data) {
        if (data === null || data === undefined)
            return;
        this._linesArea.removeChildren();
        this._variantsArea.removeChildren();
        this._tooltipArea.removeChildren();
        this._bubblesArea.removeChildren();
        this._variantContainers = [];

        for (let i = 0; i < data.variants.length; i++) {
            const variant = data.variants[i];
            const variantContainer = initializeContainer(variant, this._config, this._tooltipArea);
            variantContainer.container.y = this.height - this._config.chromosomeLine.thickness;
            variantContainer.render(viewport, this._manager);
            if (variantContainer instanceof StatisticsContainer) {
                this._bubblesArea.addChild(variantContainer.container);
            }
            else {
                this._variantsArea.addChild(variantContainer.container);
            }
            this._linesArea.addChild(variantContainer.linesGraphics);
            this._variantContainers.push(variantContainer);
        }

    }

    translateContainer(viewport, cache) {
        this.createDrawScope(viewport, cache);
        this._manager.configureZone('ends_containers', {
            y1: this._config.zones.tooltip.height,
            y2: this.height - this._config.chromosomeLine.thickness
        });
        for (let i = 0; i < this._variantContainers.length; i++) {
            const container = this._variantContainers[i];
            container.render(viewport, this._manager);
        }
        this._rebuildMask();
    }

    animate(time) {
        let somethingChanged = false;
        for (let i = 0; i < this._variantContainers.length; i++) {
            const container = this._variantContainers[i];
            somethingChanged = container.animate(time) || somethingChanged;
        }
        return somethingChanged;
    }

    _testVariant(cursor) {
        let hitVariant = null;
        const hoveredItems = [];
        for (let i = 0; i < this._variantContainers.length; i++) {
            const container = this._variantContainers[i];
            if (container.isHovers(cursor)) {
                if (container instanceof StatisticsContainer) {
                    return container;
                }
                hoveredItems.push(container);
            }
        }
        let prevPositioningInfo = null;
        for (let i = 0; i < hoveredItems.length; i++) {
            if (hoveredItems[i]._variant.positioningInfos) {
                for (let j = 0; j < hoveredItems[i]._variant.positioningInfos.length; j++) {
                    const info = hoveredItems[i]._variant.positioningInfos[j];
                    if (!prevPositioningInfo || prevPositioningInfo.layerIndex < info.layerIndex) {
                        prevPositioningInfo = info;
                        hitVariant = hoveredItems[i];
                    }
                }
            }
            else {
                hitVariant = hoveredItems[i];
                // snp variant - we should select it
                break;
            }
        }
        if (!hitVariant && hoveredItems.length > 0) {
            hitVariant = hoveredItems[0];
        }
        return hitVariant;
    }

    onMove(cursor) {
        const hitVariant = this._testVariant(cursor);
        for (let i = 0; i < this._variantContainers.length; i++) {
            const container = this._variantContainers[i];
            if (hitVariant === null) {
                container.unfade();
                container.unhover();
            }
            else if (hitVariant instanceof StatisticsContainer) {
                container.unfade();
                if (container === hitVariant)
                    container.hover();
                else
                    container.unhover();
            }
            else {
                container.unfade();
                if (container === hitVariant) {
                    container.hover();
                    container.unfade();
                } else {
                    container.unhover();
                    container.fade();
                }
            }
        }
        return hitVariant;
    }

    onClick(cursor) {
        return this._testVariant(cursor);
    }

}
