import {parseObjectInSubItems} from '../../../utils/Object';

export default class ngbMainSettingsDlgService {

    localDataService;
    settings;

    static instance(localDataService, ngbMainSettingsDlgConstant) {
        return new ngbMainSettingsDlgService(localDataService, ngbMainSettingsDlgConstant);
    }

    constructor(localDataService, ngbMainSettingsDlgConstant) {
        this.localDataService = localDataService;
        this.localSettings = null;
        this.settings = ngbMainSettingsDlgConstant;
        this.defaultLocalSettings = localDataService.getDefaultSettings();
        this.featureGroups = {};

        parseObjectInSubItems(this.settings,
            obj => obj.type === 'item' && obj.byDefault && obj.byDefault.type === 'radio' && obj.byDefault.group)
            .forEach(name => {
                this.featureGroups[name] = {
                    value: '',
                    byDefault: ''
                };
            });

    }

    getSettings() {
        this.localSettings = this.localDataService.getSettings();
        return this._buildSettings();
    }

    updateThisLocalSettingsVar(localSettings) {
        for (const key of Object.keys(this.featureGroups)) {
            localSettings.defaultFeatures[key] = this.featureGroups[key].value;
        }
        this._pickOutLocalSettings(this.settings, localSettings);
        return localSettings;
    }

    getDefaultSettings() {
        this.localSettings = this.defaultLocalSettings;
        return this._buildSettings();
    }

    getUserManagementColumns(columnsList) {
        const columnDefs = [];
        for (let i = 0; i < columnsList.length; i++) {
            const column = columnsList[i];
            switch (column.toLowerCase()) {
                case 'actions':
                    // todo check if edit/delete action is available for current user
                    columnDefs.push({
                        cellTemplate: `
                            <div layout="row" style="flex-flow: row wrap; justify-content: center; align-items: center; width: 100%">
                                <md-button
                                    aria-label="Edit"
                                    class="md-fab md-mini md-hue-1"
                                    ng-if="row.entity.editable"
                                    ng-click="grid.appScope.$ctrl.openEditDialog(row.entity, $event)">
                                    <ng-md-icon icon="edit"></ng-md-icon>
                                </md-button>
                                <md-button
                                    aria-label="Delete"
                                    class="md-fab md-mini md-hue-1"
                                    ng-if="row.entity.deletable"
                                    ng-click="grid.appScope.$ctrl.openDeleteDialog(row.entity, $event)">
                                    <ng-md-icon icon="delete"></ng-md-icon>
                                </md-button>
                            </div>`,
                        enableColumnMenu: false,
                        enableSorting: false,
                        enableMove: false,
                        field: column.toLowerCase(),
                        maxWidth: 120,
                        minWidth: 120,
                        name: ''
                    });
                    break;
                case 'groups':
                    columnDefs.push({
                        cellTemplate: `
                            <div layout="row" ng-if="row.entity.groups.length <= 4" style="flex-flow: row wrap; align-items: center;">
                                <span
                                    ng-repeat="group in row.entity.groups track by $index"
                                    style="
                                        margin: 2px;
                                        padding: 2px 4px;
                                        border-radius: 5px;
                                        border: 1px solid #ddd;
                                        background-color: #fefefe;
                                        font-size: x-small;
                                        font-weight: bold;
                                        text-transform: uppercase;">
                                    {{group}}
                                </span>
                            </div>
                            <div layout="row" ng-if="row.entity.groups.length > 4" style="flex-flow: row wrap; align-items: center;">
                                <span
                                    ng-repeat="group in row.entity.groups.slice(0, 4) track by $index"
                                    style="
                                        margin: 2px;
                                        padding: 2px 4px;
                                        border-radius: 5px;
                                        border: 1px solid #ddd;
                                        background-color: #fefefe;
                                        font-size: x-small;
                                        font-weight: bold;
                                        text-transform: uppercase;">
                                    {{group}}
                                </span>
                                <ng-md-icon icon="more_horiz">
                                    <md-tooltip>
                                        <span
                                            ng-repeat="group in row.entity.groups track by $index"
                                            style="
                                                margin: 2px;
                                                padding: 2px 4px;
                                                border-radius: 5px;
                                                border: 1px solid #ddd;
                                                background-color: #fefefe;
                                                font-size: x-small;
                                                font-weight: bold;
                                                text-transform: uppercase;
                                                color: black;">
                                            {{group}}
                                        </span>
                                    </md-tooltip>
                                </ng-md-icon>
                            </div>
                        `,
                        enableColumnMenu: false,
                        enableSorting: false,
                        field: 'groups',
                        name: 'Groups',
                        width: '*',
                        minWidth: 50,
                    });
                    break;
                case 'roles':
                    columnDefs.push({
                        cellTemplate: `
                            <div layout="row" ng-if="row.entity.groups.length <= 4" style="flex-flow: row wrap; align-items: center;">
                                <span
                                    ng-repeat="role in row.entity.roles track by $index"
                                    style="
                                        margin: 2px;
                                        padding: 2px 4px;
                                        border-radius: 5px;
                                        border: 1px solid #ddd;
                                        background-color: #fefefe;
                                        font-size: x-small;
                                        font-weight: bold;
                                        text-transform: uppercase;">
                                    {{role}}
                                </span>
                            </div>
                            <div layout="row" ng-if="row.entity.groups.length > 4" style="flex-flow: row wrap; align-items: center;">
                                <span
                                    ng-repeat="role in row.entity.roles.slice(0, 4) track by $index"
                                    style="
                                        margin: 2px;
                                        padding: 2px 4px;
                                        border-radius: 5px;
                                        border: 1px solid #ddd;
                                        background-color: #fefefe;
                                        font-size: x-small;
                                        font-weight: bold;
                                        text-transform: uppercase;">
                                    {{role}}
                                </span>
                                <ng-md-icon icon="more_horiz">
                                    <md-tooltip>
                                        <span
                                            ng-repeat="role in row.entity.roles track by $index"
                                            style="
                                                margin: 2px;
                                                padding: 2px 4px;
                                                border-radius: 5px;
                                                border: 1px solid #ddd;
                                                background-color: #fefefe;
                                                font-size: x-small;
                                                font-weight: bold;
                                                text-transform: uppercase;
                                                color: black;">
                                            {{role}}
                                        </span>
                                    </md-tooltip>
                                </ng-md-icon>
                            </div>
                        `,
                        enableColumnMenu: false,
                        enableSorting: false,
                        field: 'roles',
                        name: 'Roles',
                        width: '*',
                        minWidth: 50,
                    });
                    break;
                default:
                    columnDefs.push({
                        field: column.toLowerCase(),
                        minWidth: 50,
                        name: column,
                        width: '*',
                    });
                    break;
            }
        }

        return columnDefs;
    }

