import angular from 'angular';
import './ngbVariantsTablePaginate.scss';

// Import internal modules
import component from './ngbVariantsTablePaginate.component';
import controller from './ngbVariantsTablePaginate.controller';


// Import external modules
export default angular.module('ngbVariantsTablePaginate', [])
    .controller(controller.UID, controller)
    .component('ngbVariantsTablePaginate', component)
    .name;