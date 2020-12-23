import controller from './ngbWigColorPreference.controller';

export default function run($mdDialog, dispatcher) {
    const displayWigColorSettingsCallback = ({config, options, sources})=> {
        const {
            browserId,
            group = false
        } = options || {};
        if ((sources || []).length > 0) {
            $mdDialog.show({
                clickOutsideToClose: true,
                controller: controller.UID,
                controllerAs: 'ctrl',
                locals: {
                    browserId,
                    defaults: config.defaults,
                    group,
                    settings: config.settings,
                    sources,
                },
                template: require('./ngbWigColorPreference.dialog.tpl.html'),
            });
        }
    };

    dispatcher.on('wig:color:configure', displayWigColorSettingsCallback);
}
