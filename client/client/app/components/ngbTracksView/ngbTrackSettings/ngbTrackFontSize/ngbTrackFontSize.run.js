import controller from './ngbTrackFontSize.controller';

export default function run($mdDialog, dispatcher) {
    const displayNgbTrackFontSizeCallback = ({sources, config})=> {
        $mdDialog.show({
            clickOutsideToClose: true,
            controller: controller.UID,
            controllerAs: 'ctrl',
            locals: { 
                defaults: config.defaults,
                settings: config.settings,
                sources,
            },
            multiple: true,
            skipHide: true,
            template: require('./ngbTrackFontSize.dialog.tpl.html'),
        });
    };

    dispatcher.on('tracks:header:style:configure', displayNgbTrackFontSizeCallback);
}
