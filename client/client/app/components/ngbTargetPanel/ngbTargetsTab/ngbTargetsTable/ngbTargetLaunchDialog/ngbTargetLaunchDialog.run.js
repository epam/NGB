import {
    getGenes,
    createFilterFor,
} from '../utilities/autocompleteFunctions';

const TYPE = {
    PARASITE: 'PARASITE',
    DEFAULT: 'DEFAULT'
};

export default function run(
    $mdDialog,
    $timeout,
    dispatcher,
    ngbTargetsTabService,
    ngbTargetPanelService,
    targetContext,
    targetDataService
) {
    const displayLaunchDialog = async (target, selectedGene = null) => {
        $mdDialog.show({
            template: require('./ngbTargetLaunchDialog.tpl.html'),
            controller: function ($scope) {
                $scope.target = target;
                $scope.name = target.name;
                $scope.parasite = TYPE.PARASITE;

                $scope.genesOfInterest = [];
                $scope.translationalGenes = [];
                $scope.genes = getGenes(target.species.value, $scope.target.type, $scope.genesOfInterest, $scope.translationalGenes);

                $scope.genesOfInterestChanged = (item) => {
                    if (item) {
                        if (item.group) {
                            const items = getGroupItems(item);
                            for (let i = 0; i < items.length; i++) {
                                $scope.genesOfInterest.push(items[i]);
                            }
                            $timeout(() => {
                                const index = $scope.genesOfInterest.indexOf(item);
                                if (index !== -1) {
                                    $scope.genesOfInterest.splice(index, 1);
                                }
                            });
                        } else {
                            $scope.genesOfInterest.push(item);
                        }
                    }
                };

                if (selectedGene) {
                    if (target.type === TYPE.DEFAULT) {
                        const gene = $scope.genes
                            .filter(g => (
                                !g.group &&
                                g.geneId === selectedGene.geneId
                            ))[0];
                        $scope.genesOfInterestChanged(gene);
                    }
                    if (target.type === TYPE.PARASITE) {
                        const gene = $scope.genes.filter(g => g.targetGeneId === selectedGene.targetGeneId)[0];
                        $scope.genesOfInterestChanged(gene);
                    }
                }
                $scope.identifyDisabled = () => (
                    !$scope.genesOfInterest.length
                );

                function getGroupItems (item) {
                    const { genesOfInterest = [], translationalGenes = [] } = $scope;
                    return $scope.genes
                        .filter(s => (s.item
                            && s.speciesName === item.speciesName
                            && !genesOfInterest.includes(s)
                            && !translationalGenes.includes(s)));
                }

                function filterGroupHead() {
                    return (gene) => {
                        if (gene.group) {
                            const items = getGroupItems(gene);
                            return items.length;
                        }
                        return true;
                    };
                }

                $scope.getFilteredGenes = (text) => {
                    const genes = $scope.genes
                        .filter(s => !$scope.genesOfInterest.includes(s))
                        .filter(s => !$scope.translationalGenes.includes(s))
                        .filter(filterGroupHead());
                    if (!text) return genes;
                    return genes.filter(createFilterFor(text));
                }

                $scope.translationalGenesChanged = (item) => {
                    if (item) {
                        if (item.group) {
                            const items = getGroupItems(item);
                            for (let i = 0; i < items.length; i++) {
                                $scope.translationalGenes.push(items[i]);
                            }
                            $timeout(() => {
                                const index = $scope.translationalGenes.indexOf(item);
                                if (index !== -1) {
                                    $scope.translationalGenes.splice(index, 1);
                                }
                            });
                        } else {
                            $scope.translationalGenes.push(item);
                        }
                    }
                };

                async function getIdentificationData(scope) {
                    const params = {
                        targetId: target.id,
                        genesOfInterest: scope.genesOfInterest.map(s => s.geneId),
                        translationalGenes: scope.translationalGenes.map(s => s.geneId)
                    };
                    const info = {
                        target: target,
                        interest: scope.genesOfInterest,
                        translational: scope.translationalGenes
                    };
                    const result = await ngbTargetsTabService.getIdentificationData(params, info);
                    if (result) {
                        dispatcher.emit('target:show:identification:tab');
                        targetContext.setCurrentIdentification(target, scope);
                    }
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

                $scope.setTargetGenes = (genes) => {
                    $scope.genes = getGenes(genes, $scope.target.type, $scope.genesOfInterest, $scope.translationalGenes);
                }

                $scope.onLoadTargetGenes = (request) => {
                    const id = $scope.target.id;
                    return new Promise(resolve => {
                        targetDataService.getTargetGenesById(id, request)
                            .then(({items, totalCount}) => {
                                if (items && totalCount) {
                                    $scope.target.genesTotal = totalCount;
                                    $scope.setTargetGenes(items);
                                } else {
                                    $scope.target.genesTotal = 0;
                                    $scope.setTargetGenes([]);
                                }
                                resolve(true)
                            })
                            .catch(err => {
                                resolve(false);
                            });
                    });
                }

                $scope.close = () => {
                    $mdDialog.hide();
                };
            },
            clickOutsideToClose: true
        });
    };
    dispatcher.on('target:launch:identification', displayLaunchDialog);
}
