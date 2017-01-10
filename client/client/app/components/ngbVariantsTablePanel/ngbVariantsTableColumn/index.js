import './ngbVariantsTableColumn.scss';

import angular from 'angular';

// Import internal modules
import component from './ngbVariantsTableColumn.component';
import controller from './ngbVariantsTableColumn.controller';


// Import external modules
export default angular.module('ngbVariantsTableColumnComponent', [])
    .controller(controller.UID, controller)
    .component('ngbVariantsTableColumn', component)
    .name;