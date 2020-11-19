import helper from './ngbWigResizePreference.helper';

export default function run($mdDialog, dispatcher, projectContext) {
    const displayWigResizeSettingsCallback = ({ config }) => {
        $mdDialog.show({
            clickOutsideToClose: true,
            controller: helper($mdDialog, projectContext, config),
            template: require('./ngbWigResizePreference.dialog.tpl.html'),
        });
    };

    dispatcher.on('wig:height:configure', displayWigResizeSettingsCallback);
}
