import controller from './ngbWigColorPreference.controller';

export default function run($mdDialog, dispatcher) {
    const displayWigColorSettingsCallback = (tracks)=> {
        if ((tracks || []).length > 0) {
            const { source, config } = tracks[0];
            // todo: multiple tracks configuration
            $mdDialog.show({
                clickOutsideToClose: true,
                controller: controller.UID,
                controllerAs: 'ctrl',
                locals: {
                    defaults: config.defaults,
                    settings: config.settings,
                    source,
                },
                template: require('./ngbWigColorPreference.dialog.tpl.html'),
            });
        }
    };

    dispatcher.on('wig:color:configure', displayWigColorSettingsCallback);
}
