import ngbConstants from '../../../../constants';

export default  {
    controller: class ngbHeaderProjectController {
        constructor(
            projectContext,
            miewContext,
            dispatcher,
            $scope,
            $location,
            $window,
            utilsDataService,
            userDataService
        ) {
            this.toolbarVisibility = projectContext.toolbarVisibility;
            this.showBookmark = projectContext.currentChromosome !== null;
            this.browsingAllowed = () => projectContext.browsingAllowed;

            const onStateChange = async () => {
                this.showBookmark = projectContext.currentChromosome !== null;
                // todo: do we need an ability to save Miew state ONLY?
                // this.showBookmark = projectContext.currentChromosome !== null || !!miewContext.info;
            };

            this.isRoleModelEnabled = false;
            this.isLoggedIn = false;
            this.userIsAdmin = false;

            utilsDataService.isRoleModelEnabled().then(isRoleModelEnabled => {
                this.isRoleModelEnabled = isRoleModelEnabled;

                if (isRoleModelEnabled) {
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
                }
            });


            this.logout = () => {
                $window.location.href = `${ngbConstants.urlPrefix}/saml/logout`;
            };


            dispatcher.on('chromosome:change', onStateChange);
            dispatcher.on('reference:change', onStateChange);
            dispatcher.on('miew:structure:change', onStateChange);
            $scope.$on('$destroy', () => {
                dispatcher.removeListener('chromosome:change', onStateChange);
                dispatcher.removeListener('reference:change', onStateChange);
                dispatcher.removeListener('miew:structure:change', onStateChange);
            });
        }
    },
    template: require('./ngbMainToolbar.tpl.html')
};
