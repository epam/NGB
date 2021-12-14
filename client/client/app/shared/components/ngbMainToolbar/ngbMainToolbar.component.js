import ngbConstants from '../../../../constants';

export default  {
    controller: class ngbHeaderProjectController {
        constructor(
            projectContext,
            miewContext,
            $scope,
            $location,
            $window,
            utilsDataService,
            userDataService
        ) {
            this.toolbarVisibility = projectContext.toolbarVisibility;
            this.browsingAllowed = () => projectContext.browsingAllowed;
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
            const urlPrefix = ngbConstants.urlPrefix;
            const prefix = urlPrefix && urlPrefix.endsWith('/')
                ? urlPrefix.slice(0, -1)
                : (urlPrefix || '');
            const logoutUrl = `${prefix}/saml/logout`;
            this.logout = () => {
                $window.location.href = logoutUrl;
            };
        }
    },
    template: require('./ngbMainToolbar.tpl.html')
};
