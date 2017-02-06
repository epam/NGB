import angular from 'angular';

export default class ngbVariantDensityDiagramController {

    static get UID() {
        return 'ngbVariantDensityDiagramController';
    }

    projectContext;

    /**
     * @constructor
     * @param {$scope} scope
     * @param {dispatcher} dispatcher
     */
    /** @ngInject */
    constructor($scope, dispatcher, projectContext) {
        this.isProgressShown = true;
        const __dispatcher = this._dispatcher = dispatcher;
        this.projectContext = projectContext;
        this._scope = $scope;

        $scope.options = {
            chart: {
                color: ['#9cabe0'],
                discretebar: {
                    dispatch: {
                        elementClick: (e) => {
                            this.projectContext.changeState({chromosome: {name: e.data.chrName}});
                        }
                    }
                },
                duration: 500,
                margin: {
                    bottom: 70,
                    left: 0,
                    right: 0
                },
                showValues: true,
                showYAxis: false,
                staggerLabels : true,
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
                        let v = null;
                        if(d.indexOf('fake') !== -1) return;
                        d.indexOf('chr') === -1 ? v = `chr${  d}` : v = d;
                        return v;
                    }
                },
                y: (d) => d.value
            },
            title: {
                enable: true,
                text: 'Variants by chromosome'
            }
        };

        (async() => {

            const reloadPanel = ::this.INIT;
            const updating = async() => {
                this.isProgressShown = true;
            };
            this._dispatcher.on('variants:loading:started', updating);
            this._dispatcher.on('variants:loading:finished', reloadPanel);

            await this.INIT();

            $scope.$on('$destroy', () => {
                __dispatcher.removeListener('variants:loading:started', updating);
                __dispatcher.removeListener('variants:loading:finished', reloadPanel);
            });

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

    fixNvD3ChartObject(nvd3Object) {
        const obj = nvd3Object,
            l = nvd3Object.values.length;

        if(l <= 19) {
            const arr_w = (20 - l)%2 === 0 ? 20 - l : 21 - l;
            for (let i = 0; i < arr_w; i++) {
                const fakeObj = {chrId: '', chrName: `fake${i}`, label: `fake${i}`, value: 0, color: '#ffffff'};
                i%2 === 0 ? obj.values.unshift(fakeObj) : obj.values.push(fakeObj);
            }
        }
        return obj;
    }

    makeNvD3ChartObjectFromData(data) {
        const nvd3DataObject = [],
            nvd3DataObjectItem = {
                values: []
            };

        for (let i = 0; i < data.length; i++) {
            const variation = data[i];
            const chrName = variation.chromosome.name;
            const chrId = variation.chromosome.id;
            const idx = nvd3DataObjectItem.values.findIndex((element) =>
                element.label === chrName
            );
            if (idx === -1) {
                nvd3DataObjectItem.values.push({chrId: chrId, chrName: chrName, label: chrName, value: 1});
                continue;
            }
            ++nvd3DataObjectItem.values[idx].value;
        }
        nvd3DataObjectItem.values.sort((a, b) => {
            const aIsN = parseInt(a.label, 10);
            const bIsN = parseInt(b.label, 10);
            if (isNaN(aIsN)) {
                if (isNaN(bIsN)) {
                    return a.label > b.label ? 1 : -1;
                }
                else {
                    return 1;
                }
            }
            else if (isNaN(bIsN)) {
                return -1;
            } else {
                return aIsN - bIsN;
            }
        });

        const nvd3DataObjectItemFix = this.fixNvD3ChartObject(nvd3DataObjectItem);

        nvd3DataObject.push(nvd3DataObjectItemFix);

        return nvd3DataObject;
    }


    async updateDiagram(variants, isLoading) {
        if (isLoading) {
            return;
        }
        (!variants || variants.length === 0) ?
            this._scope.data = [] : this._scope.data = this.makeNvD3ChartObjectFromData(variants);
        this._scope.api && angular.isFunction(this._scope.api.update) ? this._scope.api.update() : '';
    }
}