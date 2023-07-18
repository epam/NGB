export default {
    restrict: 'E',
    bindings: {
        count: '<',
        text: '<'
    },
    controllerAs: '$ctrl',
    template: require('./ngbPluralText.tpl.html'),
    controller: function ($scope) {
        const build = () => {
            if ($scope.$ctrl) {
                this.result = $scope.$ctrl.count === 1 || $scope.$ctrl.count === undefined
                    ? $scope.$ctrl.text
                    : `${$scope.$ctrl.text}s`;
            }
        };
        build();
        $scope.$watch('$ctrl.count', build);
        $scope.$watch('$ctrl.text', build);
    }
};
