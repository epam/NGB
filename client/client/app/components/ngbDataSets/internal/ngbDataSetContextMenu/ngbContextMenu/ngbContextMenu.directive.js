import angular from 'angular';

export default function($injector, $window, $timeout) {
    return {
        restrict: 'A',
        link: function(scope, element, attrs) {
            const contextMenu = $injector.get(attrs.target);
            const locals = {};
            const wnd = angular.element($window);
            const triggerOnEvent = attrs.triggerOnEvent || 'contextmenu';

            if (attrs.locals) {
                const localKeys = attrs.locals.split(',').map(function(local) {
                    return local.trim();
                });
                angular.forEach(localKeys, function(key) {
                    locals[key] = scope[key];
                });
            }

            function open(event) {
                const pointerPosition = getPositionPropertiesOfEvent(event);
                const cssProperties = getCssPropertiesOfEvent(event);
                const currentTarget = event.currentTarget;
                contextMenu.bindController(locals);
                const shouldOpenContextMenuPromise = contextMenu.controllerImpl && contextMenu.controllerImpl.shouldOpenMenuPromise
                    ? contextMenu.controllerImpl.shouldOpenMenuPromise()
                    : new Promise(resolve => resolve(true));
                shouldOpenContextMenuPromise
                    .then(shouldOpen => {
                        if (shouldOpen) {
                            scope.$apply(function() {
                                contextMenu.open(currentTarget, cssProperties)
                                    .then(function (element) {
                                        element.hide();
                                        $timeout(function () {
                                            element.show(0, function () {
                                                adjustPosition(element, pointerPosition, currentTarget.clientHeight);
                                                angular.element(element).focus();
                                            });
                                        }, 0, false);
                                    });
                            });
                        }
                    });
            }

            function adjustPosition($element, pointerPosition, targetHeight = 0) {
                const viewport = {
                    left : wnd.scrollLeft(),
                    top : wnd.scrollTop()
                };

                viewport.right = viewport.left + wnd.width();
                viewport.bottom = viewport.top + wnd.height();
                const bounds = $element.offset();
                bounds.right = bounds.left + $element.outerWidth();
                bounds.bottom = bounds.top + $element.outerHeight();
                if (viewport.right < bounds.right) {
                    $element.css('left', pointerPosition.left - $element.outerWidth());
                }
                if (viewport.bottom < bounds.bottom) {
                    $element.css('top', pointerPosition.top - $element.outerHeight() - (targetHeight || 0));
                }
            }

            function close() {
                contextMenu.close();
            }

            function getPositionPropertiesOfEvent(event) {
                const position = { };
                if (event.pageX) {
                    position.left = Math.max(event.pageX, 0);
                } else {
                    const bounding = angular.element(event.target)[0].getBoundingClientRect();
                    position.left = Math.max(bounding.left, 0);
                }
                const bounding = angular.element(event.currentTarget)[0].getBoundingClientRect();
                position.top = Math.max(bounding.bottom, 0);
                return position;
            }

            function getCssPropertiesOfEvent(event) {
                const cssProperties = getPositionPropertiesOfEvent(event);

                cssProperties.top += 'px';
                cssProperties.left += 'px';
                cssProperties.position = 'absolute';

                return cssProperties;
            }

            function openContextMenu(event) {
                event.preventDefault();
                event.stopPropagation();

                scope.$apply(function() {
                    open(event);
                });
            }

            function closeContextMenu() {
                scope.$apply(function() {
                    close();
                });
            }

            element.bind(triggerOnEvent, function(event) {
                if (contextMenu.visible()) {
                    closeContextMenu();
                }
                openContextMenu(event);
            });

            function closeContextMenuIfVisible() {
                if (contextMenu.visible()) {
                    closeContextMenu();
                }
            }
            wnd.bind('click', closeContextMenuIfVisible);
            wnd.bind(triggerOnEvent, closeContextMenuIfVisible);
            wnd.bind('keyup', function(event) {
                if (contextMenu.visible() && event.keyCode === 27) {
                    closeContextMenu();
                }
            });
            wnd.on('resize', closeContextMenuIfVisible);
        }
    };
}
