import angular from 'angular';

export default function($injector, $window, $parse, $timeout) {
    return {
        restrict: 'A',
        link: function(scope, element, attrs) {
            const contextMenu = $injector.get(attrs.target);
            const locals = {};
            const win = angular.element($window);
            const triggerOnEvent = attrs.triggerOnEvent || 'contextmenu';
            let pointerOffset, target;

            // prepare locals, these define properties to be passed on to the context menu scope
            if (attrs.locals) {
                const localKeys = attrs.locals.split(',').map(function(local) {
                    return local.trim();
                });
                angular.forEach(localKeys, function(key) {
                    locals[key] = scope[key];
                });
            }

            function getPosition(target) {
                const targetPosition = {};
                const targetElement = angular.element(target);
                const bounding = targetElement[0].getBoundingClientRect();

                targetPosition.top = bounding.top;
                targetPosition.left = bounding.left;

                return targetPosition;
            }

            function getOffset(targetPosition, pointerPosition) {
                const pointerOffset = {};

                pointerOffset.offsetY = pointerPosition.top - targetPosition.top;
                pointerOffset.offsetX = pointerPosition.left - targetPosition.left;

                return pointerOffset;
            }

            function open(event) {
                const targetPosition = getPosition(event.target);
                const pointerPosition = getPositionPropertiesOfEvent(event);
                const contextMenuPromise = contextMenu.open(event.target, locals, getCssPropertiesOfEvent(event));
                target = event.target;
                pointerOffset = getOffset(targetPosition, pointerPosition);
                contextMenuPromise.then(function(element) {
                    element.hide();
                    $timeout(function() {
                        element.show(0, function() {
                            adjustPosition(element, pointerPosition);
                            angular.element(element).focus();
                        });
                    }, 0, false);
                });
            }

            function adjustPosition($element, pointerPosition) {
                const viewport = {
                    left : win.scrollLeft(),
                    top : win.scrollTop()
                };

                viewport.right = viewport.left + win.width();
                viewport.bottom = viewport.top + win.height();
                const bounds = $element.offset();
                bounds.right = bounds.left + $element.outerWidth();
                bounds.bottom = bounds.top + $element.outerHeight();
                if (viewport.right < bounds.right) {
                    $element.css('left', pointerPosition.left - $element.outerWidth());
                }
                if (viewport.bottom < bounds.bottom) {
                    $element.css('top', pointerPosition.top - $element.outerHeight());
                }
            }

            function close() {
                contextMenu.close();
            }

            function getPositionPropertiesOfEvent(event) {
                const position = { };

                if (event.pageX && event.pageY) {
                    position.top = Math.max(event.pageY, 0);
                    position.left = Math.max(event.pageX, 0);
                } else {
                    const bounding = angular.element(event.target)[0].getBoundingClientRect();

                    position.top = Math.max(bounding.bottom, 0);
                    position.left = Math.max(bounding.left, 0);
                }

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
                if (contextMenu.active()) {
                    closeContextMenu();
                }
                openContextMenu(event);
            });

            element.bind('keyup', function(event) {
                // Alt + Shift + F10
                const F10 = 121;
                if (event.keyCode === F10 && event.shiftKey && event.altKey) {
                    if (!contextMenu.active()) {
                        openContextMenu(event);
                    }
                }
            });

            function handleWindowClickEvent() {
                if (contextMenu.active()) {
                    closeContextMenu();
                }
            }

            // Firefox treats a right-click as a click and a contextmenu event while other browsers
            // just treat it as a contextmenu event
            win.bind('click', handleWindowClickEvent);
            win.bind(triggerOnEvent, handleWindowClickEvent);

            win.bind('keyup', function(event) {
                if (contextMenu.active() && event.keyCode === 27) {
                    closeContextMenu();
                }
            });

            win.on('resize', function() {
                if (target) {
                    const currentTargetPosition = getPosition(target);
                    const position = {
                        left: currentTargetPosition.left + pointerOffset.offsetX,
                        top: currentTargetPosition.top + pointerOffset.offsetY
                    };
                    contextMenu.reposition(position);
                }
            });
        }
    };
}
