import controller from './ngbTrackFontSize.controller';

export default function run($mdDialog, dispatcher) {
    const displayNgbTrackFontSizeCallback = ({source, config})=> {
        $mdDialog.show({
            clickOutsideToClose: true,
            controller: controller.UID,
            controllerAs: 'ctrl',
            locals: { 
                defaults: config.defaults,
                settings: config.settings,
                source,
            },
            template: require('./ngbTrackFontSize.dialog.tpl.html'),
        });
    };

    dispatcher.on('tracks:header:style:configure', displayNgbTrackFontSizeCallback);
}
