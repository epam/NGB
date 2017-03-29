import angular from 'angular';
export default class ngbVariantTypeDiagramController {

    static get UID() {
        return 'ngbVariantTypeDiagramController';
    }

    projectContext;
    _dispatcher;

    /**
     * @constructor
     * @param {$scope} scope
     * @param {projectDataService} dataService
     * @param {dispatcher} dispatcher
     */
    /** @ngInject */
    constructor($scope, dispatcher, projectContext) {
        this.isProgressShown = true;
        this.projectContext = projectContext;
        const __dispatcher = this._dispatcher = dispatcher;
        this._scope = $scope;

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
                y: (d)=> d.value
            },
            title: {
                enable: true,
                text: 'Variants types'
            }
        };



        const reloadPanel = ::this.INIT;
        const updating = async() => {
            this.isProgressShown = true;
        };
        this._dispatcher.on('variants:group:type:started', updating);
        this._dispatcher.on('variants:group:type:finished', reloadPanel);
        // We must remove event listener when component is destroyed.
        $scope.$on('$destroy', () => {
            __dispatcher.removeListener('variants:group:type:started', updating);
            __dispatcher.removeListener('variants:group:type:finished', reloadPanel);
        });

        (async() => {
            await this.INIT();
        })();

        angular.element(window).on('resize', () => {
            this._scope.api && angular.isFunction(this._scope.api.update) ? this._scope.api.update() : '';
        });

    }

    fixNvD3ChartObject(nvd3Object) {
        const obj = nvd3Object,
            l = nvd3Object.values.length;

        if(l <= 6) {
            const arr_w = (6 - l)%2 === 0 ? 6 - l : 7 - l;
            for (let i = 0; i < arr_w; i++) {
                const fakeObj = {color : '#ffffff', label: `fake${i}`, value: 0};
                i%2 === 0 ? obj.values.unshift(fakeObj) : obj.values.push(fakeObj);
            }
        }
        return obj;
    }

    makeNvD3ChartObjectFromData(data) {
        const nvd3DataObject = [],
            nvd3DataObjectItem = {
                values: []
            },
            typeColors = {
                'BND': '#fff9c4',
                'DEL': '#c9d6f0',
                'DUP': '#f48fb1',
                'INS': '#f3ceb6',
                'INV': '#dce775',
                'SNV': '#d8efdd',
                'UNK': '#ECECEC'
            };

        this.sampleData = [];

        let maxValue = 0;

        for (let i = 0; i < data.length; i++) {
            const {entriesCount, groupName} = data[i];
            const varType = groupName.toUpperCase();
            const bgColor = typeColors[`${varType}`];
            this.sampleData.push({color : bgColor, label: varType, value: entriesCount});
            if (maxValue < entriesCount) {
                maxValue = entriesCount;
            }
        }

        nvd3DataObjectItem.values = this.sampleData;

        const nvd3DataObjectItemFix = this.fixNvD3ChartObject(nvd3DataObjectItem);

        /*
        const fakeObj = {color : '#ffffff', label: 'fake_01', value: 0};
        const fakeObj2 = {color : '#ffffff', label: 'fake_02', value: 0};
        nvd3DataObjectItem.values.push(fakeObj);
        nvd3DataObjectItem.values.push(fakeObj2);
        */


        nvd3DataObject.push(nvd3DataObjectItemFix);

        return nvd3DataObject;

    }

    async INIT() {
        this.noDataToDisplay = !this.projectContext.variantsDataByType ||
            this.projectContext.variantsDataByType.length === 0;
        if (this.projectContext.reference && this.projectContext.variantsDataByType) {
            await this.updateDiagram(this.projectContext.variantsDataByType,
                this.projectContext.isVariantsGroupByTypeLoading);
            this.isProgressShown = this.projectContext.isVariantsGroupByTypeLoading;
            this._scope.$apply();
        }
    }

    async updateDiagram(variants, isLoading) {
        if (isLoading) {
            return;
        }
        (!variants || variants.length === 0) ? this._scope.data = [] : this._scope.data = this.makeNvD3ChartObjectFromData(variants);
        this._scope.api && angular.isFunction(this._scope.api.update) ? this._scope.api.update() : '';		
    }
}