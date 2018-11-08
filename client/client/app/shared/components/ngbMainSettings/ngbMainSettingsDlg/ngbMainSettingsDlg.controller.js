const DEFAULT_CONFIG = 7000;
export default class ngbMainSettingsDlgController {
    settings = null;

    static get UID() {
        return 'ngbMainSettingsDlgController';
    }

    showTrackHeadersIsDisabled = false;
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

    users = [{
        editable: true,
        groups: ['first', 'second', 'third', 'first', 'second', 'third', 'first', 'second', 'third', 'first', 'second', 'third', 'forth'],
        id: 1,
        name: 'userNumberOne@users.com',
        roles: ['first', 'second', 'third', 'first', 'second', 'third', 'first', 'second', 'third', 'forth'],
    }, {
        editable: true,
        groups: ['second', 'third', 'forth'],
        id: 2,
        name: 'userNumberTwo@users.com',
        roles: ['first', 'third', 'forth'],
    }, {
        editable: true,
        groups: ['second', 'third', 'forth'],
        id: 3,
        name: 'userNumberThree@users.com',
        roles: ['first', 'third', 'forth'],
    }, {
        editable: true,
        groups: ['third', 'forth'],
        id: 4,
        name: 'userNumberFour@users.com',
        roles: ['first', 'third'],
    }, {
        editable: true,
        groups: ['second', 'third'],
        id: 5,
        name: 'userNumberFive@users.com',
        roles: ['third', 'forth'],
    }];

    groups = [{
        deletable: true,
        editable: true,
        id: 1,
        group: 'userNumberOne@users.com',
        roles: ['first', 'second', 'third', 'first', 'second', 'third', 'first', 'second', 'third', 'forth'],
    }, {
        deletable: true,
        editable: true,
        id: 2,
        group: 'userNumberTwo@users.com',
        roles: ['first', 'third', 'forth'],
    }, {
        deletable: true,
        editable: true,
        id: 3,
        group: 'userNumberThree@users.com',
        roles: ['first', 'third', 'forth'],
    }, {
        deletable: true,
        editable: true,
        id: 4,
        group: 'userNumberFour@users.com',
        roles: ['first', 'third'],
    }, {
        deletable: true,
        editable: true,
        id: 5,
        group: 'userNumberFive@users.com',
        roles: ['third', 'forth'],
    }];

    roles = [{
        deletable: true,
        id: 1,
        role: 'role Number One',
    }, {
        deletable: true,
        id: 2,
        role: 'role Number Two',
    }, {
        deletable: true,
        id: 3,
        role: 'role Number Three',
    }, {
        deletable: true,
        id: 4,
        role: 'role Number Four',
    }, {
        deletable: true,
        id: 5,
        role: 'role Number Five',
    }];

    /* @ngInject */
    constructor(dispatcher, projectContext, localDataService, $mdDialog, ngbMainSettingsDlgService, $scope, settings) {
        this._dispatcher = dispatcher;
        this._localDataService = localDataService;
        this._mdDialog = $mdDialog;
        this.settings = settings;
        this.settings && (this.settings.maxBAMBP = this.settings.maxBAMBP || DEFAULT_CONFIG);
        this.showTrackHeadersIsDisabled = projectContext.collapsedTrackHeaders !== undefined && projectContext.collapsedTrackHeaders;
        this.settingsService = ngbMainSettingsDlgService;
        this.customizeSettings = this.settingsService.getSettings();

        this.scope = $scope;
        this.log = (log) => console.log(log);

        Object.assign(this.usersGridOptions, {
            ...this.gridOptions,
            appScopeProvider: this.scope,
            columnDefs: this.settingsService.getUserManagementColumns(['Name', 'Groups', 'Roles', 'Actions']),
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                // this.gridApi.colMovable.on.columnPositionChanged(this.scope, ::this.saveColumnsState);
                // this.gridApi.colResizable.on.columnSizeChanged(this.scope, ::this.saveColumnsState);
                // this.gridApi.selection.on.rowSelectionChanged(this.scope, ::this.rowClick);
                // this.gridApi.infiniteScroll.on.needLoadMoreData(this.scope, ::this.getDataDown);
                // this.gridApi.infiniteScroll.on.needLoadMoreDataTop(this.scope, ::this.getDataUp);
                // this.gridApi.core.on.sortChanged(this.scope, ::this.sortChanged);
                // this.gridApi.core.on.scrollEnd(this.scope, ::this.changeCurrentPage);
            }
        });
        this.usersGridOptions.data = this.users;
        Object.assign(this.groupsGridOptions, {
            ...this.gridOptions,
            appScopeProvider: this.scope,
            data: [],
            columnDefs: this.settingsService.getUserManagementColumns(['Group', 'Actions']),
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
            }
        });
        this.groupsGridOptions.data = this.groups;
        Object.assign(this.rolesGridOptions, {
            ...this.gridOptions,
            appScopeProvider: this.scope,
            data: [],
            columnDefs: this.settingsService.getUserManagementColumns(['Role', 'Actions']),
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
            }
        });
        this.rolesGridOptions.data = this.roles;
    }

    close() {
        this._mdDialog.hide();
    }

    save() {
        this.settingsService.updateThisLocalSettingsVar(this.settings);
        this._localDataService.updateSettings(this.settings);
        this._dispatcher.emitGlobalEvent('settings:change', this.settings);
        this.close();
    }

    getterSetterMaximumBPForBam(input) {
        return arguments.length ? this.settings.maxBAMBP = input : this.settings.maxBAMBP;
    }

    setToDefaultCustomizations() {
        this.customizeSettings = this.settingsService.getDefaultSettings();
        this.scope.$broadcast('setToDefault');
    }
}
