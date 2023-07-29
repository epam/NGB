import angular from 'angular';

import './ngbIdentificationsTab.scss';

import component from './ngbIdentificationsTab.component';
import controller from './ngbIdentificationsTab.controller';
import service from './ngbIdentificationsTab.service';

import ngbKnownDrugsPanel from './ngbKnownDrugsPanel';
import ngbDiseasesPanel from './ngbDiseasesPanel';
import ngbBibliographyPanel from './ngbBibliographyPanel';
import ngbIdentificationChat from './ngbIdentificationChat';
import ngbPluralText from './ngbPluralText';

export default angular
    .module('ngbIdentificationsTab', [
        ngbKnownDrugsPanel,
        ngbDiseasesPanel,
        ngbBibliographyPanel,
        ngbPluralText,
        ngbIdentificationChat
    ])
    .controller(controller.UID, controller)
    .component('ngbIdentificationsTab', component)
    .service('ngbIdentificationsTabService', service.instance)
    .name;
