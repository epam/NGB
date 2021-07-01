export default function () {
    return {
        restrict: 'A',
        link: function (scope, elem) {
            $(elem).on('scroll', function (event) {
                event.stopImmediatePropagation();
                event.stopPropagation();
            });
            $(elem).on('wheel', function (event) {
                event.stopImmediatePropagation();
                event.stopPropagation();
            });
        }

    };
}
