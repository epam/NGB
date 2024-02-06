import angular from 'angular';

import './ngbLaunchMenuInput.scss';

const PAGE_SIZE = 20;
const PARASITE = 'PARASITE';

const ngbLaunchMenuInput = angular.module('ngbLaunchMenuInput', []);

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
                $scope.currentPage = 1;
                $scope.firstPage = 1;
                $scope.lastPage = 1;
                mdOpenMenu(event);
                if ($scope.target.type === $scope.parasite) {
                    $scope.setScroll();
                    await $scope.onLoadGenesList()
                        .then((list) => {
                            $scope.listElements = list;
                            $timeout(() => $scope.$apply());
                        });
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

            $scope.onLoadGenesList = async () => {
                const request = {
                    page: $scope.currentPage,
                    pageSize: $scope.pageSize
                };
                $scope.loading = true;
                return new Promise(resolve => {
                    $scope.onLoadGenes(request)
                        .then(success => {
                            let list;
                            if (success) {
                                list = $scope.getGenesList($scope.inputModel);
                                $scope.totalPages = Math.ceil($scope.target.genesTotal/$scope.pageSize) || 0;
                            } else {
                                list = [];
                                $scope.totalPages = 0;
                            }
                            $scope.loading = false;
                            resolve(list);
                        });
                });
            }

            $scope.setScroll = () => {
                let requested = false;
                const pageDeep = 3;
                const itemHeight = 30;
                const maxSize = $scope.pageSize * pageDeep;
                let previousScroll = 0;
                angular.element(document.getElementById($scope.label))
                    .on('scroll', async (event) => {
                        const {clientHeight, scrollHeight} = event.currentTarget;
                        let {scrollTop} = event.currentTarget;
                        if (previousScroll < scrollTop) {
                            if (!requested && ($scope.listElements.length > maxSize)) {
                                requested = true;
                                const start = $scope.pageSize;
                                const end = (pageDeep + 1) * $scope.pageSize;
                                $scope.listElements = $scope.listElements.slice(start, end);
                                $scope.firstPage += 1;
                                $scope.$apply();
                                if ($scope.listElements.length === maxSize) {
                                    const newScollPosition = scrollTop - ($scope.pageSize * itemHeight);
                                    event.currentTarget.scrollTo(0, newScollPosition);
                                    scrollTop = newScollPosition;
                                    previousScroll = newScollPosition;
                                }
                                $timeout(() => {
                                    requested = false;
                                });
                            }
                            if (!requested &&
                                $scope.lastPage < $scope.totalPages &&
                                (scrollTop + (clientHeight * 2) >= scrollHeight)
                            ) {
                                requested = true;
                                $scope.lastPage += 1;
                                $scope.currentPage = $scope.lastPage;
                                await $scope.onLoadGenesList()
                                    .then((list) => {
                                        $scope.listElements = $scope.listElements.concat(list);
                                        $timeout(() => {
                                            requested = false;
                                            $scope.$apply();
                                        });
                                    })
                            }
                        } else {
                            if (!requested && ($scope.listElements.length > maxSize)) {
                                requested = true;
                                const start = 0;
                                const end = pageDeep * $scope.pageSize;
                                $scope.listElements = $scope.listElements.slice(start, end);
                                $scope.lastPage -= 1;
                                $scope.$apply();
                                if ($scope.listElements.length === maxSize) {
                                    const newScollPosition = scrollTop + ($scope.pageSize * itemHeight);
                                    event.currentTarget.scrollTo(0, newScollPosition);
                                    scrollTop = newScollPosition;
                                    previousScroll = newScollPosition;
                                }
                                $timeout(() => {
                                    requested = false;
                                });
                            }
                            if (!requested &&
                                $scope.currentPage > 1 &&
                                (scrollTop < clientHeight)
                            ) {
                                requested = true;
                                $scope.firstPage -= 1;
                                $scope.currentPage = $scope.firstPage;
                                await $scope.onLoadGenesList()
                                    .then((list) => {
                                        $scope.listElements = list.concat($scope.listElements);
                                        $timeout(() => {
                                            requested = false;
                                            $scope.$apply();
                                        });
                                    })
                            }
                        }
                        previousScroll = scrollTop;
                    })
            }
        }
    };
});

export default ngbLaunchMenuInput.name;
