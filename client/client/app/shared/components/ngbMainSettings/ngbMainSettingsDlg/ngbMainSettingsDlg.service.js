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
