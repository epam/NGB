import helper from './ngbWigResizePreference.helper';

export default function run($mdDialog, dispatcher, projectContext) {
    const displayWigResizeSettingsCallback = (tracks) => {
        if ((tracks || []).length > 0) {
            // todo: multiple tracks resizing
            const {config} = tracks[0];
            // todo: generate max & min heights from tracks array
            const options = { maxHeight: 150, minHeight: 50 };
            $mdDialog.show({
                clickOutsideToClose: true,
                controller: helper($mdDialog, projectContext, config, options),
                template: require('./ngbWigResizePreference.dialog.tpl.html'),
            });
        }
    };

    dispatcher.on('tracks:height:configure', displayWigResizeSettingsCallback);
}
