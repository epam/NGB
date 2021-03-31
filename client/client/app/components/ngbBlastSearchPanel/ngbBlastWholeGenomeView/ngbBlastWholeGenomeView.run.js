import { helper } from './ngbBlastWholeGenomeView.helper';

export default function run($mdDialog, dispatcher, projectContext) {
    const displayWholeGenomeViewCallback = () => {
        $mdDialog.show({
            template: require('./ngbBlastWholeGenomeView.dialog.tpl.html'),
            controller: helper($mdDialog, projectContext),
            clickOutsideToClose: true,
        });
    };
    dispatcher.on('blast:whole:genome:view', displayWholeGenomeViewCallback);
}
