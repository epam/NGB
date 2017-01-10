import angular from 'angular';
import {isEquivalent} from '../../utils/Object';

export default ($document, $window, projectContext) => ({
    link: (scope, $el) => {

        const heightToolbar = 41;

        const toolbarVisibility = projectContext.toolbarVisibility;

        const headerHeight = toolbarVisibility ? heightToolbar : 0;

        $el.height($window.innerHeight - headerHeight);
        $el.width($window.innerWidth);

        scope.onInit();

        const onResize = () => {
            const currentWindowSize = {
                height: $window.innerHeight,
                width: $window.innerWidth
            };
            if (!isEquivalent(scope.currentDimensions, currentWindowSize)) {

                $el.height(currentWindowSize.height - headerHeight);
                $el.width(currentWindowSize.width);

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