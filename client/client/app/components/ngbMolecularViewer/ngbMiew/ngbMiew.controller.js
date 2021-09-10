import Miew from 'miew';
import angular from 'angular';

export default class ngbMiewController {
    static get UID () {
        return 'ngbMiewController';
    }

    constructor($element, $scope, $window, dispatcher, miewSettings, miewContext) {
        this.$scope = $scope;
        this.miewContainer = $element[0];
        this.settings = miewSettings;
        this.miewContext = miewContext;
        this.dispatcher = dispatcher;
        this.miewCameraChanged = this.miewCameraChangedCallback.bind(this);
        const onChange = this.changePdb.bind(this);
        $scope.$watch('$ctrl.pdb', onChange);
        $scope.$watch('$ctrl.position', onChange);
        $scope.$watch('$ctrl.transcript', onChange);
        $scope.$watch('$ctrl.chains', onChange);
        $scope.$watch('$ctrl.chainId', onChange);
        $scope.$watch('$ctrl.region', onChange);
        $scope.$watch('$ctrl.displayMode', this.setChainMaterials.bind(this));
        $scope.$watch('$ctrl.displayColor', this.setChainMaterials.bind(this));
        const resizeHandler = () => {
            if (this.viewer) {
                this.viewer._onResize();
            }
        };
        angular.element($window).on('resize', resizeHandler);
        const miewActiveEventHandler = this.miewPanelActiveChanged.bind(this);
        this.dispatcher.on('miew:panel:active', miewActiveEventHandler);
        $scope.$on('$destroy', () => {
            if (this.cameraReportTimer) {
                clearTimeout(this.cameraReportTimer);
                this.cameraReportTimer = undefined;
            }
            if (this.viewer) {
                this.viewer.removeEventListener('transform', this.miewCameraChanged);
            }
            this.dispatcher.removeListener('miew:panel:active', miewActiveEventHandler);
            angular.element($window).off('resize', resizeHandler);
        });
        this.miewPanelActiveChanged(true);
    }

    miewPanelActiveChanged(active) {
        if (active) {
            this.loadCurrentPdb = undefined;
            if (this.viewer) {
                this.viewer.removeEventListener('transform', this.miewCameraChanged);
                this.viewer = null;
            }
            this.viewer = new Miew({
                container: this.miewContainer,
                settings: this.settings.viewer
            });
            if (this.viewer.init()) {
                this.viewer.enableHotKeys(false);
                this.viewer.run();
                this.viewer.addEventListener('transform', this.miewCameraChanged);
                this.changePdb();
            }
        }
    }

    miewCameraChangedCallback () {
        const report = () => {
            if (this.viewer) {
                this.camera = this.viewer.view();
                this.miewContext.update({camera: this.camera});
                if (this.$scope) {
                    this.$scope.$apply();
                }
            }
        };
        const DEBOUNCE = 250;
        if (this.cameraReportTimer) {
            clearTimeout(this.cameraReportTimer);
            this.cameraReportTimer = undefined;
        }
        this.cameraReportTimer = setTimeout(report, DEBOUNCE);
    }

    changePdb () {
        if (!this.viewer) {
            return;
        }
        this.changeHighlightedRegion();
        if (this.pdb) {
            if (this.pdb !== this.currentPdb || !this.loadCurrentPdb) {
                this.currentPdb = this.pdb;
                this.loadCurrentPdb = this.viewer.load(this.pdb);
            }
            this.loadCurrentPdb
                .then(() => {
                    this.setChainMaterials();
                })
                .then(() => {
                    if (this.camera && this.camera !== this.viewer.view()) {
                        try {
                            this.viewer.view(this.camera);
                        } catch (e) {
                            this.viewer.resetView();
                        }
                    }
                })
                .catch((e) => {
                    // eslint-disable-next-line
                    console.warn(`Error loading pdb ${this.currentPdb}`, e.message);
                });
        }
        if (!this.pdb) {
            this.currentPdb = undefined;
            this.viewer.unload();
        }
    }

