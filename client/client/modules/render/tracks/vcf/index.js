import {StatisticsContainer, VariantContainer, VcfRenderer} from './internal/drawing';
import {CachedTrack} from '../../core';
import {EventVariationInfo} from '../../../../app/shared/utils/events';
import VcfConfig from './vcfConfig';
import {VcfDataService} from '../../../../dataServices';

export class VCFTrack extends CachedTrack {

    static trackDefaultHeight = VcfConfig.height;

    dataService = new VcfDataService();
    _renderer: VcfRenderer = new VcfRenderer(VcfConfig, this.config.chromosome);
    _lastHovererd = null;

    async updateCache() {
        const reqToken = this.__currentDataUpdateReq = {};
        const data = await this.dataService.loadVcfTrack(this.cacheUpdateParameters(this.viewport));
        if (reqToken === this.__currentDataUpdateReq && this.cache) {
            this.cache.data = data;
            return await super.updateCache();
        }
        return false;
    }

    render(flags) {
        let somethingChanged = super.render(flags);
        if (flags.renderReset) {
            this.container.addChild(this._renderer.container);
            somethingChanged = true;
        }
        if (flags.brushChanged || flags.widthChanged || flags.heightChanged || flags.renderReset || flags.dataChanged) {
            this._renderer.height = this.height;
            this._renderer.render(this.viewport, this.cache, flags.heightChanged);
            somethingChanged = true;
        }
        return somethingChanged;
    }

    onMouseOut() {
        super.onMouseOut();
        this._lastHovererd = null;
        this._renderer.onMove();
        // for correct animations
        this.requestAnimation();
    }

    onClick({x, y}) {
        super.onClick({x, y});
        const variantContainer = this._renderer.onClick({x, y});
        if (variantContainer && variantContainer instanceof StatisticsContainer) {
            const length = variantContainer.variant.endIndex - variantContainer.variant.startIndex;
            const bubbleExpandFactor = 5;
            this.moveBrush({
                end: variantContainer.variant.endIndex + length / bubbleExpandFactor,
                start: variantContainer.variant.startIndex - length / bubbleExpandFactor
            });
        }
        else if (variantContainer instanceof VariantContainer) {
            const self = this;
            const mapEndContainersFn = function(m) {
                return {
                    chromosome: m.chromosome || self.config.chromosome.name,
                    chromosomeId: self.dataConfig.chromosomeId,
                    position: m.displayPosition || m.position
                };
            };
            const [endPoints] = variantContainer._endContainers.filter(m=>m.visible === true).map(mapEndContainersFn);
            const variantRequest = new EventVariationInfo(
                {
                    chromosome: {
                        id: this.config.chromosome.id,
                        name: this.config.chromosome.name
                    },
                    endPoints,
                    id: variantContainer._variant.identifier,
                    position: variantContainer._variant.serverStartIndex,
                    type: variantContainer._variant.type,
                    vcfFileId: this.dataConfig.id
                }
            );
            if (this.dataItemClicked !== null && this.dataItemClicked !== undefined) {
                this.dataItemClicked(this, variantRequest, {name: 'variant-request', position: {x, y}});
            }
        }
    }

    onHover({x, y}) {
        if (super.onHover({x, y})) {
            const hoveredItem = this._renderer.onMove({x, y});
            if (hoveredItem && !(hoveredItem instanceof StatisticsContainer) && this.shouldDisplayTooltips) {
                const tooltip = [
                    ['Chromosome', this.config.chromosome.name],
                    ['Start', hoveredItem.variant.startIndex],
                    ['End', hoveredItem.variant.endIndex],
                    ['ID', hoveredItem.variant.identifier],
                    ['Type', hoveredItem.variant.type],
                    ['REF', hoveredItem.variant.referenceAllele],
                    ['ALT', hoveredItem.variant.alternativeAlleles.join(', ')],
                    ['Quality', hoveredItem.variant.quality && hoveredItem.variant.quality.toFixed(2)]
                ];
                const filters = hoveredItem.variant.failedFilters || [];
                if (filters.length > 0) {
                    tooltip.push(['Filter', filters.map(filter => filter.value).join(', ')]);
                }
                this.tooltip.setContent(tooltip);
                this.tooltip.move({x, y});
                this.tooltip.show({x, y});
            } else {
                this.tooltip.hide();
            }
            if (hoveredItem !== this._lastHovererd) {
                this._lastHovererd = hoveredItem;
                this.requestAnimation();
                return false;
            }
        }
        return true;
    }

    animate(time) {
        return this._renderer.animate(time);
    }
}
