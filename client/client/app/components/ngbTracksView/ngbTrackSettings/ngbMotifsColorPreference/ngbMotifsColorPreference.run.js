import controller from './ngbMotifsColorPreference.controller';

export default function run($mdDialog, dispatcher) {
    const displayMotifsColorSettingsCallback = ({ config, sources, options, strand }) => {
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
                    strand
                },
                template: require('./ngbMotifsColorPreference.dialog.tpl.html'),
            });
        }
    };

    dispatcher.on('motifs:color:configure', displayMotifsColorSettingsCallback);
}
