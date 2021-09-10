import angular from 'angular';

// Import Style
import './ngbMiew.scss';

// Import internal modules
import miewComponent from './ngbMiew.component';
import miewController from './ngbMiew.controller';
import miewSettings from './ngbMiew.settings';

export default angular.module('ngbMiew', [])
    .constant('miewSettings', miewSettings)
    .controller(miewController.UID, miewController)
    .component('ngbMiew', miewComponent)
    .name;
