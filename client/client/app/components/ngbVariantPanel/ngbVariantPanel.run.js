export default function run($mdDialog, dispatcher, projectContext) {


    const displayVariantDetailsCallback = (state)=> {
        const {variant} = state;

        $mdDialog.show({

            template: require('./ngbVariantPanelDialog.tpl.html'),
            controller: function ($scope) {

                const variantInfo = {
                    id: variant.id,
                    type: variant.type,
                    vcfFileId: variant.vcfFileId,
                    position: variant.position
                };

                $scope.variantRequest = Object.assign({
                    vcfFileId: variant.vcfFileId,
                    openByUrl: variant.openByUrl,
                    fileUrl: variant.fileUrl,
                    indexUrl: variant.indexUrl,
                    position: variant.position,
                    chromosomeId: variant.chromosome.id,
                    projectId: variant.projectId,
                    projectIdNumber: variant.projectIdNumber
                }, variantInfo);

                $scope.variant = variantInfo;

                $scope.ready = false;

            },
            clickOutsideToClose: true,
            onComplete: ($scope) => {
                $scope.ready = true;
            }

        });

    };

    dispatcher.on('variant:details:select', displayVariantDetailsCallback);

}
