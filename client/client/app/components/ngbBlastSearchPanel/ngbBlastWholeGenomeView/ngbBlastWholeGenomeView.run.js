import { helper } from './ngbBlastWholeGenomeView.helper';

export default function run($mdDialog, dispatcher, projectContext) {
    const displayWholeGenomeViewCallback = (data) => {
        $mdDialog.show({
            template: require('./ngbBlastWholeGenomeView.dialog.tpl.html'),
            controller: helper($mdDialog, dispatcher, projectContext, data),
            clickOutsideToClose: true,
        });
    };
    dispatcher.on('blast:whole:genome:view', displayWholeGenomeViewCallback);
}
