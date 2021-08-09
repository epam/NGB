const DEFAULT_COLOR = '#000000';
const TRANSPARENT_COLOR = 'transparent';

export default class ngbHomologsDomainsController {
    domainsView = [];
    scale = 1;

    constructor($scope, $timeout, $window, $element, dispatcher) {
        Object.assign(this, {
            $scope,
            $timeout,
            $window,
            $element,
            dispatcher
        });
    }

    static get UID() {
        return 'ngbHomologsDomainsController';
    }

    $onChanges(changes) {
        if (changes.domains) {
            if (changes.domains.currentValue && changes.domains.currentValue !== changes.domains.previousValue) {
                this.domainsView = this.calculateDomainsView(changes.domains.currentValue);
                this.scale = this.$element.parent().width()/changes.domains.currentValue.maxHomologLength || 1;
            }
        }
    }
    $onInit() {
        this.initialize();
    }

    initialize() {
        // this.$window.resize()
    }

    calculateDomainsView(domains) {
        const result = [];
        let end = 0;
        if (!domains.domains) {
            return [];
        }
        domains.domains = domains.domains.sort((a, b) => a.start < b.start);
        domains.domains.forEach(d => {
            if (d.start - 1 > end + 1) {
                result.push({
                    width: Math.ceil((d.start - 1 - end)*this.scale),
                    color: DEFAULT_COLOR,
                    isFiller: true
                });
            }
            result.push({
                width: Math.ceil((d.end - d.start + 1)*this.scale),
                color: d.color,
                isFiller: false,
                name: d.name,
                id: d.id
            });
            end = d.end;
        });
        if (end + 1 < domains.homologLength) {
            result.push({
                width: Math.ceil((domains.homologLength - end)*this.scale),
                color: DEFAULT_COLOR,
                isFiller: true
            });
        }
        if (domains.homologLength < domains.maxHomologLength) {
            result.push({
                width: Math.ceil((domains.maxHomologLength - domains.homologLength)*this.scale),
                color: TRANSPARENT_COLOR,
                isFiller: true
            });
        }
        return result;
    }
}
