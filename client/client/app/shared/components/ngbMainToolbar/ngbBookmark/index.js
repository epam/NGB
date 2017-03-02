import './ngbBookmark.scss';

//dependencies
import angular from 'angular';

import component from './ngbBookmark.component';
import controller from './ngbBookmark.controller';
//styles

//export angular module
export default angular.module('ngbBookmarkComponent', [])
    .controller(controller.UID, controller)
    .component('ngbBookmark', component)
    .name;