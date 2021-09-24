import './ngbHeatmapColorSchemePreference.scss';

import angular from 'angular';

import constants from './ngbHeatmapColorSchemePreference.constants';
import run from './ngbHeatmapColorSchemePreference.run';

export default angular
    .module('ngbHeatmapColorSchemePreference', [])
    .constant('ngbHeatmapColorSchemePreferenceConstants', constants)
    .run(run)
    .name;
