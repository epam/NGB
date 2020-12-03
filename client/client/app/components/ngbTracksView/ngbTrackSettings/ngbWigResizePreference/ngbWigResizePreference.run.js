import helper from './ngbWigResizePreference.helper';

export default function run($mdDialog, dispatcher, projectContext) {
    const displayWigResizeSettingsCallback = (tracks) => {
        if ((tracks || []).length > 0) {
            // todo: multiple tracks resizing
            const {config} = tracks[0];
            $mdDialog.show({
                clickOutsideToClose: true,
                controller: helper($mdDialog, projectContext, config),
                template: require('./ngbWigResizePreference.dialog.tpl.html'),
            });
        }
    };

    dispatcher.on('wig:height:configure', displayWigResizeSettingsCallback);
}
