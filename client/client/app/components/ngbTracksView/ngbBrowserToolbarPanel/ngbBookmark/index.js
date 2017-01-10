//dependencies
import angular from 'angular';
import * as ngbBookmarkComponent from './ngbBookmark.component.js';


//styles
import './ngbBookmark.scss';

//export angular module
export default angular.module('ngbBookmarkComponent', [])
    .component('ngbBookmark', ngbBookmarkComponent)
    .name;