// Import Style
import './ngbMarkdown.scss';

// Import internal modules
import angular from 'angular';
import component from './ngbMarkdown.component';

export default angular.module('ngbMarkdown', [])
    .component('ngbMarkdown', component)
    .name;
