export default function () {
    return {
        restrict: 'A',
        scope: {
            customMaxLength: '<'
        },
        require: 'ngModel',
        link(scope, elm, attrs, ngModel) {
            // ngModel.$parsers.unshift(function(value) {
            //     ngModel.$setValidity('customMaxLength', value.length <= scope.customMaxLength);
            //     return value;
            // });
            // ngModel.$formatters.unshift(function(value) {
            //     ngModel.$setValidity('customMaxLength', value.length <= scope.customMaxLength);
            //     return value;
            // });
            ngModel.$validators.customMaxLength = function(modelValue) {
                if (!scope.customMaxLength || ngModel.$isEmpty(modelValue)) {
                    return true;
                }
                return modelValue.length <= scope.customMaxLength;
            };
        }
    };
}
