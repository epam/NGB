export default function run($mdDialog, dispatcher, projectContext) {


    const displayVariantDetailsCallback = (state)=> {
        const {variant} = state;
        const projectId = projectContext.projectId;

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
                    position: variant.position,
                    chromosomeId: variant.chromosome.id,
                    projectId: projectId
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