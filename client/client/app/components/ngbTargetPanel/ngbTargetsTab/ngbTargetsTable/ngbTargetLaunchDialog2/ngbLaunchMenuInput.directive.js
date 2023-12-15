import angular from 'angular';

import './ngbLaunchMenuInput.scss';

const ngbLaunchMenuInput = angular.module('ngbLaunchMenuInput', []);

ngbLaunchMenuInput.directive('ngbLaunchMenuInput', function() {
    return {
        restrict: 'E',
        scope: {
          label: '@',
          ngModel: '=',
          selectedGenes: '=',
          getGenesList: '=',
          onChangeGene: '=',
        },
        template: require('./ngbLaunchMenuInput.tpl.html'),
        controller: function($scope, $element, $timeout) {
            $scope.listIsDisplayed = false;
            $scope.hideListTimeout = null;
            $scope._hideListIsPrevented = false;
            $scope.input = $element;

            $scope.preventListFromClosing = () => {
                $scope._hideListIsPrevented = true;
            }

            $scope.stopPreventListFromClosing = () => {
                $scope._hideListIsPrevented = false;
            }

            $scope.mousedown = () => {
                $scope.preventListFromClosing()
            }

            $scope.mouseup = () => {
                $scope.stopPreventListFromClosing()
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

            $scope.openMenu = (mdOpenMenu, event) => {
                mdOpenMenu(event);
                if ($scope.hideListTimeout) {
                    clearTimeout($scope.hideListTimeout);
                    $scope.hideListTimeout = null;
                }
                $scope.listElements = $scope.getGenesList();
                $scope.listIsDisplayed = true;
            }

            $scope.onChange = (text) => {
                $scope.listElements = $scope.getGenesList(text);
            }

            $scope.apply = () => {
                $scope.ngModel = '';
            }

            $scope.onClickItem = (gene) => {
                $timeout(() => $scope.hideList());
                $scope.onChangeGene(gene);
            }

            $scope.onRemoveClicked = (gene) => {
                const index = $scope.selectedGenes.indexOf(gene);
                if (index !== -1) {
                    $scope.selectedGenes.splice(index, 1);
                }
            }
        }
    };
});

export default ngbLaunchMenuInput.name;
