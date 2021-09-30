export default class nvd3ChartController {
    constructor($scope, $element, nvd3resizer, nvd3dataCorrection, minimumItemsToShow = 0) {
        this.nvd3dataCorrection = nvd3dataCorrection;
        this.minimumItemsToShow = minimumItemsToShow;
        this.scope = $scope;
        $scope.data = [];
        const destroyResizer = nvd3resizer($element[0], $scope);
        $scope.$on('$destroy', () => {
            destroyResizer();
        });
    }

    // eslint-disable-next-line
    buildData (data) {
        return [];
    }

    updateDiagram(data, isLoading, error) {
        if (isLoading) {
            return;
        }
        if (!data || data.length === 0) {
            this.scope.options.chart.noData = error || 'No Data Available';
            this.scope.data = [];
        } else {
            this.scope.data = [{values: this.nvd3dataCorrection(this.buildData(data), this.minimumItemsToShow)}];
        }
        if (this.scope.api && typeof this.scope.api.updateWithData === 'function') {
            this.scope.api.updateWithData(this.scope.data);
        }
    }
}
