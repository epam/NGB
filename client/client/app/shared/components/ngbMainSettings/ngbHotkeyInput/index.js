import angular from 'angular';
import component from './ngbHotkeyInput.component';
import controller from './ngbHotkeyInput.controller';
import messagesConstant from './ngbHotkeyInputMessages.constant';
import './ngbHotkeyInput.scss';

export default angular.module('ngbHotkeyInputComponent', [])
    .constant('ngbHotkeyInputMessagesConstant', messagesConstant)
    .component('ngbHotkeyInput', component)
    .controller(controller.UID, controller)
    .name;
