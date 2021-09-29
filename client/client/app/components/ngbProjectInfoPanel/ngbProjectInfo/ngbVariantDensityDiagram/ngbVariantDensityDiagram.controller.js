import angular from 'angular';

const Math = window.Math;

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
    constructor($scope, $timeout, dispatcher, projectContext) {
        this.isProgressShown = true;
        const __dispatcher = this._dispatcher = dispatcher;
        this.projectContext = projectContext;
        this._scope = $scope;
        this._timeout = $timeout;

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
                y: (d) => d.value,
                noData: 'No Data Available'
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
            this._dispatcher.on('variants:group:chromosome:started', updating);
            this._dispatcher.on('variants:group:chromosome:finished', reloadPanel);
            this._dispatcher.on('refresh:project:info', reloadPanel);

            await this.INIT();

            $scope.$on('$destroy', () => {
                __dispatcher.removeListener('variants:group:chromosome:started', updating);
                __dispatcher.removeListener('variants:group:chromosome:finished', reloadPanel);
                __dispatcher.removeListener('refresh:project:info', reloadPanel);
            });

            angular.element(window).on('resize', () => {
                this._scope.api && angular.isFunction(this._scope.api.update) ? this._scope.api.update() : '';
            });

        })();
    }

    get variants() {
        return this.projectContext.variantsDataByChromosomes;
    }

    get isVariantsGroupByChromosomesLoading() {
        return this.projectContext.isVariantsGroupByChromosomesLoading;
    }
    get variantsGroupByChromosomesError() {
        return this.projectContext.variantsGroupByChromosomesError;
    }

    async INIT() {
        this.noDataToDisplay = !this.variants || this.variants.length === 0;
        if (this.projectContext.reference && this.variants) {
            await this.updateDiagram(
                this.variants,
                this.isVariantsGroupByChromosomesLoading,
                this.variantsGroupByChromosomesError
            );
            this.isProgressShown = this.isVariantsGroupByChromosomesLoading;
            this._scope.$applyAsync();
        }
    }

    fixNvD3ChartObject(nvd3Object) {
        const obj = nvd3Object,
            l = nvd3Object.values.length;

        if(l <= 19) {
            const arr_w = (20 - l)%2 === 0 ? 20 - l : 21 - l;
            for (let i = 0; i < arr_w; i++) {
                const fakeObj = {chrName: `fake${i}`, label: `fake${i}`, value: 0, color: '#ffffff'};
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

        const maximumChromosomesCount = 30;

        for (let i = 0; i < Math.min(data.length, maximumChromosomesCount); i++) {
            const {entriesCount, groupName} = data[i];
            nvd3DataObjectItem.values.push({chrName: groupName, label: groupName, value: entriesCount});
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

    async updateDiagram(variants, isLoading, error) {
        if (isLoading) {
            return;
        }
        if (!variants || variants.length === 0) {
            this._scope.options.chart.noData = error || 'No Data Available';
            this._scope.data = [];
        } else {
            this._scope.data = this.makeNvD3ChartObjectFromData(variants);
        }
        this._scope.api && angular.isFunction(this._scope.api.update) ? this._scope.api.update() : '';
    }
}