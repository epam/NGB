import angular from 'angular';

import controller from './ngbLLM.controller';
import component from './ngbLLM.component';
import dialog from './ngbLLM.dialog.run';
import './ngbLLM.scss';

export default angular.module('ngbLargeLanguageModel', [])
    .controller(controller.UID, controller)
    .component('ngbLargeLanguageModel', component)
    .run(dialog)
    .name;
