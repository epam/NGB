import angular from 'angular';

export default class ngbVariantQualityDiagramController {

    static get UID() {
        return 'ngbVariantQualityDiagramController';
    }

    projectContext;

    /**
     * @constructor
     * @param {$scope} scope
     * @param {projectDataService} dataService
     * @param {dispatcher} dispatcher
     */
    /** @ngInject */
    constructor($scope, dispatcher, projectContext, ngbVariantQualityDiagramConstants, vcfDataService) {
        this.isProgressShown = true;
        const __dispatcher = this._dispatcher = dispatcher;
        this.projectContext = projectContext;
        this._vcfDataService = vcfDataService;
        this._scope = $scope;

        $scope.options = {
            chart: {
                color: ['#9cabe0'],
                duration: 500,
                showControls : false,
                showLegend : false,
                showValues: true,
                stacked : true,
                tooltip: {
                    enabled: false
                },
                type: 'multiBarChart',
                valueFormat: (d) => d,
                x: (d) => d.label,
                y: (d) => d.value,
                xAxis : {
                    showMaxMin : true
                },
                yAxis : {
                    tickFormat : (t) => t
                },

            },
            title: {
                enable: true,
                text: 'Variants quality'
            }
        };


        const reloadPanel = ::this.INIT;
        const updating = async () => {
            this.isProgressShown = true;
        };
        this._dispatcher.on('variants:loading:started', updating);
        this._dispatcher.on('variants:loading:finished', reloadPanel);
        // We must remove event listener when component is destroyed.

        $scope.$on('$destroy', () => {
            __dispatcher.removeListener('variants:loading:started', updating);
            __dispatcher.removeListener('variants:loading:finished', reloadPanel);
        });


        this.constants = ngbVariantQualityDiagramConstants;


        (async() => {
            await this.INIT();
            angular.element(window).on('resize', () => {
                this._scope.api && angular.isFunction(this._scope.api.update) ? this._scope.api.update() : '';
            });

        })();
    }

    async INIT() {
        this.noDataToDisplay = !this.projectContext.filteredVariants ||
            this.projectContext.filteredVariants.length === 0;
        if (this.projectContext.reference && this.projectContext.filteredVariants) {
            await this.updateDiagram(this.projectContext.filteredVariants,
                this.projectContext.isVariantsLoading);
            this.isProgressShown = this.projectContext.isVariantsLoading;
            this._scope.$apply();
        }
    }

    makeNvD3ChartObjectFromData(variantQualities) {
        const nvd3DataObject = [],
            nvd3DataObjectItem = {
                values: []
            };


        let maxQual = variantQualities.length > 0 ? variantQualities[0].quality : undefined;
        let minQual = variantQualities.length > 0 ? variantQualities[0].quality : undefined;
        for (const variation of variantQualities) {
            const quality = variation.quality;
            if (quality === undefined) {
                continue;
            }
            minQual = minQual === undefined ? quality : Math.min(minQual, quality);
            maxQual = maxQual === undefined ? quality : Math.max(maxQual, quality);
        }
        maxQual = maxQual === undefined ? 0 : maxQual;
        minQual = minQual === undefined ? 0 : minQual;
        const qualStep = this.constants.qualityStep;
        const bucketCount = Math.ceil((maxQual - minQual) / qualStep) + 1;

        const sampleData = Array(bucketCount);
        for (let i = 0; i < bucketCount; ++i) {
            sampleData[i] = {label: (minQual + qualStep * i + qualStep / 2) | 0, value: 0};
        }

        for (const variation of variantQualities) {
            const quality = variation.quality;
            const bucketIdx = quality !== undefined && maxQual !== minQual ?
            ((bucketCount - 1) * (quality - minQual) / (maxQual - minQual)) | 0 : 0;
            ++sampleData[bucketIdx].value;
        }


        nvd3DataObjectItem.values = sampleData;
        nvd3DataObject.push(nvd3DataObjectItem);

        return nvd3DataObject;

    }

    async updateDiagram(variantQualities, isLoading) {
        if (isLoading) {
            return;
        }
        (!variantQualities || variantQualities.length === 0) ?
            this._scope.data = [] : this._scope.data = this.makeNvD3ChartObjectFromData(variantQualities);

        this._scope.api && angular.isFunction(this._scope.api.update) ? this._scope.api.update() : '';
    }

}