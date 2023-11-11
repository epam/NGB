import {
    groupedBySpecies,
    createFilterFor,
} from '../utilities/autocompleteFunctions';

const COLUMN_LIST = ['name', 'genes of interest', 'translational genes'];

export default function run(
    $mdDialog,
    $timeout,
    dispatcher,
    targetDataService,
    ngbTargetsTabService,
    ngbTargetPanelService,
    targetContext,
) {
    const displaySavedIdentificationsDialog = async (target) => {
        $mdDialog.show({
            template: require('./ngbTargetSavedIdentificationsDialog.tpl.html'),
            controller: function ($scope) {
                $scope.columnList = COLUMN_LIST;
                $scope.actionFailed = false;
                $scope.errorMessageList = null;
                $scope.isChanged = false;
                $scope.name = target.name;
                $scope.genes = groupedBySpecies(target.species.value);
                $scope.searchText = null;
                
                $scope.identificationsModel = target.identifications.map(t => {
                    const {id, name, genesOfInterest, translationalGenes} = t;
                    const getGeneById = (id) => {
                        const genes = $scope.genes.filter(gene => !gene.group && gene.geneId === id);
                        return genes.length ? genes[0] : undefined;
                    };
                    return {
                        id,
                        name,
                        genesOfInterest: genesOfInterest
                            .map(geneId => getGeneById(geneId))
                            .filter(i => i),
                        translationalGenes: translationalGenes
                            .map(geneId => getGeneById(geneId))
                            .filter(i => i),
                        actionLoading: false,
                    }
                })

                function getGroupItems(item, model) {
                    const { genesOfInterest = [], translationalGenes = [] } = model;
                    return $scope.genes.filter(s => s.item
                        && s.speciesName === item.speciesName
                        && !genesOfInterest.includes(s)
                        && !translationalGenes.includes(s));
                }

                $scope.getFilteredGenes = (text, model) => {
                    const filterGroupHead = (model) => {
                        return (gene) => {
                            if (gene.group) {
                                const items = getGroupItems(gene, model);
                                return items.length;
                            }
                            return true;
                        };
                    };
                    const genes = $scope.genes
                        .filter(s => !model.genesOfInterest.includes(s))
                        .filter(s => !model.translationalGenes.includes(s))
                        .filter(filterGroupHead(model));
                    if (!text) return genes;
                    return genes.filter(createFilterFor(text));
                }

                $scope.genesOfInterestChanged = (item, model) => {
                    if (item && item.group) {
                        const items = getGroupItems(item, model);
                        for (let i = 0; i < items.length; i++) {
                            model.genesOfInterest.push(items[i]);
                        }
                        $timeout(() => {
                            const i = model.genesOfInterest.indexOf(item);
                            if (i) {
                                model.genesOfInterest.splice(i, 1);
                            }
                        });
                    }
                    document.activeElement.blur();
                };

                $scope.translationalGenesChanged = (item, model) => {
                    if (item && item.group) {
                        const items = getGroupItems(item, model);
                        for (let i = 0; i < items.length; i++) {
                            model.translationalGenes.push(items[i]);
                        }
                        $timeout(() => {
                            const index = model.translationalGenes.indexOf(item);
                            if (index) {
                                model.translationalGenes.splice(index, 1);
                            }
                        });
                    }
                    document.activeElement.blur();
                };

                $scope.onClickSave = (index) => {}

                function deleteIdentification(id) {
                    return new Promise((resolve) => {
                        targetDataService.deleteIdentification(id)
                            .then(() => {
                                $scope.errorMessageList = null;
                                $scope.actionFailed = false;
                                resolve(true);
                            })
                            .catch(error => {
                                $scope.errorMessageList = [error.message];
                                $scope.actionFailed = true;
                                resolve(false);
                            });
                    });
                }

                $scope.onClickDelete = async (index) => {
                    const id = $scope.identificationsModel[index].id;
                    if (!id) return;
                    $scope.identificationsModel[index].actionLoading = true;
                    const isDeleted = await deleteIdentification(id);
                    if (isDeleted) {
                        $scope.identificationsModel = $scope.identificationsModel
                            .filter((item, i) => i !== index);
                        if (!$scope.identificationsModel.length) {
                            $scope.close();
                        }
                        $scope.isChanged = true;
                        $timeout(() => $scope.$apply());
                    }
                }

                async function launchIdentification(model) {
                    const params = {
                        targetId: target.id,
                        genesOfInterest: model.genesOfInterest.map(s => s.geneId),
                        translationalGenes: model.translationalGenes.map(s => s.geneId)
                    };
                    const info = {
                        target: target,
                        interest: model.genesOfInterest,
                        translational: model.translationalGenes
                    };
                    const result = await ngbTargetsTabService.getIdentificationData(params, info);
                    if (result) {
                        dispatcher.emit('target:show:identification:tab');
                        targetContext.setCurrentIdentification(target, model);
                    }
                }

                function isIdentificationLaunched(identificationTarget, model) {
                    if (identificationTarget.target.id !== target.id) return false;
                        const interest = identificationTarget.interest.map(g => g.geneId).sort();
                        const translational = identificationTarget.translational.map(g => g.geneId).sort();
                        const genesOfInterest = model.genesOfInterest.map(g => g.geneId).sort();
                        const translationalGenes = model.translationalGenes.map(g => g.geneId).sort();
                        const isEqual = (current, saved) => {
                            if (current.length !== saved.length) return false;
                            return saved.every((item, index) => item === current[index]);
                        }
                        return isEqual(genesOfInterest, interest)
                            && isEqual(translationalGenes, translational);
                }

                function showConfirmDialog(model) {
                    $mdDialog.show({
                        template: require('../ngbTargetLaunchDialog/ngbTargetLaunchConfirmDialog.tpl.html'),
                        controller: function($scope, $mdDialog) {
                            $scope.launch = function () {
                                launchIdentification(model);
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
                }

                $scope.onClickLaunch = (index) => {
                    const {identificationData, identificationTarget} = ngbTargetPanelService;
                    const model = $scope.identificationsModel[index];
                    if (identificationData && identificationTarget) {
                        const isLaunched = isIdentificationLaunched(identificationTarget, model);
                        if (isLaunched) {
                            dispatcher.emit('target:show:identification:tab');
                            $mdDialog.hide();
                        } else {
                            showConfirmDialog(model);
                        }
                    } else {
                        launchIdentification(model);
                        $mdDialog.hide();
                    }
                }

                $scope.close = () => {
                    $mdDialog.hide();
                    if ($scope.isChanged) {
                        dispatcher.emit('target:table:update');
                    }
                };
            },
            clickOutsideToClose: false
        });
    };
    dispatcher.on('target:show:saved:identifications', displaySavedIdentificationsDialog);
}
