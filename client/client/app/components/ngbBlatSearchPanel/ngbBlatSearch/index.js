
import angular from 'angular';
import uiGrid from '../../../compat/uiGrid';
import './ngbBlatSearch.scss';

// Import internal modules
import tableController from './ngbBlatSearch.controller';
import ngbBlatSearch from './ngbBlatSearch.component.js';
import messages from './ngbBlatSearch.messages.js';
import service from './ngbBlatSearch.service.js';

// Import external modules
export default angular.module('ngbBlatSearchComponent', [uiGrid])
    .constant('blatSearchMessages', messages)
    .service('blatSearchService', service.instance)
    .controller(tableController.UID, tableController)
    .component('ngbBlatSearch', ngbBlatSearch)
    .name;