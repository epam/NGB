import angular from 'angular';

export default angular.module('collapsibleMenu', [])
    .directive('collapsibleMenuItem', () => function (scope, element, attr) {
        if (scope.registerMenuItem) {
            scope.registerMenuItem(element, scope.$eval(attr.collapsibleMenuItem));
        }
        scope.$watch(function () { return element.outerWidth(); }, () => {
            scope.correctHiddenItems && scope.correctHiddenItems();
        });
        scope.$on('destroy', () => {
            if (scope.unregisterMenuItem) {
                scope.unregisterMenuItem(element, scope.$eval(attr.collapsibleMenuItem));
            }
        });
    })
    .name;
