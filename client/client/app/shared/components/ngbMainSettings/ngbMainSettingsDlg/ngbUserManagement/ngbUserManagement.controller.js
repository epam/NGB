import BaseController from '../../../../baseController';

export default class ngbUserManagementController extends BaseController {

    gridOptions = {
        infiniteScrollRowsFromEnd: 10,
        infiniteScrollUp: true,
        infiniteScrollDown: true,
        enableFiltering: false,
        enableGridMenu: false,
        enableHorizontalScrollbar: 0,
        enablePinning: false,
        enableRowHeaderSelection: false,
        enableRowSelection: true,
        headerRowHeight: 20,
        height: '100%',
        multiSelect: false,
        rowHeight: 60,
        showHeader: true,
        treeRowHeaderAlwaysVisible: false,
        saveWidths: true,
        saveOrder: true,
        saveScroll: false,
        saveFocus: false,
        saveVisible: true,
        saveSort: true,
        saveFilter: false,
        savePinning: true,
        saveGrouping: false,
        saveGroupingExpandedStates: false,
        saveTreeView: false,
        saveSelection: false,
    };
    usersGridOptions = {};
    groupsGridOptions = {};
    rolesGridOptions = {};
    isUsersLoading = true;

    static get UID() {
        return 'ngbUserManagementController';
    }

    /* @ngInject */
    constructor($mdDialog, $scope, ngbUserManagementService, projectContext) {
        super();
        Object.assign(this, {
            $mdDialog,
            $scope,
            service: ngbUserManagementService,
        });
        // this.log = (log) => console.log(log);

        Object.assign(this.usersGridOptions, {
            ...this.gridOptions,
            appScopeProvider: this.scope,
            columnDefs: this.service.getUserManagementColumns(['User', 'Groups', 'Roles', 'Actions']),
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
            }
        });
        Object.assign(this.groupsGridOptions, {
            ...this.gridOptions,
            appScopeProvider: this.scope,
            data: [],
            columnDefs: this.service.getUserManagementColumns(['Group', 'Actions']),
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
            }
        });
        Object.assign(this.rolesGridOptions, {
            ...this.gridOptions,
            appScopeProvider: this.scope,
            data: [],
            columnDefs: this.service.getUserManagementColumns(['Role', 'Actions']),
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
            }
        });

        this.service.getUsers((users) => {
            this.usersGridOptions.data = users;

            if (this.groupsGridOptions.data && this.rolesGridOptions.data) {
                this.isUsersLoading = false;
                if (this.scope !== null && this.scope !== undefined) {
                    this.scope.$apply();
                }
            }
        });
        this.service.getRolesAndGroups((groups, roles) => {
            this.groupsGridOptions.data = groups;
            this.rolesGridOptions.data = roles;

            if (this.usersGridOptions.data) {
                this.isUsersLoading = false;
                if (this.scope !== null && this.scope !== undefined) {
                    this.scope.$apply();
                }
            }
        });
    }

    openEditUserDlg() {
        // todo
        // this.$mdDialog.show({
        //     clickOutsideToClose: true,
        //     controller: '',
        //     controllerAs: 'ctrl',
        //     parent: angular.element(document.body),
        //     template: require('./_.tpl.html'),
        // });
    }

    openEditRoleDlg() {
        //
    }

}

