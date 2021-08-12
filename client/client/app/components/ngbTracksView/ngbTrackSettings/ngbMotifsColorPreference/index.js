import './ngbMotifsColorPreference.scss';

import angular from 'angular';

import colorPicker from '../../../../compat/colorPicker';
import component from './ngbMotifsColorPreference.component';
import controller from './ngbMotifsColorPreference.controller';
import run from './ngbMotifsColorPreference.run';

export default angular.module('ngbMotifsColorPreference', [colorPicker])
    .controller(controller.UID, controller)
    .component('ngbMotifsColorPreference', component)
    .run(run)
    .name;
