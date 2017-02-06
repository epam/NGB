export default {
    template: `<!--<div class="title-dot" ng-if="$ctrl.reference"></div>-->
    <!--<h3 class="md-title project-title" ng-if="$ctrl.project">{{$ctrl.project.name}}</h3>-->
    <h3 class="md-subhead page-title" ng-if="$ctrl.reference">{{$ctrl.reference.name}}</h3>`,
    /* @ngInject */
    controller: function ($scope, $timeout, projectDataService, dispatcher, projectContext) {
        const self = this;
        const callback = function() {
            self.reference = projectContext.reference;
            $timeout(::$scope.$apply);
        };

        dispatcher.on('reference:change', callback);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('reference:change', callback);
        });
        callback();
    }
};
