import './ngbVariantsFilterCheckbox.scss';

import angular from 'angular';

// Import internal modules
import controller from './ngbVariantsFilterCheckbox.controller';
import component from './ngbVariantsFilterCheckbox.component';

// Import external modules
export default angular.module('ngbVariantsFilterCheckbox', [])
    .controller(controller.UID, controller)
    .component('ngbVariantsFilterCheckbox', component)
    .name;