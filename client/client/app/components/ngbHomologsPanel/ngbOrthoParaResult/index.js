import './ngbOrthoParaResult.scss';
import './ngbOrthoParaResultTable/ngbOrthoParaResultTable.scss';
import angular from 'angular';

// Import internal modules
import component from './ngbOrthoParaResult.component';
import controller from './ngbOrthoParaResult.controller';
import service from './ngbOrthoParaResult.service';
import tableComponent from './ngbOrthoParaResultTable/ngbOrthoParaResultTable.component';
import tableController from './ngbOrthoParaResultTable/ngbOrthoParaResultTable.controller';


export default angular.module('ngbOrthoParaResult', [])
    .service('ngbOrthoParaResultService', service.instance)
    .controller(tableController.UID, tableController)
    .component('ngbOrthoParaResultTable', tableComponent)
    .controller(controller.UID, controller)
    .component('ngbOrthoParaResult', component)
    .name;
