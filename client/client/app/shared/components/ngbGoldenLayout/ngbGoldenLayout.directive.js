import angular from 'angular';
import {isEquivalent} from '../../utils/Object';

export default ($document, $window, projectContext) => ({
    link: (scope, $el) => {

        const widthToolbar = 50;

        const toolbarVisibility = projectContext.toolbarVisibility;

        const menuWidth = toolbarVisibility ? widthToolbar : 0;

        $el.height($window.innerHeight);
        $el.width($window.innerWidth - menuWidth);

        scope.onInit();

        const onResize = () => {
            const currentWindowSize = {
                height: $window.innerHeight,
                width: $window.innerWidth
            };
            if (!isEquivalent(scope.currentDimensions, currentWindowSize)) {

                $el.height(currentWindowSize.height);
                $el.width(currentWindowSize.width - menuWidth);

                scope.currentDimensions = currentWindowSize;
                scope.onResize();
            }
        };


        angular.element($window).on('resize', onResize);

        scope.$on('$destroy', () => {
            angular.element($window).off('resize', onResize);
        });
    },
    restrict: 'A',
    scope: {
        onInit: '&',
        onResize: '&',
    }
});