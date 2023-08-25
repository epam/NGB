export default function run($mdDialog, $timeout, dispatcher, ngbTargetsTabService, ngbTargetPanelService) {
    const displayLaunchDialog = async (target) => {
        const groupedBySpecies = (species) => {
            const groups = species.reduce((acc, curr) => {
                if (!acc[curr.speciesName]) {
                    acc[curr.speciesName] = {
                        count: 1,
                        value: [curr]
                    };
                } else {
                    acc[curr.speciesName].count += 1;
                    acc[curr.speciesName].value.push(curr);
                }
                return acc;
            }, {});
            const grouped = Object.values(groups).reduce((acc, curr) => {
                const getItem = (item) => ({
                    speciesName: item.speciesName,
                    geneId: item.geneId,
                    geneName: item.geneName,
                    taxId: item.taxId,
                });
                if (curr.count === 1) {
                    const group = curr.value[0];
                    acc.push({
                        group: false,
                        item: false,
                        span: `${group.geneName} (${group.speciesName})`,
                        chip: `${group.geneName} (${group.speciesName})`,
                        hidden: `${group.geneName} ${group.speciesName}`,
                        ...getItem(group)
                    });
                }
                if (curr.count > 1) {
                    const sumChip = curr.value.map(g => g.geneName);
                    const head = curr.value[0];
                    acc.push({
                        group: true,
                        item: false,
                        span: `${head.speciesName}`,
                        hidden: `${sumChip.join(' ')} ${head.speciesName}`,
                        ...getItem(head)
                    });
                    acc = [...acc, ...curr.value.map(group => ({
                        group: false,
                        item: true,
                        span: `${group.geneName}`,
                        chip: `${group.geneName} (${group.speciesName})`,
                        hidden: `${group.geneName} ${group.speciesName}`,
                        ...getItem(group)
                    }))];
                }
                return acc;
            }, []);
            return grouped;
        };
        $mdDialog.show({
            template: require('./ngbTargetLaunchDialog.tpl.html'),
            controller: function ($scope) {
                $scope.name = target.name;
                $scope.genes = groupedBySpecies(target.species.value);
                $scope.genesOfInterest = [];
                $scope.translationalGenes = [];
                $scope.searchText = null;

                $scope.identifyDisabled = () => (
                    !$scope.genesOfInterest.length || !$scope.translationalGenes.length
                );

                function createFilterFor(text) {
                    return (gene) => gene.hidden.toLowerCase().includes(text.toLowerCase());
                }

                function filterGroupHead(list) {
                    return (gene) => {
                        if (gene.group) {
                            const items = getGroupItems(gene, list);
                            return items.length;
                        }
                        return true;
                    };
                }
                
                $scope.getGenesOfInterest = (text) => {
                    const genes = $scope.genes
                        .filter(s => !$scope.genesOfInterest.includes(s))
                        .filter(filterGroupHead($scope.genesOfInterest));
                    if (!text) return genes;
                    return genes.filter(createFilterFor(text));
                }

                $scope.getTranslationalGenes = (text) => {
                    const genes = $scope.genes
                        .filter(s => !$scope.translationalGenes.includes(s))
                        .filter(filterGroupHead($scope.translationalGenes));
                    if (!text) return genes;
                    return genes.filter(createFilterFor(text));
                }

                function getGroupItems (item, list) {
                    return $scope.genes
                        .filter(s => s.item
                            && s.speciesName === item.speciesName
                            && !list.includes(s));
                }

                $scope.genesOfInterestChanged = (item) => {
                    if (item && item.group) {
                        const items = getGroupItems(item, $scope.genesOfInterest);
                        for (let i = 0; i < items.length; i++) {
                            $scope.genesOfInterest.push(items[i]);
                        }
                        $timeout(() => {
                            const index = $scope.genesOfInterest.indexOf(item);
                            if (index) {
                                $scope.genesOfInterest.splice(index, 1);
                            }
                        });
                    }
                };

                $scope.translationalGenesChanged = (item) => {
                    if (item && item.group) {
                        const items = getGroupItems(item, $scope.translationalGenes);
                        for (let i = 0; i < items.length; i++) {
                            $scope.translationalGenes.push(items[i]);
                        }
                        $timeout(() => {
                            const index = $scope.translationalGenes.indexOf(item);
                            if (index) {
                                $scope.translationalGenes.splice(index, 1);
                            }
                        });
                    }
                };

                function getIdentificationData(scope) {
                    const params = {
                        targetId: target.id,
                        genesOfInterest:  scope.genesOfInterest.map(s => s.geneId),
                        translationalGenes: scope.translationalGenes.map(s => s.geneId)
                    };
                    const info = {
                        target: target,
                        interest:  scope.genesOfInterest,
                        translational: scope.translationalGenes
                    };
                    ngbTargetsTabService.getIdentificationData(params, info);
                }

                $scope.identify = () => {
                    if (ngbTargetPanelService.identificationData && ngbTargetPanelService.identificationTarget) {
                        const self = $scope;
                        $mdDialog.show({
                            template: require('./ngbTargetLaunchConfirmDialog.tpl.html'),
                            controller: function($scope, $mdDialog) {
                                $scope.launch = function () {
                                    getIdentificationData(self);
                                    $mdDialog.hide();
                                    $mdDialog.hide();
                                };
                                $scope.cancel = function () {
                                    $mdDialog.hide();
                                };
                            },
                            preserveScope: true,
                            autoWrap: true,
                            skipHide: true,
                        });
                        return;
                    }
                    getIdentificationData($scope);
                    $mdDialog.hide();
                };

                $scope.close = () => {
                    $mdDialog.hide();
                };
                $scope.change = () => {
                    $scope.inReference = !$scope.inReference;
                };
            },
            clickOutsideToClose: true
        });
    };
    dispatcher.on('target:launch:identification', displayLaunchDialog);
}
