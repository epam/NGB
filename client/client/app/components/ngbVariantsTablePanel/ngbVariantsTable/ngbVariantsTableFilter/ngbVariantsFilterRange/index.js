import './ngbVariantsFilterRange.scss';

import angular from 'angular';

// Import internal modules
import controller from './ngbVariantsFilterRange.controller';
import component from './ngbVariantsFilterRange.component';

// Import external modules
export default angular.module('ngbVariantsFilterRange', [])
    .controller(controller.UID, controller)
    .component('ngbVariantsFilterRange', component)
    .name;