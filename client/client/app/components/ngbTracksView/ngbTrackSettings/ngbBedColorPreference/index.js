import './ngbBedColorPreference.scss';

import angular from 'angular';

import colorPicker from '../../../../compat/colorPicker';
import component from './ngbBedColorPreference.component';
import controller from './ngbBedColorPreference.controller';
import run from './ngbBedColorPreference.run';

export default angular.module('ngbBedColorPreference', [colorPicker])
    .controller(controller.UID, controller)
    .component('ngbBedColorPreference', component)
    .run(run)
    .name;