    changeHighlightedRegion () {
        this.highlightRegion = null;
        if (
            this.region &&
            this.transcript &&
            this.transcript.gene &&
            this.transcript.exon
        ) {
            const {gene, exon: exonArray = []} = this.transcript;
            for (let i = 0; i < exonArray.length; i++) {
                const ex = exonArray[i];
                const absoluteStartIndex = ex.start + gene.startIndex;
                const absoluteEndIndex = ex.end + gene.startIndex;
                if ((this.region.startIndex >= absoluteStartIndex && this.region.startIndex <= absoluteEndIndex) ||
                    (this.region.endIndex >= absoluteStartIndex && this.region.endIndex <= absoluteEndIndex) ||
                    (this.region.startIndex < absoluteStartIndex && this.region.endIndex > absoluteEndIndex)) {
                    ex.affectedRange = {
                        start: Math.max(this.region.startIndex - gene.startIndex, ex.start),
                        end: Math.min(this.region.endIndex - gene.startIndex, ex.end)
                    };
                }
                else {
                    ex.affectedRange = null;
                }
            }
            let length = 0;
            let min = null;
            let max = null;
            let affectedMin = null;
            let affectedMax = null;
            for (let i = 0; i < exonArray.length; i++) {
                const ex = exonArray[i];
                ex.relative = {
                    start: length,
                    end: length + (ex.end - ex.start)
                };
                if (!min || min > ex.relative.start) {
                    min = ex.relative.start;
                }
                if (!max || max < ex.relative.end) {
                    max = ex.relative.end;
                }
                length += (ex.end - ex.start) + 1;
                if (ex.affectedRange) {
                    ex.affectedRelativeRange = {
                        start: ex.affectedRange.start + (ex.relative.start - ex.start),
                        end: ex.affectedRange.end + (ex.relative.start - ex.start)
                    };
                    if (!affectedMin || affectedMin > ex.affectedRelativeRange.start) {
                        affectedMin = ex.affectedRelativeRange.start;
                    }
                    if (!affectedMax || affectedMax < ex.affectedRelativeRange.end) {
                        affectedMax = ex.affectedRelativeRange.end;
                    }
                }
            }
            this.highlightRegion = {
                start: Math.floor(affectedMin / 3),
                end: Math.floor(affectedMax / 3)
            };
        }
        const chainInfos = [];
        if (this.highlightRegion && this.position) {
            const positionInfos = this.position.split(',');
            for (let i = 0; i < positionInfos.length; i++) {
                const info = positionInfos[i].trim();
                const chains = info.split('=')[0].split('/');
                const rangeStart = +info.split('=')[1].split('-')[0];
                const rangeEnd = +info.split('=')[1].split('-')[1];
                if ((this.highlightRegion.start >= rangeStart && this.highlightRegion.start <= rangeEnd) ||
                    (this.highlightRegion.end >= rangeStart && this.highlightRegion.end <= rangeEnd) ||
                    (this.highlightRegion.start < rangeStart && this.highlightRegion.end > rangeEnd)) {
                    for (let j = 0; j < chains.length; j++) {
                        chainInfos.push({
                            chainId: chains[j],
                            start: Math.max(this.highlightRegion.start, rangeStart) - rangeStart + 1,
                            end: Math.min(this.highlightRegion.end, rangeEnd) - rangeStart + 1
                        });
                    }
                }
            }
        }
        this.chainInfos = chainInfos.slice();
    }

    setChainMaterials() {
        if (!this.viewer) {
            return;
        }
        const repCount = this.viewer.repCount();
        for (let i = repCount - 1; i >= 0; i--) {
            this.viewer.repRemove(i);
        }
        const setRepresentation = (rep, index) => {
            // Miew does not allow to remove all representations,
            // at least 1 will persist. At this case,
            // we should update first representation (index === 0)
            // and create other ones.
            if (index === 0) {
                this.viewer.rep(index, rep);
            }
            else {
                this.viewer.repAdd(rep);
            }
        };
        if (this.chains && this.chains.length) {
            const chainIsSelected = entity => !this.chainId ||
                /^all$/i.test(this.chainId) ||
                entity.chainId === this.chainId;
            this.chains.forEach((entity, index) => {
                const selector = `chain ${entity.chainId}`;
                const mode = this.displayMode || 'CA';
                const colorer = this.displayColor || 'SS';
                const material = chainIsSelected(entity)
                    ? 'SF'
                    : 'GL';
                setRepresentation({selector, mode, colorer, color: null, material}, index);
            });
            if (this.chainInfos.length > 0) {
                this.chains.forEach((entity) => {
                    for (let i = 0; i < this.chainInfos.length; i++) {
                        const chainInfo = this.chainInfos[i];
                        if (chainInfo.chainId === entity.chainId) {
                            const selector = `chain ${entity.chainId} and sequence ${chainInfo.start}:${chainInfo.end}`;
                            const mode = this.displayMode || 'CA';
                            const colorer = this.displayColor || 'UN';
                            const material = chainIsSelected(entity)
                                ? 'SF'
                                : 'GL';
                            setRepresentation({selector, mode, colorer, color: '', material});
                        }
                    }
                });
            }

            setRepresentation({
                selector: 'hetatm and not water',
                mode: this.displayMode || 'BS',
                colorer: this.displayColor || 'EL',
                material: 'SF'
            });
        }
    }
}
