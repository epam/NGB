export default function () {
    return {
        restrict: 'A',
        require: '^mdChips',
        link: function (scope, element, attributes, mdChipsCtrl) {
            mdChipsCtrl.onInputBlur = function () {
                const chipBuffer = this.getChipBuffer();
                if (chipBuffer === '' && chipBuffer === null) return;
            };
        }
    };
}
