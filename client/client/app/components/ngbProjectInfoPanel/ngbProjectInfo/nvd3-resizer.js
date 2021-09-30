export default function nvd3Resizer() {
    return function ($parent, $scope) {
        if ($parent && $scope) {
            let width = 0;
            let height = 0;
            let animationFrame;
            const animationFrameFn = () => {
                if (
                    $parent &&
                    $scope &&
                    ($parent.clientWidth !== width || $parent.clientHeight !== height)
                ) {
                    width = $parent.clientWidth;
                    height = $parent.clientHeight;
                    if ($scope.api && typeof $scope.api.update === 'function') {
                        $scope.api.updateWithTimeout(0); // nvd3.update better be called after event loop layout build phase
                    }
                }
                animationFrame = requestAnimationFrame(animationFrameFn);
            };
            animationFrameFn();
            return () => cancelAnimationFrame(animationFrame);
        }
        return () => {};
    };
}
