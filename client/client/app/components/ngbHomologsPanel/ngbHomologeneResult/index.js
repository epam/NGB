import './ngbHomologeneResult.scss';
import './ngbHomologeneResultTable/ngbHomologeneResultTable.scss';
import angular from 'angular';

// Import internal modules
import component from './ngbHomologeneResult.component';
import controller from './ngbHomologeneResult.controller';
import service from './ngbHomologeneResult.service';
import tableComponent from './ngbHomologeneResultTable/ngbHomologeneResultTable.component';
import tableController from './ngbHomologeneResultTable/ngbHomologeneResultTable.controller';


export default angular.module('ngbHomologeneResult', [])
    .service('ngbHomologeneResultService', service.instance)
    .controller(tableController.UID, tableController)
    .component('ngbHomologeneResultTable', tableComponent)
    .controller(controller.UID, controller)
    .component('ngbHomologeneResult', component)
    .name;
