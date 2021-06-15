import controller from './ngbCustomizationTableRow.controller';

export default function () {
    return {
        restrict: 'A',
        scope: {
            customizeSettings: '=',
            settingItem: '='
        },
        controller: controller.UID,
        controllerAs: '$ctrl',
        bindToController: true,
        replace: true,
        template: require('./ngbCustomizationTableRow.tpl.html')
    };
}
