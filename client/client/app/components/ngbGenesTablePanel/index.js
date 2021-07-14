import angular from 'angular';

// Import Style
import './ngbGenesTablePanel.scss';

// Import internal modules
import component from './ngbGenesTablePanel.component.js';
import controller from './ngbGenesTablePanel.controller';

// Import components
import  ngbGenesTable from './ngbGenesTable';


// Import external modules
export default angular.module('ngbGenesTablePanel', [ngbGenesTable])
    .component('ngbGenesTablePanel', component)
    .controller(controller.UID, controller)
    .name;
