import './ngbBookmark.scss';

//dependencies
import angular from 'angular';

import component from './ngbBookmark.component';
import controller from './ngbBookmark.controller';
import dlgController from './ngbBookmarkSaveDlg.controller';
//styles

//export angular module
export default angular.module('ngbBookmarkComponent', [])
    .controller(dlgController.UID, dlgController)
    .controller(controller.UID, controller)
    .component('ngbBookmark', component)
    .name;
