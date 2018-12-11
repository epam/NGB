import ngbConstants from '../../../../constants';

export default  {
    controller: class ngbHeaderProjectController {
        constructor(projectContext, dispatcher, $scope, $location, $window, userDataService) {
            this.toolbarVisibility = projectContext.toolbarVisibility;
            this.showBookmark = projectContext.currentChromosome !== null;
            this.browsingAllowed = () => projectContext.browsingAllowed;

            const onStateChange = async () => {
                this.showBookmark = projectContext.currentChromosome !== null;
            };
            userDataService.currentUserIsAdmin().then(isAdmin => {
                this.userIsAdmin = isAdmin;
                $scope.$apply();
            });

            userDataService.getCurrentUser().then(user => {
                this.isLoggedIn = user && user.enabled;
                $scope.$apply();
            }, () => {
                this.isLoggedIn = false;
                $scope.$apply();
            });

            this.logout = () => {
                $window.location.href = `${ngbConstants.urlPrefix}/saml/logout`;
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
