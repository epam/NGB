import controller from './ngbColorPicker.controller';

export default {
    bindings: {
        color: '=',
        options: '=',
        ngCloak: '='
    },
    restrict: 'E',
    controller,
    template: require('./ngbColorPicker.html')
};
