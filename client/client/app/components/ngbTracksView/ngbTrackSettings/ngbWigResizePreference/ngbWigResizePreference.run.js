import helper from './ngbWigResizePreference.helper';

export default function run($mdDialog, dispatcher, projectContext) {
    const displayWigResizeSettingsCallback = ({
        config,
        options = { maxHeight: 150, minHeight: 50 },
    }) => {
        $mdDialog.show({
            clickOutsideToClose: true,
            controller: helper($mdDialog, projectContext, config, options),
            template: require('./ngbWigResizePreference.dialog.tpl.html'),
        });
    };

    dispatcher.on('wig:height:configure', displayWigResizeSettingsCallback);
}
