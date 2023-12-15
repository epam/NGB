import angular from 'angular';

import './ngbLaunchMenuInput.scss';

const ngbLaunchMenuInput = angular.module('ngbLaunchMenuInput', []);

ngbLaunchMenuInput.directive('ngbLaunchMenuInput', function() {
    return {
        restrict: 'E',
        scope: {
          label: '@',
        },
        template: require('./ngbLaunchMenuInput.tpl.html'),
        controller: function($scope) {}
    };
});

export default ngbLaunchMenuInput.name;
