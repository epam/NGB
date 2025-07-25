import nvd3ChartController from '../nvd3-chart-controller';
import d3 from 'd3';

const Math = window.Math;
const MINIMUM_BARS_TO_SHOW = 20;

export default class ngbVariantDensityDiagramController extends nvd3ChartController{

    static get UID() {
        return 'ngbVariantDensityDiagramController';
    }

    projectContext;
    constructor($scope, $timeout, $element, dispatcher, projectContext, nvd3resizer, nvd3dataCorrection) {
        super($scope, $element, nvd3resizer, nvd3dataCorrection, MINIMUM_BARS_TO_SHOW);
        this.isProgressShown = true;
        this.projectContext = projectContext;
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
                        if (d.indexOf('fake') !== -1) return;
                        let tickValue = d;
                        if (!tickValue.toLowerCase().startsWith('chr') && tickValue.length < 10) {
                            tickValue = `chr${tickValue}`;
                        } else if (tickValue.length > 10) {
                            tickValue = tickValue.slice(0, 3) + '...' + tickValue.slice(-3);
                        }
                        return tickValue;
                    }
                },
                y: (d) => d.value,
                noData: 'No Data Available',
                callback: function() {
                    setTimeout(function () {
                        d3.selectAll('.ngb-nvd3-density .nv-x .tick').each(function () {
                            const tick = d3.select(this);
                            const [v] = tick.data();
                            tick
                                .append('title')
                                .text(String(v));
                        });
                    }, 2000);
                }
            },
            title: {
                enable: true,
                text: 'Variants by chromosome'
            }
        };
        const reloadPanel = this.INIT.bind(this);
        const updating = () => this.isProgressShown = true;
        dispatcher.on('variants:group:chromosome:started', updating);
        dispatcher.on('variants:group:chromosome:finished', reloadPanel);
        dispatcher.on('refresh:project:info', reloadPanel);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('variants:group:chromosome:started', updating);
            dispatcher.removeListener('variants:group:chromosome:finished', reloadPanel);
            dispatcher.removeListener('refresh:project:info', reloadPanel);
        });
        this.INIT();
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

    INIT() {
        this.noDataToDisplay = !this.variants || this.variants.length === 0;
        if (this.projectContext.reference && this.variants) {
            this.updateDiagram(
                this.variants,
                this.isVariantsGroupByChromosomesLoading,
                this.variantsGroupByChromosomesError
            );
            this.isProgressShown = this.isVariantsGroupByChromosomesLoading;
        }
    }

    buildData(data) {
        const maximumChromosomesCount = 30;
        const values = [];
        for (let i = 0; i < Math.min(data.length, maximumChromosomesCount); i++) {
            const {entriesCount, groupName} = data[i];
            values.push({chrName: groupName, label: groupName, value: entriesCount});
        }
        values.sort((a, b) => {
            const aChromosomeNumber = parseInt(a.label, 10);
            const bChromosomeNumber = parseInt(b.label, 10);
            const aNumber = Number.isNaN(Number(aChromosomeNumber)) ? Infinity : Number(aChromosomeNumber);
            const bNumber = Number.isNaN(Number(bChromosomeNumber)) ? Infinity : Number(bChromosomeNumber);
            if (aNumber === bNumber) {
                return a.label.localeCompare(b.label);
            }
            return aNumber - bNumber;
        });
        return values;
    }
}
