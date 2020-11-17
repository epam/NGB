import './ngbWigColorPreference.scss';

import angular from 'angular';

import colorPicker from '../../../../compat/colorPicker';
import component from './ngbWigColorPreference.component';
import controller from './ngbWigColorPreference.controller';
import run from './ngbWigColorPreference.run';

export default angular.module('ngbWigColorPreference', [colorPicker])
    .controller(controller.UID, controller)
    .component('ngbWigColorPreference', component)
    .run(run)
    .name;