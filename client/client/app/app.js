// Import base modules
import angular from 'angular';
import angularAnimate from 'angular-animate';
import angularAria from 'angular-aria';
import angularMaterialIcons from 'angular-material-icons';
import angularMessages from 'angular-messages';
import angularNvd3 from 'angular-nvd3';
import angularUiRouter from 'angular-ui-router';

// Import modules for work on Safari
import 'intl';
import 'intl/locale-data/jsonp/en.js';
// Import dependencies
import appConstants from '../constants/angular-module';
import dataServices from '../dataServices/angular-module';
import {FCSourcesManager, GroupAutoScaleManager} from '../modules/render/index';
import config from './app.config';
import controller from './app.controller';
import interceptor from './app.interceptor';
import layoutConstant from './app.layout.constant.js';
import routes from './app.routes';

//Import styles
import './app.scss';
import angularMaterial from './compat/angularMaterial';
import components from './components';
import PathwaysService from './components/ngbPathways/ngbPathways.service.js';
import AppearanceContext from './shared/appearanceContext';
import BamCoverageContext from './shared/bamCoverageContext';
import BLASTContext from './shared/blastContext';
import browserDetect from './shared/browserDetect';
import sharedComponents from './shared/components';
import {dispatcher} from './shared/dispatcher';
import eventHotkey from './shared/eventHotkeys';
import HeatmapContext from './shared/heatmapContext';
import './shared/hotkeys';
import lastActionRepeater from './shared/lastActionRepeater';
import MiewContext from './shared/miewContext';
import MotifsContext from './shared/motifsContext';
import NotificationsContext from './shared/notificationsContext';
import BisulfiteModeContext from './shared/bisulfiteModeContext';

// Import internal modules
import projectContext from './shared/projectContext';
import SelectionContext from './shared/selectionContext';
import appServices from './shared/services';

browserDetect();

export default angular.module('NGB', [
    angularAria,
    angularAnimate,
    angularMaterial,
    angularMaterialIcons,
    angularNvd3,
    angularUiRouter,
    angularMessages,

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
    .service('blastContext', BLASTContext.instance)
    .service('eventHotkey', eventHotkey.instance)
    .service('lastActionRepeater', lastActionRepeater.instance)
    .constant('appLayout', layoutConstant)
    .service('fcSourcesManager', FCSourcesManager.instance)
    .service('groupAutoScaleManager', GroupAutoScaleManager.instance)
    .service('notificationsContext', NotificationsContext.instance)
    .service('miewContext', MiewContext.instance)
    .service('heatmapContext', HeatmapContext.instance)
    .service('appearanceContext', AppearanceContext.instance)
    .service('motifsContext', MotifsContext.instance)
    .service('bamCoverageContext', BamCoverageContext.instance)
    .service('ngbPathwaysService', PathwaysService.instance)
    .service('bisulfiteModeContext', BisulfiteModeContext.instance)
    .name;
