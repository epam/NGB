import angular from 'angular';
import NgbChatController from './ngbChat.controller';
import NgbChatComponent from './ngbChat.component';
import './ngbChat.scss';

export default angular.module('ngbChat', [])
    .component('ngbChat', NgbChatComponent)
    .controller(NgbChatController.UID, NgbChatController)
    .name;
