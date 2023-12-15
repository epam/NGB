import angular from 'angular';

import './ngbLaunchMenuInput.scss';

const ngbLaunchMenuInput = angular.module('ngbLaunchMenuInput', []);

ngbLaunchMenuInput.directive('ngbLaunchMenuInput', function() {
    return {
        restrict: 'E',
        scope: {
          label: '@',
          selectedGenes: '=',
          getGenesList: '=',
          onChangeGene: '=',
        },
        template: require('./ngbLaunchMenuInput.tpl.html'),
        controller: function($scope, $element, $timeout, $mdMenu) {
            $scope.input = $element[0].getElementsByClassName('launch-input')[0];
            $scope.inputModel = '';

            $scope.onChange = (text) => {
                $scope.inputModel = text;
                $scope.listElements = $scope.getGenesList(text);
            }

            $scope.onClickItem = (gene) => {
                $scope.onChangeGene(gene);
                $scope.inputModel = '';
                $scope.input.value = '';
                $timeout(() => $scope.$apply());
            }

            $scope.openMenu = (mdOpenMenu, event) => {
                mdOpenMenu(event);
                $scope.listElements = $scope.getGenesList($scope.inputModel);
            }

            $scope.onRemoveClicked = (gene) => {
                const index = $scope.selectedGenes.indexOf(gene);
                if (index !== -1) {
                    $scope.selectedGenes.splice(index, 1);
                }
            }

            $scope.onBackspace = () => {
                if ($scope.inputModel.length) return;
                $mdMenu.cancel()
                if (!$scope.selectedGenes.length) return;
                if ($scope.removedGeneIndex === undefined) {
                    $scope.removedGeneIndex = $scope.selectedGenes.length - 1;
                } else {
                    $scope.onRemoveClicked($scope.selectedGenes[$scope.removedGeneIndex])
                    $scope.removedGeneIndex = undefined;
                }
            }

            $scope.onBlur = () => {
                $scope.removedGeneIndex = undefined;
            }

            $scope.onKeyPress = (event) => {
                switch ((event.code || '').toLowerCase()) {
                    case 'enter':
                        $scope.removedGeneIndex = undefined;
                        break;
                    case 'backspace':
                        $scope.onBackspace();
                        break;
                    default:
                        $scope.removedGeneIndex = undefined;
                        break;
                }
            }
        }
    };
});

export default ngbLaunchMenuInput.name;
