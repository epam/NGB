import angular from 'angular';

import './ngbLaunchMenuInput.scss';

import ngbLaunchMenuPagination from '../ngbLaunchMenuPagination';

const PAGE_SIZE = 30;
const PARASITE = 'PARASITE';

const ngbLaunchMenuInput = angular.module('ngbLaunchMenuInput', [ngbLaunchMenuPagination]);

ngbLaunchMenuInput.directive('ngbLaunchMenuInput', function() {
    return {
        restrict: 'E',
        scope: {
          label: '@',
          selectedGenes: '=',
          getGenesList: '=',
          onChangeGene: '=',
          onLoadGenes: '=',
          target: '<',
        },
        template: require('./ngbLaunchMenuInput.tpl.html'),
        controller: function($scope, $element, $timeout, $mdMenu) {
            $scope.input = $element[0].getElementsByClassName('launch-input')[0];
            $scope.inputModel = '';
            $scope.parasite = PARASITE;

            $scope.loading = false;
            $scope.pageSize = PAGE_SIZE;
            $scope.currentPage = 1;

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

            $scope.openMenu = async (mdOpenMenu, event) => {
                mdOpenMenu(event);
                if ($scope.target.type === $scope.parasite) {
                    await $scope.onLoadGenesList();
                    $timeout(() => $scope.$apply());
                } else {
                    $scope.listElements = $scope.getGenesList($scope.inputModel);
                }
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

            $scope.onChangePage = async (page) => {
                $scope.currentPage = page;
                await $scope.onLoadGenesList()
                    .then(() => $scope.$apply());
            }

            $scope.onLoadGenesList = async () => {
                const request = {
                    page: $scope.currentPage,
                    pageSize: $scope.pageSize
                };
                $scope.loading = true;
                return new Promise(resolve => {
                    $scope.onLoadGenes(request)
                        .then(success => {
                            if (success) {
                                $scope.listElements = $scope.getGenesList($scope.inputModel);
                                $scope.totalPages = Math.ceil($scope.target.genesTotal/$scope.pageSize) || 0;
                            } else {
                                $scope.listElements = [];
                                $scope.totalPages = 0;
                            }
                            $scope.loading = false;
                            resolve(true);
                        });
                });
            }
        }
    };
});

export default ngbLaunchMenuInput.name;
