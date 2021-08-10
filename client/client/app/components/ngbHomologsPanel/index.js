import angular from 'angular';

// Import Style
import './ngbHomologsPanel.scss';

// Import internal modules
import ngbHomologsPanel from './ngbHomologsPanel.component';
import controller from './ngbHomologsPanel.controller';
import service from './ngbHomologs.service';
import {dispatcher} from '../../shared/dispatcher';


// Import components
import ngbHomologeneTable from './ngbHomologeneTable';
import ngbHomologeneResult from './ngbHomologeneResult';
import ngbOrthoParaTable from './ngbOrthoParaTable';
import ngbOrthoParaResult from './ngbOrthoParaResult';
import ngbHomologsDomains from './ngbHomologsDomains';
import ngbHomologsPanelPaginate from './ngbHomologsPanelPaginate';

// Import external modules
export default angular
    .module('ngbHomologsPanel', [
        ngbHomologsPanelPaginate, ngbHomologsDomains,
        ngbHomologeneTable, ngbHomologeneResult,
        ngbOrthoParaTable, ngbOrthoParaResult
    ])
    .service('dispatcher', dispatcher.instance)
    .service('ngbHomologsService', service.instance)
    .component('ngbHomologsPanel', ngbHomologsPanel)
    .controller(controller.UID, controller)
    .name;
