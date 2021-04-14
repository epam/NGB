import "core-js/stable";
import "regenerator-runtime/runtime";

import browserDetect from './shared/browserDetect';
browserDetect();

import './shared/hotkeys';

// Import modules for work on Safari
import 'intl';
import 'intl/locale-data/jsonp/en.js';

// Import base modules
import angular from 'angular';
import angularAnimate from 'angular-animate';
import angularAria from 'angular-aria';
import angularMaterial from './compat/angularMaterial';
import angularMaterialIcons from 'angular-material-icons';
import angularNvd3 from 'angular-nvd3';
import angularUiRouter from 'angular-ui-router';

// Import internal modules
import projectContext from './shared/projectContext';
import SelectionContext from './shared/selectionContext';
import config from './app.config';
import controller from './app.controller';
import {dispatcher} from './shared/dispatcher';
import eventHotkey from './shared/eventHotkeys';
import interceptor from './app.interceptor';
import lastActionRepeater from './shared/lastActionRepeater';
import layoutConstant from './app.layout.constant.js';
import routes from './app.routes';
// Import dependencies

import appConstants from '../constants/angular-module';
import appServices from './shared/services';
import components from './components';
import dataServices from '../dataServices/angular-module';
import {GroupAutoScaleManager} from '../modules/render/index';
import sharedComponents from './shared/components';

//Import styles
import './app.scss';

export default angular.module('NGB', [
    angularAria,
    angularAnimate,
    angularMaterial,
    angularMaterialIcons,
    angularNvd3,
    angularUiRouter,

    appConstants,
    appServices,
    components,
    dataServices,
    sharedComponents
])
    .config(['$mdThemingProvider', config])
    .config(routes)
    .controller(controller.UID, controller)
    .service('interceptor', interceptor)
    .service('dispatcher', dispatcher.instance)
    .service('projectContext', projectContext.instance)
    .service('selectionContext', SelectionContext.instance)
    .service('eventHotkey', eventHotkey.instance)
    .service('lastActionRepeater', lastActionRepeater.instance)
    .constant('appLayout', layoutConstant)
    .service('groupAutoScaleManager', GroupAutoScaleManager.instance)
    .name;