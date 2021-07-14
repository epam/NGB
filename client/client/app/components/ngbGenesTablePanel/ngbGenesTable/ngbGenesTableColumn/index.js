import './ngbGenesTableColumn.scss';

import angular from 'angular';

// Import internal modules
import component from './ngbGenesTableColumn.component';
import controller from './ngbGenesTableColumn.controller';


// Import external modules
export default angular.module('ngbGenesTableColumnComponent', [])
    .controller(controller.UID, controller)
    .component('ngbGenesTableColumn', component)
    .name;
