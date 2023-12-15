import angular from 'angular';

import './ngbLaunchMenuInput.scss';

const ngbLaunchMenuInput = angular.module('ngbLaunchMenuInput', []);

ngbLaunchMenuInput.directive('ngbLaunchMenuInput', function() {
    return {
        restrict: 'E',
        scope: {
          label: '@',
          ngModel: '=',
          getGenesList: '=',
        },
        template: require('./ngbLaunchMenuInput.tpl.html'),
        controller: function($scope, $element) {
            $scope.listIsDisplayed = false;
            $scope.hideListTimeout = null;
            $scope._hideListIsPrevented = false;
            $scope.input = $element;

            $scope.preventListFromClosing = () => {
                $scope._hideListIsPrevented = true;
                $scope.input.focus();
            }

            $scope.stopPreventListFromClosing = () => {
                $scope._hideListIsPrevented = false;
            }

            $scope.hideListDelayed = () => {
                if ($scope.hideListTimeout) {
                    clearTimeout($scope.hideListTimeout);
                    $scope.hideListTimeout = null;
                }
                $scope.hideListTimeout = setTimeout(() => {
                    $scope.hideList();
                }, 100);
            }

            $scope.hideList = () => {
                if ($scope.hideListTimeout) {
                    clearTimeout($scope.hideListTimeout);
                    $scope.hideListTimeout = null;
                }
                if ($scope._hideListIsPrevented) {
                    return;
                }
                $scope.listIsDisplayed = false;
                $scope.apply();
                $scope.$apply();
            }

            $scope.displayList = () => {
                if ($scope.hideListTimeout) {
                    clearTimeout($scope.hideListTimeout);
                    $scope.hideListTimeout = null;
                }
                $scope.listElements = $scope.getGenesList();
                const input = $scope.input[0];
                const top = input.offsetTop + input.clientHeight - 8;
                $scope.listPosition = {
                    top: `${top}px`,
                    left: `${170}px`
                }
                $scope.listIsDisplayed = true;
            }

            $scope.onChange = (text) => {
                $scope.listElements = $scope.getGenesList(text);
            }

            $scope.apply = () => {
                $scope.ngModel = '';
            }
        }
    };
});

export default ngbLaunchMenuInput.name;
