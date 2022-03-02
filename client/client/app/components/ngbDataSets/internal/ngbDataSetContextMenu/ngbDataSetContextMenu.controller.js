import angular from 'angular';
import ngbPermissionsFormController from '../ngbPermissionsForm/ngbPermissionsForm.controller';
import ngbDataSetMetadataController from '../ngbDataSetMetadata/ngbDataSetMetadata.controller';

export default class NgbDataSetContextMenuController {

    node;
    projectElements = [];
    hasRole = false;

    constructor($scope, $mdDialog, ngbDataSetContextMenu, utilsDataService, userDataService, projectDataService, projectContext) {
        this.$scope = $scope;
        this.node = $scope.node;
        this.$mdDialog = $mdDialog;
        this.ngbDataSetContextMenu = ngbDataSetContextMenu;
        this.utilsDataService = utilsDataService;
        this.userDataService = userDataService;
        this.projectDataService = projectDataService;
        this.projectContext = projectContext;
        this.checkUserRole();
        if (this.node.isProject) {
            this.configureProjectElements();
        }
    }

    get resourceDownloadAvailable () {
        return !this.projectContext || this.projectContext.resourceDownloadAvailable;
    }

    checkUserRole() {
        this.utilsDataService.isRoleModelEnabled().then(utilsDataService => {
            if (utilsDataService) {
                this.userDataService.getCurrentUser()
                    .then(user => this.hasRole = user.hasRoles(this.node.roles || []));
            } else {
                this.hasRole = false;
            }
        });
    }

    openPermissions(event) {
        event.preventDefault();
        event.stopPropagation();
        if (this.ngbDataSetContextMenu.visible()) {
            this.ngbDataSetContextMenu.close();
        }
        this.$mdDialog.show({
            clickOutsideToClose: true,
            controller: ngbPermissionsFormController,
            controllerAs: 'ctrl',
            locals: {
                node: this.node
            },
            parent: angular.element(document.body),
            skipHide: true,
            template: require('../ngbPermissionsForm/ngbPermissionsForm.template.html'),
        });
    }

    getFileLink(id) {
        return this.projectDataService.getDatasetFileLink(id);
    }

    configureProjectElements() {
        const referenceList = this.projectContext.references
            .filter(r => +r.bioDataItemId === +this.node.reference.bioDataItemId);
        if (referenceList.length) {
            this.projectElements = [
                referenceList[0],
                ...referenceList[0].annotationFiles
            ]
                .reduce((result, item) => {
                    const [fileWithThisSource] = result.filter(resultItem => resultItem.source &&
                        item.source &&
                        resultItem.source === item.source
                    );
                    if (fileWithThisSource) {
                        return result;
                    }
                    return [...result, item];
                }, []);
            this.projectElements.forEach(e => {
                const splitPath = (e.source || e.path || '').split('/');
                e.filename = splitPath[splitPath.length - 1];
            });
        }
    }
    openMetadataPopup(event) {
        event.preventDefault();
        event.stopPropagation();
        if (this.ngbDataSetContextMenu.visible()) {
            this.ngbDataSetContextMenu.close();
        }
        this.$mdDialog.show({
            clickOutsideToClose: true,
            controller: ngbDataSetMetadataController,
            controllerAs: 'ctrl',
            locals: {
                node: this.node
            },
            parent: angular.element(document.body),
            skipHide: true,
            template: require('../ngbDataSetMetadata/ngbDataSetMetadata.tpl.html'),
        });
    }
}
