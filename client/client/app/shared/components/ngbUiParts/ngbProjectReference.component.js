export default {
    template: '<span ng-repeat=\'reference in $ctrl.items\'>{{reference.name}}</span>',
    controller: function () {

    },
    bindings: {
        items: '<'
    }
};
