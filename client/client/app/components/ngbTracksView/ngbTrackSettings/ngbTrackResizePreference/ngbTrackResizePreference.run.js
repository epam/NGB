import helper from './ngbTrackResizePreference.helper';

export default function run($mdDialog, dispatcher, projectContext) {
    const displayTrackResizeSettingsCallback = tracks => {
        if ((tracks || []).length > 0) {
            $mdDialog.show({
                clickOutsideToClose: true,
                controller: helper($mdDialog, projectContext, tracks),
                template: require('./ngbTrackResizePreference.dialog.tpl.html'),
            });
        }
    };

    dispatcher.on('tracks:height:configure', displayTrackResizeSettingsCallback);
}
