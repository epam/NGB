import angular from 'angular';

// Import Style
import './ngbMiew.scss';


// Import internal modules
import miewComponent from './ngbMiew.component';


export default angular.module('ngbMiewComponent', [])
    .component('ngbMiew', miewComponent)
    .name;
