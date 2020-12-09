export default () => ({
    restrict: 'AC',
    link(scope, element) {
        let isResizing = false;
        let eventStartScreenY;
        
        element.on('mousedown', event => {
            isResizing = true;

            eventStartScreenY = event.screenY;
            scope.$emit('resizeStart');

            event.preventDefault();
            event.stopImmediatePropagation();
        });

        const moveListener = event => {
            if (!isResizing)
                return;

            scope.$emit('resize', event.screenY - eventStartScreenY);

            event.preventDefault();
            event.stopImmediatePropagation();
        };

        const endListener = event => {
            isResizing = false;
            scope.$emit('resizeEnd');
            if (event.target.tagName.toLowerCase() === 'input' && event.target.type === 'text' && event.target === document.activeElement) {
                return;
            }
            event.preventDefault();
        };

        window.addEventListener('mousemove', moveListener);
        window.addEventListener('mouseleave', endListener);
        window.addEventListener('mouseup', endListener);

        scope.$on('$destroy', () => {
            window.removeEventListener('mousemove', moveListener);
            window.removeEventListener('mouseleave', endListener);
            window.removeEventListener('mouseup', endListener);
        });
    }
});