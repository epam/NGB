import angular from 'angular';

// Import Style
import './ngbBlastSearchPanel.scss';
import {dispatcher} from '../../shared/dispatcher';

// Import internal modules
import ngbBlastSearchPanel from './ngbBlastSearchPanel.component';
import controller from './ngbBlastSearchPanel.controller';
import service from './ngbBlastSearchPanel.service';
import messages from './ngbBlastSearchPanel.messages.js';
import durationFilter from './ngbBlastSearch.duration.filter';


// Import components
import ngbBlastHistory from './ngbBlastHistory';
import ngbBlastSearchForm from './ngbBlastSearchFormPanel';
import ngbBlastWholeGenomeView from './ngbBlastWholeGenomeView';

// Import external modules
export default angular
    .module('ngbBlastSearchPanel', [ngbBlastWholeGenomeView, ngbBlastSearchForm, ngbBlastHistory])
    .constant('blastSearchMessages', messages)
    .service('dispatcher', dispatcher.instance)
    .service('ngbBlastSearchService', service.instance)
    .component('ngbBlastSearchPanel', ngbBlastSearchPanel)
    .controller(controller.UID, controller)
    .filter('duration', durationFilter)
    .name;
