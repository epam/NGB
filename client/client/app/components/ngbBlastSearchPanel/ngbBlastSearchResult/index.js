import './ngbBlastSearchResult.scss';
import './ngbBlastSearchResultTable/ngbBlastSearchResultTable.scss';
import angular from 'angular';

// Import internal modules
import component from './ngbBlastSearchResult.component';
import controller from './ngbBlastSearchResult.controller';
import tableComponent from './ngbBlastSearchResultTable/ngbBlastSearchResultTable.component';
import tableController from './ngbBlastSearchResultTable/ngbBlastSearchResultTable.controller';
import tableService from './ngbBlastSearchResultTable/ngbBlastSearchResultTable.service';


export default angular.module('ngbBlastSearchResult', [])
    .service('ngbBlastSearchResultTableService', tableService.instance)
    .controller(tableController.UID, tableController)
    .component('ngbBlastSearchResultTable', tableComponent)
    .controller(controller.UID, controller)
    .component('ngbBlastSearchResult', component)
    .name;
