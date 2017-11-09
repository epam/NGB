import angular from 'angular';

// Import Style
import './ngbBlatSearchPanel.scss';

// Import internal modules
import ngbBlatSearchPanel from './ngbBlatSearchPanel.component';
import controller from './ngbBlatSearchPanel.controller';

// Import components
import  ngbBlatSearch from './ngbBlatSearch';


// Import external modules
export default angular.module('ngbBlatSearchPanel', [ngbBlatSearch])
    .component('ngbBlatSearchPanel', ngbBlatSearchPanel)
    .controller(controller.UID, controller)
    .name;
