import './ngbWigColorPreference.scss';

import angular from 'angular';

import controller from './ngbWigColorPreference.controller';
import component from './ngbWigColorPreference.component';
import run from './ngbWigColorPreference.run';

export default angular.module('ngbWigColorPreference', [])
    .controller(controller.UID, controller)
    .component('ngbWigColorPreference', component)
    .run(run)
    .name;