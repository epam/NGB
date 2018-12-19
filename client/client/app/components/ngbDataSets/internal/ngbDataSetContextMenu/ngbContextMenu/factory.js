import angular from 'angular';

export default function ($q, $compile, $animate, $rootScope, $controller) {
    return function contextMenuFactory(config) {
        if (!config.template) {
            throw new Error('Expected context menu to have template');
        }

        const controller = config.controller || angular.noop,
            controllerAs = config.controllerAs;
        let container = null,
            element = null,
            scope;

        const htmlTemplate = config.template;

        function open(target, locals, css) {
            return new Promise((resolve) => {
                this.target = target;
                this.target.classList.add('ngb-context-menu-opened');
                if (scope && locals) {
                    setLocals(locals);
                }
                attach(htmlTemplate, locals);
                if (css) {
                    element.css(css);
                }
                element.appendTo(container);
                $animate.enabled(false, element);
                resolve(element);
            });
        }

        function attach(html, locals) {
            container = angular.element(config.container || document.body);
            element = angular.element(html);
            if (element.length === 0) {
                throw new Error('The template contains no elements; you need to wrap text nodes');
            }

            scope = $rootScope.$new();
            scope.closeContextMenu = close;
            scope.contextMenuScope = scope;
            if (locals) {
                setLocals(locals);
            }

            const ctrl = $controller(controller, {$scope: scope});
            if (controllerAs) {
                scope[controllerAs] = ctrl;
            }
            $compile(element)(scope);
        }

        function setLocals(locals) {
            for (const prop in locals) {
                if (locals.hasOwnProperty(prop)) {
                    scope[prop] = locals[prop];
                }
            }
        }

        function close(disableFocus) {
            if (this.target) {
                this.target.classList.remove('ngb-context-menu-opened');
            }
            const deferred = $q.defer();
            if (element) {
                scope.$destroy();
                deferred.resolve();
                element.remove();
                if (this.target && !disableFocus) {
                    this.target.focus();
                }

                element = null;
            } else {
                deferred.resolve();
            }
            return deferred.promise;
        }

        function visible() {
            return !!element;
        }

        return {
            visible: visible,
            close: close,
            open: open
        };

    };
}
