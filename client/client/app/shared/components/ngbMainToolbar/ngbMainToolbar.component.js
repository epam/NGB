import ngbConstants from '../../../../constants';

export default  {
    controller: class ngbHeaderProjectController {
        constructor(projectContext, dispatcher, $scope, $location, $window, utilsDataService, userDataService) {
            this.toolbarVisibility = projectContext.toolbarVisibility;
            this.showBookmark = projectContext.currentChromosome !== null;
            this.browsingAllowed = () => projectContext.browsingAllowed;

            const onStateChange = async () => {
                this.showBookmark = projectContext.currentChromosome !== null;
            };

            this.isRoleModelEnabled = false;
            this.isLoggedIn = false;
            this.userIsAdmin = false;

            utilsDataService.isRoleModelEnabled().then(isRoleModelEnabled => {
                this.isRoleModelEnabled = isRoleModelEnabled;

                if (isRoleModelEnabled) {
                    userDataService.currentUserIsAdmin().then(isAdmin => {
                        this.userIsAdmin = isAdmin;
                    });
                    userDataService.getCurrentUser().then(user => {
                        this.isLoggedIn = user && user.enabled;
                    }, () => {
                        this.isLoggedIn = false;
                    });
                }
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
