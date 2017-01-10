// Import Style
import './ngbMainSettings.scss';

import angular from 'angular';
import colorPicker from '../../../compat/colorPicker';

import './ngbMainSettings.scss';

// Import internal modules --> components
import ngbGroupedCheckboxComponent from './ngbGroupedCheckbox';
import ngbHotkeyInputComponent from './ngbHotkeyInput';
import ngbMainSettingsDlgComponent from './ngbMainSettingsDlg';


import settingsBtnComponent from './ngbMainSettings.component';
import controller from './ngbMainSettings.controller';


export default angular.module('ngbMainSettingsComponent', [
    colorPicker, ngbHotkeyInputComponent, ngbGroupedCheckboxComponent,  ngbMainSettingsDlgComponent
])
    .component('ngbMainSettings', settingsBtnComponent)
    .controller(controller.UID, controller)
    .name;
