export default function () {
    return {
        scope: true,
        link: function (scope, element, attrs) {
            const property = attrs.property;
            const elementChande = attrs.element;

            scope.$watch(property, function (newValue) {
                element.prop('indeterminate', newValue);
            });

            scope.$watch(elementChande, function (newValue) {
                const indeterminate = element.attr('indeterminate') === 'true';
                if (!newValue && indeterminate) {
                    element.prop('indeterminate', indeterminate);
                }
            });
        }
    };
}