import angular from 'angular';
import {NumberFormatter} from '../../../../../modules/render/utilities';

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
        this._dispatcher.on('variants:group:quality:started', updating);
        this._dispatcher.on('variants:group:quality:finished', reloadPanel);
        // We must remove event listener when component is destroyed.

        $scope.$on('$destroy', () => {
            __dispatcher.removeListener('variants:group:quality:started', updating);
            __dispatcher.removeListener('variants:group:quality:finished', reloadPanel);
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
        this.noDataToDisplay = !this.projectContext.variantsDataByQuality ||
            this.projectContext.variantsDataByQuality.length === 0;
        if (this.projectContext.reference && this.projectContext.variantsDataByQuality) {
            await this.updateDiagram(this.projectContext.variantsDataByQuality,
                this.projectContext.isVariantsGroupByQualityLoading);
            this.isProgressShown = this.projectContext.isVariantsGroupByQualityLoading;
            this._scope.$apply();
        }
    }

    makeNvD3ChartObjectFromData(variantQualities) {
        const nvd3DataObject = [],
            nvd3DataObjectItem = {
                values: []
            };


        let maxQual = undefined;
        let minQual = undefined;
        for (let i = 0; i < variantQualities.length; i++) {
            const {groupName} = variantQualities[i];
            const quality = +groupName;
            minQual = minQual === undefined ? quality : Math.min(minQual, quality);
            maxQual = maxQual === undefined ? quality : Math.max(maxQual, quality);
        }

        maxQual = maxQual === undefined ? 0 : maxQual;
        minQual = minQual === undefined ? 0 : minQual;
        const maxBucketCount = this.constants.maximumBars;
        let qualStep = this.constants.qualityStep;
        let bucketCount = Math.ceil((maxQual - minQual) / qualStep) + 1;
        if (bucketCount > maxBucketCount) {
            bucketCount = maxBucketCount;
            qualStep = (maxQual - minQual) / (bucketCount - 1);
        }

        const sampleData = Array(bucketCount);
        for (let i = 0; i < bucketCount; ++i) {
            sampleData[i] = {label: NumberFormatter.textWithPrefix(Math.ceil((minQual + qualStep * i + qualStep / 2)) | 0), value: 0};
        }

        for (let i = 0; i < variantQualities.length; i++) {
            const {entriesCount, groupName} = variantQualities[i];
            const quality = +groupName;
            const bucketIdx = quality !== undefined && maxQual !== minQual ?
            ((bucketCount - 1) * (quality - minQual) / (maxQual - minQual)) | 0 : 0;
            sampleData[bucketIdx].value += entriesCount;
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