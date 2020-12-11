import controller from './ngbBedColorPreference.controller';

export default function run($mdDialog, dispatcher) {
    const displayBedColorSettingsCallback = ({ config, sources, options }) => {
        if ((sources || []).length > 0) {
            const {
                color,
                defaultColor
            } = config || {};
            $mdDialog.show({
                clickOutsideToClose: true,
                controller: controller.UID,
                controllerAs: 'ctrl',
                locals: {
                    color,
                    defaultColor,
                    options,
                    sources,
                },
                template: require('./ngbBedColorPreference.dialog.tpl.html'),
            });
        }
    };

    dispatcher.on('bed:color:configure', displayBedColorSettingsCallback);
}
