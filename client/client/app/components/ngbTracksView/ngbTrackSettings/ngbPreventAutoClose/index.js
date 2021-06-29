export default function () {
    return {
        restrict: 'A',
        scope: {
            ngbPreventAutoClose: '<'
        },
        link(scope, element) {
            if (scope.ngbPreventAutoClose) {
                element.attr('md-prevent-menu-close', 'md-prevent-menu-close');
            }
        }
    };
}
