const DEFAULT_COLOR = '#000000';
const TRANSPARENT_COLOR = 'transparent';

export default class ngbHomologsDomainsController {
    domainsView = [];
    domainsDesc = [];

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
                Object.assign(this, this.calculateDomainsView(changes.domains.currentValue));
            }
        }
    }

    calculateDomainsView(domains) {
        const domainsView = [];
        let end = 0;
        const maxHomologLength = domains.maxHomologLength;
        if (!domains.domains) {
            return [];
        }
        if (domains.domains.length) {
            domains.domains = domains.domains.sort((a, b) => a.start < b.start);
            domains.domains.forEach(d => {
                if (d.start - 1 > end + 1) {
                    domainsView.push({
                        width: Math.ceil((d.start - 1 - end) / maxHomologLength * 100),
                        color: DEFAULT_COLOR,
                        isFiller: true
                    });
                }
                domainsView.push({
                    start: d.start,
                    end: d.end,
                    width: Math.ceil((d.end - d.start + 1) / maxHomologLength * 100),
                    color: d.color,
                    isFiller: false,
                    name: d.name,
                    id: d.id
                });
                end = d.end;
            });
            if (end + 1 < domains.homologLength) {
                domainsView.push({
                    width: Math.ceil((domains.homologLength - end) / maxHomologLength * 100),
                    color: DEFAULT_COLOR,
                    isFiller: true
                });
            }
            if (domains.homologLength < domains.maxHomologLength) {
                domainsView.push({
                    width: Math.ceil((domains.maxHomologLength - domains.homologLength) / maxHomologLength * 100),
                    color: TRANSPARENT_COLOR,
                    isFiller: true
                });
            }
        } else {
            domainsView.push({
                width: 100,
                color: TRANSPARENT_COLOR,
                isFiller: true
            });
        }
        const domainsDesc = {
            domains: domainsView.filter(d => !d.isFiller),
            accession_id: domains.accession_id,
            aa: domains.homologLength,
            toggleTooltip: false
        };
        return {
            domainsView: domainsView,
            domainsDesc: domainsDesc
        };
    }
}
