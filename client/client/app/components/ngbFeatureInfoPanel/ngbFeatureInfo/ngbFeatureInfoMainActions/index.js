import angular from 'angular';

import './ngbFeatureInfoMainActions.scss';

import controller from './ngbFeatureInfoMainActions.controller';
import component from './ngbFeatureInfoMainActions.component';


export default angular.module('ngbFeatureInfoMainActions', [])
    .component('ngbFeatureInfoMainActions', component)
    .controller(controller.UID, controller)
    .name;
