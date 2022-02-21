import controller from './ngbVcfSampleAliases.controller';

export default function run($mdDialog, dispatcher) {
    const displaySampleAliases = (tracks)=> {
        const [anyTrack] = tracks;
        if (anyTrack) {
            const {config, samplesInfo} = anyTrack;
            $mdDialog.show({
                template: require('./ngbVcfSampleAliases.dialog.tpl.html'),
                controller: controller.UID,
                controllerAs: '$ctrl',
                clickOutsideToClose: true,
                locals: {
                    config,
                    samplesInfo
                }
            });
        }
    };

    dispatcher.on('vcf:rename:samples', displaySampleAliases);
}
