import angular from 'angular';
import component from '../ngbPathwaysColorSchemePreference/ngbPathwaysColorSchemePreference.component';
import controller from '../ngbPathwaysColorSchemePreference/ngbPathwaysColorSchemePreference.controller';
import constants from './ngbPathwaysColorSchemePreference.constants';
import './ngbPathwaysColorSchemePreference.scss';

export default angular
    .module('ngbPathwaysColorSchemePreference', [])
    .constant('ngbPathwaysColorSchemePreferenceConstants', constants)
    .controller(controller.UID, controller)
    .component('ngbPathwaysColorSchemePreference', component)
    .name;
