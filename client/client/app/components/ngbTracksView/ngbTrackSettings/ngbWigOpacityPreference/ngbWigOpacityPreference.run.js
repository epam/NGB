import controller from './ngbWigOpacityPreference.controller';

export default function run($mdDialog, dispatcher) {
    const displayWigOpacitySettingsCallback = ({ source, config })=> {
        $mdDialog.show({
            clickOutsideToClose: true,
            controller: controller.UID,
            controllerAs: 'ctrl',
            locals: { 
                config: config,
                source,
            },
            template: require('./ngbWigOpacityPreference.dialog.tpl.html'),
        });
    };

    dispatcher.on('wig:opacity:configure', displayWigOpacitySettingsCallback);
}
