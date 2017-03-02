export default  {
    controller: class ngbHeaderProjectController {
        constructor(projectContext, dispatcher, $scope) {
            this.toolbarVisibility = projectContext.toolbarVisibility;
            this.showBookmark = projectContext.currentChromosome !== null;
            this.browsingAllowed = () => {
                return projectContext.browsingAllowed;
            };

            const onStateChange = async () => {
                this.showBookmark = projectContext.currentChromosome !== null;
            };

            dispatcher.on('chromosome:change', onStateChange);
            dispatcher.on('reference:change', onStateChange);
            $scope.$on('$destroy', () => {
                dispatcher.removeListener('chromosome:change', onStateChange);
                dispatcher.removeListener('reference:change', onStateChange);
            });
        }
    },
    template: require('./ngbMainToolbar.tpl.html')
};
