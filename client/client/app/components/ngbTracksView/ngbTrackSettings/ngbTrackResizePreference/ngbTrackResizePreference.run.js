import controller from '../ngbTrackFontSize/ngbTrackFontSize.controller';
import helper from './ngbTrackResizePreference.helper';

export default function run($mdDialog, dispatcher, projectContext) {
    const displayTrackResizeSettingsCallback = (options) => {
        const {tracks} = options;
        if ((tracks || []).length > 0) {
            $mdDialog.show({
                clickOutsideToClose: true,
                controller: helper($mdDialog, projectContext, options),
                template: require('./ngbTrackResizePreference.dialog.tpl.html'),
            });
        }
    };

    dispatcher.on('tracks:height:configure', displayTrackResizeSettingsCallback);
}
