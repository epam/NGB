import angular from 'angular';

// Import Style
import './ngbVariantsTablePanel.scss';

// Import internal modules
import ngbVariantsTablePanel from './ngbVariantsTablePanel.component.js';
import controller from './ngbVariantsTablePanel.controller';

// Import components
import  ngbVariantsTable from './ngbVariantsTable';
import  ngbVariantsTableColumn from './ngbVariantsTableColumn';


// Import external modules
export default angular.module('ngbVariantsTablePanel', [ngbVariantsTable,ngbVariantsTableColumn])
    .component('ngbVariantsTablePanel', ngbVariantsTablePanel)
    .controller(controller.UID, controller)
    .name;