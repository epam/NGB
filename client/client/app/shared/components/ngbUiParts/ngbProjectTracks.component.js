export default {
    template: '<span class=\'md-label md-label__round md-label__light md-label__selectable\' aria-label=\'refresh\'>' +
                '<md-tooltip md-direction=\'right\' class=\'md-tooltip__full-height\'>' +
                    '<div ng-repeat=\'(formatName, formatCount) in $ctrl.items track by $index\'>{{formatName}} {{formatCount == 1 ? \'\' : \'(\'+formatCount+\')\'}}</div>' +
                '</md-tooltip>' +
                '{{$ctrl.count}}' +
              '</span>',
    /* @ngInject */
    controller: function () {

    },
    bindings: {
        items: '<',
        count: '<'
    }
};
