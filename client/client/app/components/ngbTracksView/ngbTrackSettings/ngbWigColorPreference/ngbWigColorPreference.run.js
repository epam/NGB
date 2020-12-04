import controller from './ngbWigColorPreference.controller';

export default function run($mdDialog, dispatcher) {
    const displayWigColorSettingsCallback = ({config, sources})=> {
        if ((sources || []).length > 0) {
            $mdDialog.show({
                clickOutsideToClose: true,
                controller: controller.UID,
                controllerAs: 'ctrl',
                locals: {
                    defaults: config.defaults,
                    settings: config.settings,
                    sources,
                },
                template: require('./ngbWigColorPreference.dialog.tpl.html'),
            });
        }
    };

    dispatcher.on('wig:color:configure', displayWigColorSettingsCallback);
}
