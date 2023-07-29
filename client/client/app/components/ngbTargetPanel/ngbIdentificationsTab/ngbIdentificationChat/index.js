import angular from 'angular';
import NgbIdentificationChatController from './ngbIdentificationChat.controller';
import NgbIdentificationChatComponent from './ngbIdentificationChat.component';
import NgbIdentificationChatService from './ngbIdentificationChat.service';
import './ngbIdentificationChat.scss';

export default angular.module('ngbIdentificationChat', [])
    .service(NgbIdentificationChatService.UID, NgbIdentificationChatService.instance)
    .component('ngbIdentificationChat', NgbIdentificationChatComponent)
    .controller(NgbIdentificationChatController.UID, NgbIdentificationChatController)
    .name;
