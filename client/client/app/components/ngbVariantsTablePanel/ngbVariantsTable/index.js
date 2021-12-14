
import angular from 'angular';
import uiGrid from '../../../compat/uiGrid';
import './ngbVariantsTable.scss';

// Import internal modules
import tableController from './ngbVariantsTable.controller';
import ngbVariantsTable from './ngbVariantsTable.component.js';
import messages from './ngbVariantsTable.messages.js';
import service from './ngbVariantsTable.service.js';
import ngbVariantsTableFilter from './ngbVariantsTableFilter';
import arrayFilter from './array-filter';

// Import external modules
export default angular.module('ngbVariantsTableComponent', [uiGrid, ngbVariantsTableFilter])
    .constant('variantsTableMessages', messages)
    .service('variantsTableService', service.instance)
    .controller(tableController.UID, tableController)
    .component('ngbVariantsTable', ngbVariantsTable)
    .filter('array', arrayFilter)
    .name;
