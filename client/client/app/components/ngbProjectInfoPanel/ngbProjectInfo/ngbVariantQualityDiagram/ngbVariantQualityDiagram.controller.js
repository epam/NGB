import {NumberFormatter} from '../../../../../modules/render/utilities';
import nvd3ChartController from '../nvd3-chart-controller';

export default class ngbVariantQualityDiagramController extends nvd3ChartController{
    static get UID() {
        return 'ngbVariantQualityDiagramController';
    }
    projectContext;
    constructor(
        $scope,
        $timeout,
        $element,
        dispatcher,
        projectContext,
        ngbVariantQualityDiagramConstants,
        nvd3resizer,
        nvd3dataCorrection
    ) {
        super($scope, $element, nvd3resizer, nvd3dataCorrection);
        this.isProgressShown = true;
        this.projectContext = projectContext;
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
                noData: 'No Data Available'
            },
            title: {
                enable: true,
                text: 'Variants quality'
            }
        };
        const reloadPanel = this.INIT.bind(this);
        const updating = () => this.isProgressShown = true;
        dispatcher.on('variants:group:quality:started', updating);
        dispatcher.on('variants:group:quality:finished', reloadPanel);
        dispatcher.on('refresh:project:info', reloadPanel);
        // We must remove event listener when component is destroyed.
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('variants:group:quality:started', updating);
            dispatcher.removeListener('variants:group:quality:finished', reloadPanel);
            dispatcher.removeListener('refresh:project:info', reloadPanel);
        });
        this.constants = ngbVariantQualityDiagramConstants;
        this.INIT();
    }

    INIT() {
        this.noDataToDisplay = !this.projectContext.variantsDataByQuality ||
            this.projectContext.variantsDataByQuality.length === 0;
        if (this.projectContext.reference && this.projectContext.variantsDataByQuality) {
            this.updateDiagram(this.projectContext.variantsDataByQuality,
                this.projectContext.isVariantsGroupByQualityLoading,
                this.projectContext.variantsGroupByQualityError);
            this.isProgressShown = this.projectContext.isVariantsGroupByQualityLoading;
        }
    }

    buildData(variantQualities) {
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
        return sampleData;
    }
}
