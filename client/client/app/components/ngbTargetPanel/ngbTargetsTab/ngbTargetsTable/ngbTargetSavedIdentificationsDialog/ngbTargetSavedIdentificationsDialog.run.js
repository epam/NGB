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
                $scope.isLaunched = false;
                $scope.name = target.name;

                $scope.identifications = target.identifications.map(t => {
                    const {id, name, genesOfInterest, translationalGenes} = t;
                    const getGenesById = (ids) => {
                        return ids
                            .reduce((acc, id) => {
                                const genesWithId = target.species.value.filter(s => s.geneId === id);
                                if (!genesWithId.length) return;
                                if (genesWithId.length === 1) {
                                    acc.push(genesWithId[0]);
                                } else {
                                    const notIncluded = genesWithId.filter(g => {
                                        return !acc.filter(a => (a.speciesName === g.speciesName
                                            && a.geneName === g.geneName)).length;
                                    });
                                    acc.push(notIncluded[0]);
                                }
                                return acc;
                            }, [])
                            .filter(i => i);
                    }
                    return {
                        id,
                        name,
                        genesOfInterest: getGenesById(genesOfInterest),
                        translationalGenes: getGenesById(translationalGenes),
                        deleteLoading: false,
                    }
                })

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
                    const id = $scope.identifications[index].id;
                    if (!id) return;
                    $scope.identifications[index].deleteLoading = true;
                    const isDeleted = await deleteIdentification(id);
                    if (isDeleted) {
                        const deletedIdentification = {
                            genesOfInterest: [...$scope.identifications[index].genesOfInterest],
                            translationalGenes: [...$scope.identifications[index].translationalGenes],
                        };
                        $scope.identifications = $scope.identifications
                            .filter((item, i) => i !== index);
                        $scope.isChanged = true;
                        const {identificationData, identificationTarget} = ngbTargetPanelService;
                        if (identificationData && identificationTarget) {
                            $scope.isLaunched = isIdentificationLaunched(identificationTarget, deletedIdentification);
                        }
                        if (!$scope.identifications.length) {
                            $scope.close();
                        }
                        $timeout(() => $scope.$apply());
                    }
                }

                async function launchIdentification(launchIdentification) {
                    const params = {
                        targetId: target.id,
                        genesOfInterest: launchIdentification.genesOfInterest.map(s => s.geneId),
                        translationalGenes: launchIdentification.translationalGenes.map(s => s.geneId)
                    };
                    const info = {
                        target: target,
                        interest: launchIdentification.genesOfInterest,
                        translational: launchIdentification.translationalGenes
                    };
                    const result = await ngbTargetsTabService.getIdentificationData(params, info);
                    if (result) {
                        dispatcher.emit('target:show:identification:tab');
                        targetContext.setCurrentIdentification(target, launchIdentification);
                    }
                }

                function isIdentificationLaunched(identificationTarget, identification) {
                    if (identificationTarget.target.id !== target.id) return false;
                        const interest = identificationTarget.interest.map(g => g.geneId).sort();
                        const translational = identificationTarget.translational.map(g => g.geneId).sort();
                        const genesOfInterest = identification.genesOfInterest.map(g => g.geneId).sort();
                        const translationalGenes = identification.translationalGenes.map(g => g.geneId).sort();
                        const isEqual = (current, saved) => {
                            if (current.length !== saved.length) return false;
                            return saved.every((item, index) => item === current[index]);
                        }
                        return isEqual(genesOfInterest, interest)
                            && isEqual(translationalGenes, translational);
                }

                function showConfirmDialog(identification) {
                    $mdDialog.show({
                        template: require('../ngbTargetLaunchDialog/ngbTargetLaunchConfirmDialog.tpl.html'),
                        controller: function($scope, $mdDialog) {
                            $scope.launch = function () {
                                launchIdentification(identification);
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
                    const identification = $scope.identifications[index];
                    if (identificationData && identificationTarget) {
                        const isLaunched = isIdentificationLaunched(identificationTarget, identification);
                        if (isLaunched) {
                            dispatcher.emit('target:show:identification:tab');
                            $mdDialog.hide();
                        } else {
                            showConfirmDialog(identification);
                        }
                    } else {
                        launchIdentification(identification);
                        $mdDialog.hide();
                    }
                }

                $scope.close = () => {
                    $mdDialog.hide();
                    if ($scope.isChanged) {
                        dispatcher.emit('target:table:update');
                    }
                    if ($scope.isLaunched) {
                        dispatcher.emit('target:identification:status:update');
                    }
                };
            },
            clickOutsideToClose: false
        });
    };
    dispatcher.on('target:show:saved:identifications', displaySavedIdentificationsDialog);
}
