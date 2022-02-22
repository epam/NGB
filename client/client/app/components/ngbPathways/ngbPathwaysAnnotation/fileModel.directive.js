export default function () {
    return {
        restrict: 'A',
        require: 'ngModel',
        link: function(scope, elem, attrs, ngModel) {
            elem.on('change', function(){
                const reader = new FileReader();

                reader.addEventListener('load', function (e) {
                    ngModel.$setViewValue({
                        value: e.target.result,
                        name: elem[0].files[0].name
                    });
                });

                reader.readAsBinaryString(elem[0].files[0]);
            });
        }
    };
}
