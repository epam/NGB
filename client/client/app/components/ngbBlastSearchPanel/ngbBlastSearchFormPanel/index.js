// Import Style
import angular from 'angular';
import directive from './customMaxLength.directive';

// Import components
import ngbBlastAdditionalParams from './ngbBlastAdditionalParams';
import component from './ngbBlastSearchForm.component';

// Import internal modules
import * as constants from './ngbBlastSearchForm.constants';
import controller from './ngbBlastSearchForm.controller';
import './ngbBlastSearchForm.scss';

export default angular
    .module('ngbBlastSearchForm', [ngbBlastAdditionalParams])
    .constant('ngbBlastSearchFormConstants', constants)
    .directive('customMaxLength', directive)
    .controller(controller.UID, controller)
    .component('ngbBlastSearchForm', component)
    .name;
