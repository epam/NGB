import controller from './ngbTrackFontSize.controller';

export default function run($mdDialog, dispatcher) {
    const displayNgbTrackFontSizeCallback = ({sources, config, types, options})=> {
        const {group = false} = options || {};
        const applyToAllTracksVisible = !group;
        const applyToAllTracksOfTypeVisible = !group;
        $mdDialog.show({
            clickOutsideToClose: true,
            controller: controller.UID,
            controllerAs: 'ctrl',
            locals: {
                defaults: config.defaults,
                options: {
                    applyToAllTracksOfTypeVisible,
                    applyToAllTracksVisible
                },
                settings: config.settings,
                sources,
                types
            },
            multiple: true,
            skipHide: true,
            template: require('./ngbTrackFontSize.dialog.tpl.html'),
        });
    };

    dispatcher.on('tracks:header:style:configure', displayNgbTrackFontSizeCallback);
}
