const DEFAULT_COLOR = '#000000';
const TRANSPARENT_COLOR = 'transparent';

export default class ngbHomologsDomainsController {
    domainsView = [];
    scale = 1;

    constructor($scope, $timeout, $window, dispatcher) {
        Object.assign(this, {
            $scope,
            $timeout,
            $window,
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
            }
        }
    }

    calculateDomainsView(domains) {
        const result = [];
        let end = 0;
        const maxHomologLength = domains.maxHomologLength;
        if (!domains.domains) {
            return [];
        }
        domains.domains = domains.domains.sort((a, b) => a.start < b.start);
        domains.domains.forEach(d => {
            if (d.start - 1 > end + 1) {
                result.push({
                    width: Math.ceil((d.start - 1 - end)/maxHomologLength*100),
                    color: DEFAULT_COLOR,
                    isFiller: true
                });
            }
            result.push({
                width: Math.ceil((d.end - d.start + 1)/maxHomologLength*100),
                color: d.color,
                isFiller: false,
                name: d.name,
                id: d.id
            });
            end = d.end;
        });
        if (end + 1 < domains.homologLength) {
            result.push({
                width: Math.ceil((domains.homologLength - end)/maxHomologLength*100),
                color: DEFAULT_COLOR,
                isFiller: true
            });
        }
        if (domains.homologLength < domains.maxHomologLength) {
            result.push({
                width: Math.ceil((domains.maxHomologLength - domains.homologLength)/maxHomologLength*100),
                color: TRANSPARENT_COLOR,
                isFiller: true
            });
        }
        return result;
    }
}
