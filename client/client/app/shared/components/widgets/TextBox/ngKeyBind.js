export default  function (keyCodes) {
    function map(obj) {
        const mapped = {};
        for (const key in obj) {
            if(obj.hasOwnProperty(key)) {
                const action = obj[key];
                if (keyCodes.hasOwnProperty(key)) {
                    mapped[keyCodes[key]] = action;
                }
            }
        }
        return mapped;
    }

    return function (scope, element, attrs) {
        const bindings = map(scope.$eval(attrs.ngKeyBind));
        element.bind('keydown keypress', function ($event) {
            if (bindings.hasOwnProperty($event.which)) {
                scope.$apply(function () {                   
                    scope.$eval(bindings[$event.which]);
                });
            }
        });
    };
}