import Miew from  '../../../compat/miew';
import  angular from 'angular';

export default  {
    template: '<div class="miew-container" data-miew="menu=0&theme=light&themes.light=0xFFFFFF&fog=0&axes=0&fps=0&singleUnit=0"></div>',
    bindings: {
        pdb: '<',
        chainId: '<',
        description: '<',
        region: '<'
    },
    controller: function ($element, $window, $scope) {
        const minHeight = 150;

        $scope.$watch('$ctrl.chainId', () => {
            if (this.parsingFinished) {
                let chainId = this.chainId;
                if (chainId === 'All') {
                    chainId = '';
                }
                setChainMaterials(chainId, this.description.entities);
            }
        });

        angular.element($window).on('resize', () => {
            $element.height(minHeight);
            setHeight();
            viewer._onResize();

        });

        setHeight();

        const container = $element.find('.miew-container');

        const viewer = new Miew(Object.assign({},
            {
                singleUnit: false,
                container: container[0]
            },
            Miew.optionsAttr(container[0].getAttribute('data-miew'))
        ));
        let highlightRegion = null;
        if (this.region) {
            for (let i = 0; i < this.pdb.transcript.exon.length; i++) {
                const ex = this.pdb.transcript.exon[i];
                const absoluteStartIndex = ex.start + this.pdb.transcript.gene.startIndex;
                const absoluteEndIndex = ex.end + this.pdb.transcript.gene.startIndex;
                if ((this.region.startIndex >= absoluteStartIndex && this.region.startIndex <= absoluteEndIndex) ||
                    (this.region.endIndex >= absoluteStartIndex && this.region.endIndex <= absoluteEndIndex) ||
                    (this.region.startIndex < absoluteStartIndex && this.region.endIndex > absoluteEndIndex)) {
                    ex.affectedRange = {
                        start: Math.max(this.region.startIndex - this.pdb.transcript.gene.startIndex, ex.start),
                        end: Math.min(this.region.endIndex - this.pdb.transcript.gene.startIndex, ex.end)
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
            for (let i = 0; i < this.pdb.transcript.exon.length; i++) {
                const ex = this.pdb.transcript.exon[i];
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
            highlightRegion = {
                start: Math.floor(affectedMin / 3),
                end: Math.floor(affectedMax / 3)
            };
        }
        const chainInfos = [];
        if (highlightRegion) {
            const positionInfos = this.pdb.position.split(',');
            for (let i = 0; i < positionInfos.length; i++) {
                const info = positionInfos[i].trim();
                const chains = info.split('=')[0].split('/');
                const rangeStart = +info.split('=')[1].split('-')[0];
                const rangeEnd = +info.split('=')[1].split('-')[1];
                if ((highlightRegion.start >= rangeStart && highlightRegion.start <= rangeEnd) ||
                    (highlightRegion.end >= rangeStart && highlightRegion.end <= rangeEnd) ||
                    (highlightRegion.start < rangeStart && highlightRegion.end > rangeEnd)) {
                    for (let j = 0; j < chains.length; j++) {
                        chainInfos.push({
                            chainId: chains[j],
                            start: Math.max(highlightRegion.start, rangeStart) - rangeStart + 1,
                            end: Math.min(highlightRegion.end, rangeEnd) - rangeStart + 1
                        });
                    }
                }
            }
        }

        if (viewer.init()) {
            viewer.load(this.pdb.id);
            viewer.run();
        }
        viewer.addEventListener('parsingFinished', () => {
            this.parsingFinished = true;
            let chainId = this.chainId;
            if (chainId === 'All') {
                chainId = '';
            }
            setChainMaterials(chainId, this.description.entities);
        });


        function setChainMaterials(chainId, entities) {
            const repCount = viewer.repCount();
            for (let i = repCount - 1; i >= 0; i--) {
                viewer.repRemove(i);
            }
            const setRepresentation = (rep, index) => {
                // Miew does not allow to remove all representations,
                // at least 1 will persist. At this case,
                // we should update first representation (index === 0)
                // and create other ones.
                if (index === 0) {
                    viewer.rep(index, rep);
                }
                else {
                    viewer.repAdd(rep);
                }
            };
            if (entities) {
                entities.forEach((entity, index) => {
                    const selector = `chain ${  entity.chainId}`;
                    const mode = 'CARTOON_MODE';
                    const colorer = 'SECONDARY_STRUCTURE_COLORER';
                    const material = (!chainId || entity.chainId === chainId) ? 'MATERIAL_SOFT' : 'MATERIAL_GLASS';
                    setRepresentation({selector, mode, colorer, color: null, material}, index);
                });
                if (chainInfos.length > 0) {
                    entities.forEach((entity) => {
                        for (let i = 0; i < chainInfos.length; i++) {
                            const chainInfo = chainInfos[i];
                            if (chainInfo.chainId === entity.chainId) {
                                const selector = `chain ${  entity.chainId  } and sequence ${  chainInfo.start  }:${  chainInfo.end}`;
                                const mode = 'CARTOON_MODE';
                                const colorer = 'UNIFORM_COLORER';
                                const material = (!chainId || entity.chainId === chainId) ? 'MATERIAL_SOFT' : 'MATERIAL_GLASS';
                                setRepresentation({selector, mode, colorer, color: '', material});
                            }
                        }
                    });
                }

                setRepresentation({
                    selector: 'hetatm and not water',
                    mode: 'BALLS_AND_STICKS_MODE',
                    colorer: 'ATOM_TYPE_COLORER',
                    material: 'MATERIAL_SOFT'
                });
            }

        }

        function setHeight() {
            const margin = 16;
            const parentH = $element.closest('md-content ').height();
            let calcHeight = parentH - $element[0].offsetTop - margin;

            if (calcHeight < minHeight)
                calcHeight = minHeight;
            $element.height(calcHeight);
        }

    }
};