
import angular from 'angular';
import uiGrid from '../../../compat/uiGrid';

// Import internal modules
import tableController from './ngbVariantsTable.controller';
import ngbVariantsTable from './ngbVariantsTable.component.js';
import messages from './ngbVariantsTable.messages.js';
import service from './ngbVariantsTable.service.js';

// Import external modules
export default angular.module('ngbVariantsTableComponent', [uiGrid])
    .constant('variantsTableMessages', messages)
    .service('variantsTableService', service.instance)
    .controller(tableController.UID, tableController)
    .component('ngbVariantsTable', ngbVariantsTable)
    .name;