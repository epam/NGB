import angular from 'angular';

import controller from './controller';
import component from './component';
import {Placement} from './placement';

import './styles.scss';

export {Placement};

export default angular.module('ngbFloatingPanel', [])
    .controller(controller.UID, controller)
    .component('ngbFloatingPanel', component)
    .name;
