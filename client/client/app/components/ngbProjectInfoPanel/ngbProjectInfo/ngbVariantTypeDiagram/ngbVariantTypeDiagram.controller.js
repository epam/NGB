import nvd3ChartController from '../nvd3-chart-controller';

const MINIMUM_BARS_TO_SHOW = 6;
const UNKNOWN_COLOR = '#ECECEC';
const VARIANT_TYPE_COLORS = {
    BND: '#fff9c4',
    DEL: '#c9d6f0',
    DUP: '#f48fb1',
    INS: '#f3ceb6',
    INV: '#dce775',
    SNV: '#d8efdd',
    UNK: UNKNOWN_COLOR
};

export default class ngbVariantTypeDiagramController extends nvd3ChartController {
    static get UID() {
        return 'ngbVariantTypeDiagramController';
    }

    projectContext;

    constructor($scope, $timeout, $element, dispatcher, projectContext, nvd3resizer, nvd3dataCorrection) {
        super($scope, $element, nvd3resizer, nvd3dataCorrection, MINIMUM_BARS_TO_SHOW);
        this.isProgressShown = true;
        this.projectContext = projectContext;
        $scope.options = {
            chart: {
                duration: 500,
                discretebar: {
                    dispatch: {
                        elementClick: (e) => {
                            this.projectContext.vcfFilter.selectedVcfTypes = [e.data.label];
                            this.projectContext.filterVariants();
                        }
                    }
                },
                margin: {
                    left: 0,
                    right: 0
                },
                showValues: true,
                showYAxis: false,
                tooltip: {
                    enabled: false
                },
                type: 'discreteBarChart',
                valueFormat: (d) => {
                    if(d === 0) return;
                    return d;
                },
                x: (d) => d.label,
                xAxis: {
                    axisLabel: '',
                    tickFormat : (d) =>  {
                        if(d.indexOf('fake') !== -1) return;
                        return d;
                    }
                },
                y: (d)=> d.value,
                noData: 'No Data Available'
            },
            title: {
                enable: true,
                text: 'Variants types'
            }
        };
        const reloadPanel = this.INIT.bind(this);
        const updating = () => this.isProgressShown = true;
        dispatcher.on('variants:group:type:started', updating);
        dispatcher.on('variants:group:type:finished', reloadPanel);
        dispatcher.on('refresh:project:info', reloadPanel);
        // We must remove event listener when component is destroyed.
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('variants:group:type:started', updating);
            dispatcher.removeListener('variants:group:type:finished', reloadPanel);
            dispatcher.removeListener('refresh:project:info', reloadPanel);
        });
        this.INIT();
    }

    buildData(data = []) {
        const sampleData = [];
        let maxValue = 0;
        for (let i = 0; i < data.length; i++) {
            const {entriesCount, groupName} = data[i];
            const varType = groupName.toUpperCase();
            const bgColor = VARIANT_TYPE_COLORS[`${varType}`];
            sampleData.push({
                color : bgColor || UNKNOWN_COLOR,
                label: varType,
                value: entriesCount
            });
            if (maxValue < entriesCount) {
                maxValue = entriesCount;
            }
        }
        return sampleData;

    }

    INIT() {
        this.noDataToDisplay = !this.projectContext.variantsDataByType ||
            this.projectContext.variantsDataByType.length === 0;
        if (this.projectContext.reference && this.projectContext.variantsDataByType) {
            this.updateDiagram(this.projectContext.variantsDataByType,
                this.projectContext.isVariantsGroupByTypeLoading,
                this.projectContext.variantsGroupByTypeError);
            this.isProgressShown = this.projectContext.isVariantsGroupByTypeLoading;
        }
    }
}
