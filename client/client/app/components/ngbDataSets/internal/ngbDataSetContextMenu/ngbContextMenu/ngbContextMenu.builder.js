import angular from 'angular';

export default function ($q, $compile, $animate, $rootScope, $controller) {
    return (config) => new NgbContextMenu($q, $compile, $animate, $rootScope, $controller, config);
}

class NgbContextMenu {

    controller;
    controllerAs;
    container = null;
    element = null;
    scope;

    htmlTemplate;

    constructor($q, $compile, $animate, $rootScope, $controller, config) {
        if (!config.template) {
            throw new Error('Expected context menu to have template');
        }
        this.$animate = $animate;
        this.$controller = $controller;
        this.$compile = $compile;
        this.$rootScope = $rootScope;
        this.$q = $q;
        this.config = config;
        this.container = angular.element(this.config.container || document.body);
        this.controller = config.controller || angular.noop;
        this.controllerAs = config.controllerAs;
        this.htmlTemplate = config.template;
    }

    open(target, css) {
        return new Promise((resolve) => {
            this.target = target;
            this.target.classList.add('ngb-context-menu-opened');
            this.attach();
            if (css && this.element) {
                this.element.css(css);
            }
            this.element && this.element.appendTo(this.container);
            this.$animate.enabled(false, this.element);
            resolve(this.element);
        });
    }

    bindController(locals) {
        if (this.controllerImpl) {
            return;
        }
        this.scope = this.$rootScope.$new();
        if (locals) {
            this.setLocals(locals);
        }
        this.controllerImpl = this.$controller(this.controller, {$scope: this.scope});
        if (this.controllerAs) {
            this.scope[this.controllerAs] = this.controllerImpl;
        }
    }

    attach() {
        this.element = angular.element(this.htmlTemplate);
        if (this.element.length === 0) {
            throw new Error('The template contains no elements; you need to wrap text nodes');
        }
        this.$compile(this.element)(this.scope);
    }

    setLocals(locals) {
        for (const prop in locals) {
            if (locals.hasOwnProperty(prop)) {
                this.scope[prop] = locals[prop];
            }
        }
    }

    close(disableFocus) {
        delete this.controllerImpl;
        if (this.target) {
            this.target.classList.remove('ngb-context-menu-opened');
        }
        const deferred = this.$q.defer();
        if (this.element) {
            this.scope.$destroy();
            deferred.resolve();
            this.element.remove();
            if (this.target && !disableFocus) {
                this.target.focus();
            }
            this.element = null;
        } else {
            deferred.resolve();
        }
        return deferred.promise;
    }

    visible() {
        return !!this.element;
    }
}