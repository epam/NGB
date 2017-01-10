import ngKeyBind from './ngKeyBind';
import ngAutoFocus from './ngAutoFocus';
import angular from 'angular';

export default angular.module('ngbTextBox', [])
    .constant('keyCodes', {
        esc: 27,
        space: 32,
        enter: 13,
        tab: 9,
        backspace: 8,
        shift: 16,
        ctrl: 17,
        alt: 18,
        capslock: 20,
        numlock: 144
    })
    .directive('ngKeyBind', ngKeyBind)
    .directive('ngAutoFocus', ngAutoFocus)
    .name;