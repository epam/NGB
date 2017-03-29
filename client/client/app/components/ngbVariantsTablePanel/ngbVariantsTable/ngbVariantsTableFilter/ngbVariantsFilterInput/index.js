import './ngbVariantsFilterInput.scss';

import angular from 'angular';

// Import internal modules
import controller from './ngbVariantsFilterInput.controller';
import component from './ngbVariantsFilterInput.component';

// Import external modules
export default angular.module('ngbVariantsFilterInput', [])
    .controller(controller.UID, controller)
    .component('ngbVariantsFilterInput', component)
    .name;