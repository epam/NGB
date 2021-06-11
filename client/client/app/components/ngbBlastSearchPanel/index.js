import angular from 'angular';

// Import Style
import './ngbBlastSearchPanel.scss';

// Import internal modules
import ngbBlastSearchPanel from './ngbBlastSearchPanel.component';
import controller from './ngbBlastSearchPanel.controller';
import service from './ngbBlastSearchPanel.service';
import messages from './ngbBlastSearchPanel.messages.js';
import {dispatcher} from '../../shared/dispatcher';
import durationFilter from './ngbBlastSearch.duration.filter';
import percentageFilter from './ngbBlastSearch.percentage.filter';
import naFilter from './ngbBlastSearch.na.filter';


// Import components
import ngbBlastHistory from './ngbBlastHistory';
import ngbBlastSearchAlignment from './ngbBlastSearchAlignmentList';
import ngbBlastSearchForm from './ngbBlastSearchFormPanel';
import ngbBlastSearchResult from './ngbBlastSearchResult';
import ngbBlastWholeGenomeView from './ngbBlastWholeGenomeView';

// Import external modules
export default angular
    .module('ngbBlastSearchPanel',
        [ngbBlastWholeGenomeView, ngbBlastSearchForm, ngbBlastSearchResult, ngbBlastHistory, ngbBlastSearchAlignment]
    )
    .constant('blastSearchMessages', messages)
    .service('dispatcher', dispatcher.instance)
    .service('ngbBlastSearchService', service.instance)
    .component('ngbBlastSearchPanel', ngbBlastSearchPanel)
    .controller(controller.UID, controller)
    .filter('duration', durationFilter)
    .filter('percentage', percentageFilter)
    .filter('na', naFilter)
    .name;
