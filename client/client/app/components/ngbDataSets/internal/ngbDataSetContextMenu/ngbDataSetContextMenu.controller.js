import angular from 'angular';
import ngbPermissionsFormController from '../ngbPermissionsForm/ngbPermissionsForm.controller';

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
                ...referenceList[0].annotationFiles,
                ...this.node.items.filter(node => node.format === 'REFERENCE')
            ];
            this.projectElements.forEach(e => {
                const splitPath = e.path.split('/');
                e.filename = splitPath[splitPath.length - 1];
            });
        }
    }
}