    _buildSettings() {
        for (const key of Object.keys(this.featureGroups)) {
            this.featureGroups[key].value = this.localSettings.defaultFeatures[key];
            this.featureGroups[key].byDefault = this.defaultLocalSettings.defaultFeatures[key];
        }
        this._mergeSettings(this.settings);
        return this.settings;
    }

    _mergeSettings(settings) {
        for (const item of settings) {
            if (item.type === 'item') {
                this._mergeHotkey(item);
                this._mergeColor(item);
                this._mergeDefaultFeatures(item);
            }
            if (item.subItems) {
                this._mergeSettings(item.subItems);
            }
        }
    }

    _mergeHotkey(item) {
        item.hotkey = (this.localSettings.hotkeys[item.name] ? this.localSettings.hotkeys[item.name].hotkey : null) || '';
    }

    _mergeColor(item) {
        if (item.colors) {
            for (const color of item.colors) {
                const path = color.name.split('.');
                color.value = ngbMainSettingsDlgService.decimalToHexString(this.localSettings.colors[path[0]][path[1]]);
            }
        }
    }

    _mergeDefaultFeatures(item) {
        if (item.byDefault) {
            if (item.byDefault.type === 'checkbox') {
                item.byDefault.model = this.localSettings.defaultFeatures[item.byDefault.name];
            }
            if (item.byDefault.type === 'radio') {
                item.byDefault.model = this.featureGroups[item.byDefault.group];
            }
        }
    }


    _pickOutLocalSettings(setttings, localSettings) {
        for (const item of setttings) {
            if (item.type === 'item') {
                this._pickOutHotkey(item, localSettings);
                this._pickOutColor(item, localSettings);
                this._pickOutDefaultFeatures(item, localSettings);
            }
            if (item.subItems) {
                this._pickOutLocalSettings(item.subItems, localSettings);
            }
        }
    }

    _pickOutHotkey(item, localSettings) {
        localSettings.hotkeys[item.name] = {
            hotkey: item.hotkey
        };
    }

    _pickOutColor(item, localSettings) {
        if (item.colors) {
            for (const color of item.colors) {
                if (color.value) {
                    const path = color.name.split('.');
                    localSettings.colors[path[0]][path[1]] = typeof color.value === 'string' ? parseInt(color.value.substring(1), 16) : color.value;
                }
            }
        }
    }

    _pickOutDefaultFeatures(item, localSettings) {
        if (item.byDefault) {
            if (item.byDefault.type === 'checkbox') {
                localSettings.defaultFeatures[item.byDefault.name] = item.byDefault.model;
            }
        }
    }

    static decimalToHexString(d) {
        let hex = Number(d).toString(16);
        hex = `#${  '000000'.substr(0, 6 - hex.length)  }${hex}`;
        return hex;
    }
}
