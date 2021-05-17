// Import Style
import './ngbBlastSearchForm.scss';

import angular from 'angular';

// Import internal modules
import component from './ngbBlastSearchForm.component';
import controller from './ngbBlastSearchForm.controller';

export default angular
    .module('ngbBlastSearchForm', [])
    .controller(controller.UID, controller)
    .component('ngbBlastSearchForm', component)
    .name;
