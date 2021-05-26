// Import Style
import './ngbBlastSearchForm.scss';

import angular from 'angular';

// Import internal modules
import * as constants from './ngbBlastSearchForm.constants';
import component from './ngbBlastSearchForm.component';
import controller from './ngbBlastSearchForm.controller';

export default angular
    .module('ngbBlastSearchForm', [])
    .constant('ngbBlastSearchFormConstants', constants)
    .controller(controller.UID, controller)
    .component('ngbBlastSearchForm', component)
    .name;
