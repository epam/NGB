import VcfAnalyzer from '../../../../../dataServices/vcf/vcf-analyzer';
import {VcfDataService, ProjectDataService, GenomeDataService, GeneDataService} from '../../../../../dataServices';
import GeneFeatureAnalyzer from '../../../../../dataServices/gene/gene-feature-analyzer';
import {utilities} from '../utilities';
import {Sorting} from '../../../../../modules/render/utilities';
const Math = window.Math;

export default class ngbVariantVisualizerService {
    static instance(dispatcher, constants, vcfDataService, projectContext, projectDataService, genomeDataService, geneDataService) {
        return new ngbVariantVisualizerService(dispatcher, constants, vcfDataService, projectContext, projectDataService, genomeDataService, geneDataService);
    }

    projectContext;

    _constants;
    _vcfDataService: VcfDataService;
    _projectDataService: ProjectDataService;
    _genomeDataService: GenomeDataService;
    _geneDataService: GeneDataService;
    _dispatcher;

    _referenceId;
    _chromosome;
    _project;
    _genesFiles: Array;

    constructor(dispatcher, constants, vcfDataService, projectContext, projectDataService, genomeDataService, geneDataService) {
        this.projectContext = projectContext;
        this._vcfDataService = vcfDataService;
        this._projectDataService = projectDataService;
        this._genomeDataService = genomeDataService;
        this._geneDataService = geneDataService;
        this._constants = constants;
        this._dispatcher = dispatcher;
        this._genesFiles = [];
        this._project = null;
        this._chromosome = null;
    }

    get project() {
        return this._project;
    }

    set project(project) {
        this._project = project;
    }

    get referenceId() {
        return this._referenceId;
    }

    set referenceId(ref) {
        this._referenceId = ref;
    }

    get chromosome() {
        return this._chromosome;
    }

    set chromosome(chromosome) {
        this._chromosome = chromosome;
    }

    get genesFiles(): Array {
        return this._genesFiles;
    }

    loadChromosomeData(chromosomeId) {
        const [chr] = this.projectContext.chromosomes.filter(c => c.id === chromosomeId);
        this.chromosome = chr;
    }

    loadProjectData() {
        if (!this.projectContext.reference) {
            return;
        }
        this.referenceId = this.projectContext.reference.id;
        this._genesFiles = this.projectContext.reference.geneFile ? [this.projectContext.reference.geneFile] : this.projectContext.geneTracks;
    }

    async preAnalyze(variant) {
        try {
            this.loadChromosomeData(variant.chromosomeId);
            this.loadProjectData();
            if (this.genesFiles.length === 0) {
                return {
                    error: 'No genes file is available'
                };
            }
            return {
                geneFiles: this.genesFiles,
                selectedGeneFile: this.genesFiles.length > 0 ? this.genesFiles[0] : null
            };
        } catch(errorObj) {
            return {
                error: 'Error loading variant info'
            };
        }
    }

    async analyze(variant, selectedGeneFile) {
        try {
            this.loadChromosomeData(variant.chromosomeId);
            this.loadProjectData();

            const variantData = await this._vcfDataService.getVariantInfo({
                id: variant.openByUrl ? undefined : variant.vcfFileId,
                openByUrl: variant.openByUrl,
                fileUrl: variant.openByUrl ? variant.fileUrl : undefined,
                indexUrl: variant.openByUrl ? variant.indexUrl : undefined,
                chromosomeId: variant.chromosomeId,
                position: variant.position
            });
            if (!variantData) {
                return {
                    error: 'Error loading variant info'
                };
            }
            const analyzedVariant = VcfAnalyzer.analyzeVariant(variantData, this.chromosome.name);
            if (analyzedVariant.structural || analyzedVariant.interChromosome) {
                return await this.analyzeStructuralVariant(analyzedVariant, selectedGeneFile);
            }
            else {
                return await this.analyzeShortVariant(analyzedVariant, selectedGeneFile);
            }
        } catch(errorObj) {
            return {
                error: 'Error loading variant info'
            };
        }
    }

    async analyzeStructuralVariant(variant, selectedGeneFile) {
        const supportedVariants = ['inv', 'bnd', 'del', 'dup'];

        if (supportedVariants.indexOf(variant.type.toLowerCase()) === -1) {
            return {error: `'${  variant.type  }' variant visualization is not supported.`};
        }
        let breakpoints = [];
        const chromosomes = [];
        const genes = {};
        const geneNames = [];
        const domainNames = [];
        const domainColors = {};
        const variantConnections = [];
        let duplicatedGenes = null; // for 'dup' variant only
        if (!variant.interChromosome && variant.type.toLowerCase() === 'inv') {
            breakpoints = [
                {position: variant.startIndex, chromosome: this.chromosome},
                {position: variant.endIndex, chromosome: this.chromosome}
            ];
            variantConnections.push({
                start: {breakpointIndex: 0, attachedAtRight: true},
                end: {breakpointIndex: 1, attachedAtRight: true}
            });
            variantConnections.push({
                start: {breakpointIndex: 1, attachedAtRight: false},
                end: {breakpointIndex: 0, attachedAtRight: false}
            });
        }
        else if (!variant.interChromosome && variant.type.toLowerCase() === 'dup') {
            breakpoints = [
                {position: variant.startIndex, chromosome: this.chromosome},
                {position: variant.endIndex, chromosome: this.chromosome}
            ];
            variantConnections.push({
                start: {breakpointIndex: 0, attachedAtRight: false},
                end: {breakpointIndex: 1, attachedAtRight: true}
            });
            const duplicatedGenesRequestThresholdRange = 100000000;
            const endIndexCorrected = Math.min(variant.endIndex, variant.startIndex + duplicatedGenesRequestThresholdRange);
            const genesInRange = await this._geneDataService.loadGeneTrack({
                startIndex: variant.startIndex,
                endIndex: endIndexCorrected,
                scaleFactor: 0.001,
                chromosomeId: this.chromosome.id,
                id: selectedGeneFile.id
            }, this.referenceId);
            duplicatedGenes = genesInRange.filter(gene => gene.feature.toLowerCase() === 'gene').map(gene => GeneFeatureAnalyzer.updateFeatureName(gene).name);
        }
        else if (!variant.interChromosome && variant.type.toLowerCase() === 'del') {
            breakpoints = [
                {position: variant.startIndex, chromosome: this.chromosome},
                {position: variant.endIndex, chromosome: this.chromosome}
            ];
            variantConnections.push({
                start: {breakpointIndex: 0, attachedAtRight: true},
                end: {breakpointIndex: 1, attachedAtRight: false}
            });
        }
        else {
            breakpoints.push({position: variant.startIndex, chromosome: this.chromosome});
            for (let i = 0; i < variant.alternativeAllelesInfo.length; i++) {
                const altAll = variant.alternativeAllelesInfo[i];
                if (altAll.mate) {
                    breakpoints.push({
                        position: altAll.mate.position,
                        chromosome: {name: altAll.mate.chromosome}
                    });
                    variantConnections.push({
                        start: {
                            breakpointIndex: 0,
                            attachedAtRight: altAll.mate.attachedAt === 'right'
                        }, end: {breakpointIndex: breakpoints.length - 1, attachedAtRight: altAll.mate.reverseComp}
                    });
                }
            }
        }
        breakpoints = Sorting.quickSort(breakpoints, true, x => x.position);

        if (selectedGeneFile) {
            for (let i = 0; i < breakpoints.length; i++) {
                breakpoints[i].affectedGenes = await this.getAffectedGenes(breakpoints[i], selectedGeneFile.id);
                if (!breakpoints[i].affectedGenes || breakpoints[i].affectedGenes.length === 0) {
                    breakpoints[i].affectedGenes = this.getEmptyAffectedGenes(i);
                }
                if (breakpoints[i].affectedGenes.length > 0)
                    breakpoints[i].affectedGene = breakpoints[i].affectedGenes[0];
            }
        }

        if (duplicatedGenes) {
            for (let i = 0; i < breakpoints.length; i++) {
                if (breakpoints[i].affectedGenes) {
                    for (let j = 0; j < breakpoints[i].affectedGenes.length; j++) {
                        const geneName = breakpoints[i].affectedGenes[j].name || GeneFeatureAnalyzer.updateFeatureName(breakpoints[i].affectedGenes[j]).name;
                        const ind = duplicatedGenes.indexOf(geneName);
                        if (ind >= 0) {
                            duplicatedGenes.splice(ind, 1);
                        }
                    }
                }
            }
        }
        for (let i = 0; i < breakpoints.length; i++) {
            if (chromosomes.indexOf(breakpoints[i].chromosome.name.toLowerCase()) === -1) {
                chromosomes.push(breakpoints[i].chromosome.name.toLowerCase());
            }
            if (breakpoints[i].affectedGene && !breakpoints[i].affectedGene.empty) {
                for (let t = 0; t < breakpoints[i].affectedGene.transcripts.length; t++) {
                    if (breakpoints[i].affectedGene.transcripts[t].domain) {
                        for (let d = 0; d < breakpoints[i].affectedGene.transcripts[t].domain.length; d++) {
                            if (domainNames.indexOf(breakpoints[i].affectedGene.transcripts[t].domain[d].name) === -1) {
                                domainNames.push(breakpoints[i].affectedGene.transcripts[t].domain[d].name);
                                domainColors[breakpoints[i].affectedGene.transcripts[t].domain[d].name] = {
                                    index: domainNames.length - 1
                                };
                            }
                        }
                    }
                }
            }
            if (breakpoints[i].affectedGene !== undefined && breakpoints[i].affectedGene !== null) {
                if (geneNames.indexOf(breakpoints[i].affectedGene.name) === -1) {
                    geneNames.push(breakpoints[i].affectedGene.name);
                    genes[breakpoints[i].affectedGene.name] = {
                        gene: breakpoints[i].affectedGene,
                        chromosome: breakpoints[i].chromosome
                    };
                }
            }
        }

        const altInfos = [];

        if (variant.type.toLowerCase() === 'inv') {
            altInfos.push(this.recoverAlternativeExonStructure(variantConnections[0], breakpoints));
            altInfos.push(this.recoverAlternativeExonStructure(variantConnections[1], breakpoints, true));
        }
        else if (variant.type.toLowerCase() === 'dup') {
            altInfos.push(...this.recoverAlternativeExonStructureForDuplication(breakpoints, variantConnections[0]));
        }
        else {
            for (let i = 0; i < variantConnections.length; i++) {
                altInfos.push(this.recoverAlternativeExonStructure(variantConnections[i], breakpoints));
            }
        }

        return {
            variantInfo: variant,
            chromosomes,
            analysisResult: {
                breakpoints,
                chromosomes,
                genes,
                geneNames,
                duplicatedGeneNames: (duplicatedGenes && duplicatedGenes.length > 0) ? utilities.trimArray(duplicatedGenes) : null,
                variantConnections,
                altInfos,
                domainColors
            },
            geneFile: selectedGeneFile,
        };
    }

    rebuildStructuralVariantVisualizerData(visualizerData) {
        const geneNames = [];
        const genes = {};
        const domainNames = [];
        const domainColors = {};
        const breakpoints = visualizerData.analysisResult.breakpoints;
        for (let i = 0; i < breakpoints.length; i++) {
            if (breakpoints[i].affectedGene !== undefined && breakpoints[i].affectedGene !== null) {
                if (geneNames.indexOf(breakpoints[i].affectedGene.name) === -1) {
                    geneNames.push(breakpoints[i].affectedGene.name);
                    genes[breakpoints[i].affectedGene.name] = {
                        gene: breakpoints[i].affectedGene,
                        chromosome: breakpoints[i].chromosome
                    };
                }
                if (breakpoints[i].affectedGene.transcripts) {
                    for (let t = 0; t < breakpoints[i].affectedGene.transcripts.length; t++) {
                        if (breakpoints[i].affectedGene.transcripts[t].domain) {
                            for (let d = 0; d < breakpoints[i].affectedGene.transcripts[t].domain.length; d++) {
                                if (domainNames.indexOf(breakpoints[i].affectedGene.transcripts[t].domain[d].name) === -1) {
                                    domainNames.push(breakpoints[i].affectedGene.transcripts[t].domain[d].name);
                                    domainColors[breakpoints[i].affectedGene.transcripts[t].domain[d].name] = {
                                        index: domainNames.length - 1
                                    };
                                }
                            }
                        }
                    }
                }
            }
        }

        const altInfos = [];
        if (visualizerData.variantInfo.type.toLowerCase() === 'inv') {
            altInfos.push(this.recoverAlternativeExonStructure(visualizerData.analysisResult.variantConnections[0], breakpoints));
            altInfos.push(this.recoverAlternativeExonStructure(visualizerData.analysisResult.variantConnections[1], breakpoints, true));
        }
        else if (visualizerData.variantInfo.type.toLowerCase() === 'dup') {
            altInfos.push(...this.recoverAlternativeExonStructureForDuplication(breakpoints, visualizerData.analysisResult.variantConnections[0]));
        }
        else {
            for (let i = 0; i < visualizerData.analysisResult.variantConnections.length; i++) {
                altInfos.push(this.recoverAlternativeExonStructure(visualizerData.analysisResult.variantConnections[i], breakpoints));
            }
        }

        visualizerData.analysisResult.genes = genes;
        visualizerData.analysisResult.geneNames = geneNames;
        visualizerData.analysisResult.altInfos = altInfos;
        visualizerData.analysisResult.domainColors = domainColors;

        return visualizerData;
    }

    async getAffectedGenes(breakpoint, geneFileId) {
        const fakeRange = 1;
        if (breakpoint.chromosome.id === null || breakpoint.chromosome.id === undefined) {
            const chromosome = await this._genomeDataService.loadChromosomeByName(this.referenceId, breakpoint.chromosome.name);
            if (!chromosome)
                return [];
            breakpoint.chromosome = chromosome;
        }
        const range = {
            start: Math.max(1, Math.round(breakpoint.position - fakeRange)),
            end: Math.min(breakpoint.chromosome.size, Math.round(breakpoint.position + fakeRange))
        };
        let data = await this._geneDataService.loadGeneTranscriptTrack({
            id: geneFileId,
            chromosomeId: breakpoint.chromosome.id,
            startIndex: range.start,
            endIndex: range.end,
            scaleFactor: 1
        }, this.referenceId);
        if (data !== null && data !== undefined && data.length > 0) {
            data = data.filter(x => !x.status || x.status.toLowerCase() === 'ok');
        }
        for (let i = 0; i < data.length; i++) {
            this.getAffectedGeneInfo(breakpoint, data[i]);
        }
        return data;
    }

    getEmptyAffectedGenes(index) {
        return [{empty: true, name: `empty_${  index}`}];
    }

    getAffectedGeneInfo(breakpoint, gene) {
        if (gene) {
            if (gene.attributes && gene.attributes.hasOwnProperty('gene_symbol')) {
                gene.name = gene.attributes.gene_symbol;
            }
            else if (gene.attributes && gene.attributes.hasOwnProperty('gene_name')) {
                gene.name = gene.attributes.gene_name;
            }
            else if (gene.attributes && gene.attributes.hasOwnProperty('gene_id')) {
                gene.name = gene.attributes.gene_id;
            }
            else {
                gene.name = gene.name || '';
            }
            if (gene.transcripts) {
                for (let i = 0; i < gene.transcripts.length; i++) {
                    for (let j = 0; j < gene.transcripts[i].exon.length; j++) {
                        gene.transcripts[i].exon[j].geneName = gene.name;
                    }
                }

                gene.consensusDomains = this.recoverConsensusDomains(gene.transcripts);
                gene.consensusExons = this.recoverConsensusTranscript(gene.transcripts, gene.consensusDomains, gene.strand);
                gene.totalExonsLength = gene.consensusExons.reduce((a, b) => (a instanceof Object ? (a.end - a.start) : a) + (b.end - b.start));
                for (let i = 0; i < gene.consensusExons.length; i++) {
                    const exon = gene.consensusExons[i];
                    exon.strand = gene.strand ? gene.strand.toLowerCase() === 'positive' : null;
                    exon.index = (gene.strand && gene.strand.toLowerCase() === 'negative') ? (gene.consensusExons.length - i - 1) : i;
                }
                if (!breakpoint.relativePositions) {
                    breakpoint.relativePositions = {};
                }
                let relativePositionIsSet = false;
                for (let i = 0; i < gene.consensusExons.length; i++) {
                    const exon = gene.consensusExons[i];
                    if (exon.end + gene.startIndex > breakpoint.position) {
                        if (exon.start + gene.startIndex > breakpoint.position) {
                            breakpoint.relativePositions[gene.name] = exon.relativePosition.start;
                        }
                        else {
                            breakpoint.relativePositions[gene.name] = breakpoint.position - gene.startIndex - exon.start + exon.relativePosition.start;
                        }
                        relativePositionIsSet = true;
                        break;
                    }
                }
                if (!relativePositionIsSet && gene.consensusExons && gene.consensusExons.length > 0) {
                    breakpoint.relativePositions[gene.name] = gene.consensusExons[gene.consensusExons.length - 1].relativePosition.end;
                }
            }
            else {
                gene.empty = true;
            }
            return gene;
        }
        return null;
    }

    recoverConsensusTranscript(transcripts, domains, strand) {
        const exons = [];
        let position = 0;
        let exonFound = true;
        for (let i = 0; i < transcripts.length; i++) {
            transcripts[i].cds = transcripts[i].exon;
        }
        while (exonFound) {
            let nextExon = null;
            for (let i = 0; i < transcripts.length; i++) {
                if (transcripts[i].cds === undefined || transcripts[i].cds === null)
                    continue;
                for (let j = 0; j < transcripts[i].cds.length; j++) {
                    if (transcripts[i].cds[j].start >= position) {
                        if (!nextExon || nextExon.start > transcripts[i].cds[j].start || (nextExon.start === transcripts[i].cds[j].start && nextExon.end < transcripts[i].cds[j].end)) {
                            nextExon = Object.assign({}, transcripts[i].cds[j]);
                        }
                    }
                }
            }
            exonFound = nextExon !== null;
            if (exonFound) {
                exons.push(nextExon);
                position = nextExon.end + 1;
            }
        }
        let prevPosition = 0;
        for (let i = 0; i < exons.length; i++) {
            const exon = exons[i];
            exon.domains = [];
            exon.relativePosition = {
                start: prevPosition,
                end: prevPosition + (exon.end - exon.start)
            };
            prevPosition += (exon.end - exon.start);
            for (let j = 0; j < domains.length; j++) {
                if ((domains[j].start >= exon.relativePosition.start && domains[j].start <= exon.relativePosition.end) ||
                    (domains[j].end >= exon.relativePosition.start && domains[j].end <= exon.relativePosition.end) ||
                    (domains[j].start <= exon.relativePosition.start && domains[j].end >= exon.relativePosition.end)) {
                    exon.domains.push({
                        domain: domains[j],
                        domainIndex: j,
                        range: {
                            start: Math.max(domains[j].start, exon.relativePosition.start),
                            end: Math.min(domains[j].end, exon.relativePosition.end)
                        }
                    });
                }
            }
        }
        const findConsensusExon = (exon) => {
            for (let i = 0; i < exons.length; i++) {
                if (exon.start >= exons[i].start && exon.start <= exons[i].end) {
                    return exons[i];
                }
            }
            return null;
        };
        for (let i = 0; i < transcripts.length; i++) {
            transcripts[i].cds = Sorting.quickSort(transcripts[i].cds, true, x => x.start);
            for (let j = 0; j < transcripts[i].cds.length; j++) {
                transcripts[i].cds[j].strand = strand;
                if (transcripts[i].cds[j].strand && transcripts[i].cds[j].strand.toLowerCase() === 'positive') {
                    transcripts[i].cds[j].index = j;
                }
                else if (transcripts[i].cds[j].strand && transcripts[i].cds[j].strand.toLowerCase() === 'negative') {
                    transcripts[i].cds[j].index = transcripts[i].cds.length - j - 1;
                }
                const consensusExon = findConsensusExon(transcripts[i].cds[j]);
                if (consensusExon) {
                    const diffFromStart = transcripts[i].cds[j].start - consensusExon.start;
                    transcripts[i].cds[j].relativePosition = {
                        start: consensusExon.relativePosition.start + diffFromStart,
                        end: consensusExon.relativePosition.start + diffFromStart + (transcripts[i].cds[j].end - transcripts[i].cds[j].start)
                    };
                }
            }
        }
        for (let t = 0; t < transcripts.length; t++) {
            let prevPosition = 0;
            for (let i = 0; i < transcripts[t].cds.length; i++) {
                const exon = transcripts[t].cds[i];
                exon.domains = [];
                exon.structurePosition = {
                    start: prevPosition,
                    end: prevPosition + (exon.relativePosition.end - exon.relativePosition.start)
                };
                prevPosition += (exon.relativePosition.end - exon.relativePosition.start);
                const diffFromStart = exon.relativePosition.start - exon.structurePosition.start;
                if (transcripts[t].domain) {
                    for (let j = 0; j < transcripts[t].domain.length; j++) {
                        const domain = transcripts[t].domain[j];
                        if ((domain.start >= exon.structurePosition.start && domain.start <= exon.structurePosition.end) ||
                            (domain.end >= exon.structurePosition.start && domain.end <= exon.structurePosition.end) ||
                            (domain.start <= exon.structurePosition.start && domain.end >= exon.structurePosition.end)) {
                            exon.domains.push({
                                domain: domain,
                                range: {
                                    start: diffFromStart + Math.max(domain.start, exon.structurePosition.start),
                                    end: diffFromStart + Math.min(domain.end, exon.structurePosition.end)
                                }
                            });
                        }
                    }
                }
            }
        }
        return exons;
    }

    recoverConsensusDomains(transcripts) {
        const domains = [];
        let position = 0;
        let domainFound = true;
        while (domainFound) {
            let nextDomain = null;
            for (let i = 0; i < transcripts.length; i++) {
                if (transcripts[i].domain === undefined || transcripts[i].domain === null)
                    continue;
                for (let j = 0; j < transcripts[i].domain.length; j++) {
                    if (transcripts[i].domain[j].start >= position) {
                        if (!nextDomain || nextDomain.start > transcripts[i].domain[j].start || (nextDomain.start === transcripts[i].domain[j].start && nextDomain.end > transcripts[i].domain[j].end)) {
                            nextDomain = transcripts[i].domain[j];
                        }
                    }
                }
            }
            domainFound = nextDomain !== null;
            if (domainFound) {
                domains.push(nextDomain);
                position = nextDomain.end + 1;
            }
        }
        return domains;
    }

    recoverAlternativeExonStructure(variantConnection, breakpoints, reverse = false) {
        const left = breakpoints[variantConnection.start.breakpointIndex].affectedGene.empty
            ? [this.copyEmptyGene(breakpoints[variantConnection.start.breakpointIndex].affectedGene)]
            : this.splitExons(breakpoints[variantConnection.start.breakpointIndex].affectedGene.consensusExons,
            breakpoints[variantConnection.start.breakpointIndex].relativePositions[breakpoints[variantConnection.start.breakpointIndex].affectedGene.name],
            breakpoints.filter(x => x.relativePositions).map(x => x.relativePositions[breakpoints[variantConnection.start.breakpointIndex].affectedGene.name]),
            variantConnection.start.attachedAtRight, reverse ? variantConnection.start.attachedAtRight : !variantConnection.start.attachedAtRight);
        const right = breakpoints[variantConnection.end.breakpointIndex].affectedGene.empty
            ? [this.copyEmptyGene(breakpoints[variantConnection.end.breakpointIndex].affectedGene)]
            : this.splitExons(breakpoints[variantConnection.end.breakpointIndex].affectedGene.consensusExons,
            breakpoints[variantConnection.end.breakpointIndex].relativePositions[breakpoints[variantConnection.end.breakpointIndex].affectedGene.name],
            breakpoints.filter(x => x.relativePositions).map(x => x.relativePositions[breakpoints[variantConnection.end.breakpointIndex].affectedGene.name]),
            variantConnection.end.attachedAtRight, reverse ? !variantConnection.end.attachedAtRight : variantConnection.end.attachedAtRight);
        const merge = reverse ? this.mergeExonsSequences(right, left) : this.mergeExonsSequences(left, right);
        if (breakpoints[variantConnection.start.breakpointIndex].chromosome.id === breakpoints[variantConnection.end.breakpointIndex].chromosome.id) {
            merge.chromosome = breakpoints[variantConnection.start.breakpointIndex].chromosome;
        }
        else {
            merge.chromosome = null;
        }
        let length = 0;
        for (let i = 0; i < merge.consensusExons.length; i++) {
            length += (merge.consensusExons[i].relativePosition.end - merge.consensusExons[i].relativePosition.start);
        }
        merge.totalExonsLength = length;
        return merge;
    }

    duplicateExonSrtructure(exonStructure) {
        const duplicatedExonStructure = [];
        for (let i = 0; i < exonStructure.length; i++) {
            const duplicatedExon = Object.assign({}, exonStructure[i]);
            duplicatedExon.domains = [];
            if (exonStructure[i].domains) {
                for (let j = 0; j < exonStructure[i].domains.length; j++) {
                    const domain = Object.assign({}, exonStructure[i].domains[j]);
                    domain.range = {
                        start: exonStructure[i].domains[j].range.start,
                        end: exonStructure[i].domains[j].range.end
                    };
                    duplicatedExon.domains.push(domain);
                }
            }
            if (exonStructure[i].relativePosition) {
                duplicatedExon.relativePosition = {
                    start: exonStructure[i].relativePosition.start,
                    end: exonStructure[i].relativePosition.end
                };
            }
            duplicatedExonStructure.push(duplicatedExon);
        }
        return duplicatedExonStructure;
    }

    recoverAlternativeExonStructureForDuplication(breakpoints, variantConnection) {
        const leftOuter = breakpoints[variantConnection.start.breakpointIndex].affectedGene.empty
            ? [this.copyEmptyGene(breakpoints[variantConnection.start.breakpointIndex].affectedGene)]
            : this.splitExons(breakpoints[variantConnection.start.breakpointIndex].affectedGene.consensusExons,
            breakpoints[variantConnection.start.breakpointIndex].relativePositions[breakpoints[variantConnection.start.breakpointIndex].affectedGene.name],
            breakpoints.filter(x => x.relativePositions).map(x => x.relativePositions[breakpoints[variantConnection.start.breakpointIndex].affectedGene.name]),
            !variantConnection.start.attachedAtRight, false);
        const left = breakpoints[variantConnection.start.breakpointIndex].affectedGene.empty
            ? [this.copyEmptyGene(breakpoints[variantConnection.start.breakpointIndex].affectedGene)]
            : this.splitExons(breakpoints[variantConnection.start.breakpointIndex].affectedGene.consensusExons,
            breakpoints[variantConnection.start.breakpointIndex].relativePositions[breakpoints[variantConnection.start.breakpointIndex].affectedGene.name],
            breakpoints.filter(x => x.relativePositions).map(x => x.relativePositions[breakpoints[variantConnection.start.breakpointIndex].affectedGene.name]),
            variantConnection.start.attachedAtRight, false);
        const right = breakpoints[variantConnection.end.breakpointIndex].affectedGene.empty
            ? [this.copyEmptyGene(breakpoints[variantConnection.end.breakpointIndex].affectedGene)]
            : this.splitExons(breakpoints[variantConnection.end.breakpointIndex].affectedGene.consensusExons,
            breakpoints[variantConnection.end.breakpointIndex].relativePositions[breakpoints[variantConnection.end.breakpointIndex].affectedGene.name],
            breakpoints.filter(x => x.relativePositions).map(x => x.relativePositions[breakpoints[variantConnection.end.breakpointIndex].affectedGene.name]),
            variantConnection.end.attachedAtRight, false);
        const rightOuter = breakpoints[variantConnection.end.breakpointIndex].affectedGene.empty
            ? [this.copyEmptyGene(breakpoints[variantConnection.end.breakpointIndex].affectedGene)]
            : this.splitExons(breakpoints[variantConnection.end.breakpointIndex].affectedGene.consensusExons,
            breakpoints[variantConnection.end.breakpointIndex].relativePositions[breakpoints[variantConnection.end.breakpointIndex].affectedGene.name],
            breakpoints.filter(x => x.relativePositions).map(x => x.relativePositions[breakpoints[variantConnection.end.breakpointIndex].affectedGene.name]),
            !variantConnection.end.attachedAtRight, false);

        const structures = [];

        const duplicatedLeft = this.duplicateExonSrtructure(left);
        const duplicatedRight = this.duplicateExonSrtructure(right);

        if (breakpoints[variantConnection.start.breakpointIndex].affectedGene.name !== breakpoints[variantConnection.end.breakpointIndex].affectedGene.name) {
            structures.push(this.mergeExonsSequences(leftOuter, left));
            structures.push(this.mergeExonsSequences(right, duplicatedLeft));
            structures.push(this.mergeExonsSequences(duplicatedRight, rightOuter));
        }
        else {
            structures.push(this.mergeExonsSequences(leftOuter, left, duplicatedLeft, rightOuter));
        }

        for (let i = 0; i < structures.length; i++) {
            if (breakpoints[variantConnection.start.breakpointIndex].chromosome.id === breakpoints[variantConnection.end.breakpointIndex].chromosome.id) {
                structures[i].chromosome = breakpoints[variantConnection.start.breakpointIndex].chromosome;
            }
            else {
                structures[i].chromosome = null;
            }
            let length = 0;
            for (let j = 0; j < structures[i].consensusExons.length; j++) {
                length += (structures[i].consensusExons[j].relativePosition.end - structures[i].consensusExons[j].relativePosition.start);
            }
            structures[i].totalExonsLength = length;
        }
        return structures;
    }

    copyEmptyGene(emptyGene) {
        return {
            name: emptyGene.name,
            empty: true
        };
    }

    splitExons(exons, breakpointPosition, anotherBreakpoints, takeLeftPart = true, revert = false) {
        let leftBorder = 0;
        let rightBorder = 0;
        for (let i = 0; i < exons.length; i++) {
            if (exons[i].relativePosition.end > rightBorder) {
                rightBorder = exons[i].relativePosition.end;
            }
        }
        for (let i = 0; i < anotherBreakpoints.length; i++) {
            if (anotherBreakpoints[i] === breakpointPosition)
                continue;
            if (anotherBreakpoints[i] > leftBorder && anotherBreakpoints[i] < breakpointPosition) {
                leftBorder = anotherBreakpoints[i];
            }
            if (anotherBreakpoints[i] < rightBorder && anotherBreakpoints[i] > breakpointPosition) {
                rightBorder = anotherBreakpoints[i];
            }
        }
        let result = [];
        for (let i = 0; i < exons.length; i++) {
            if (takeLeftPart) {
                if (exons[i].relativePosition.start < breakpointPosition && exons[i].relativePosition.end >= leftBorder) {
                    const exon = Object.assign({}, exons[i]);
                    exon.relativePosition = {
                        start: exons[i].relativePosition.start,
                        end: exons[i].relativePosition.end
                    };
                    exon.relativePosition.start = Math.max(leftBorder, exon.relativePosition.start);
                    exon.relativePosition.end = Math.min(breakpointPosition, exon.relativePosition.end);
                    if (exon.relativePosition.end === breakpointPosition) {
                        exon.isBreakpoint = true;
                        exon.breakpointPosition = revert ? 'start' : 'end';
                    }
                    exon.domains = [];
                    for (let j = 0; j < exons[i].domains.length; j++) {
                        const domain = Object.assign({}, exons[i].domains[j]);
                        domain.range = {
                            start: exons[i].domains[j].range.start,
                            end: exons[i].domains[j].range.end
                        };
                        exon.domains.push(domain);
                    }
                    result.push(exon);
                }
            }
            else {
                if (exons[i].relativePosition.end > breakpointPosition && exons[i].relativePosition.start <= rightBorder) {
                    const exon = Object.assign({}, exons[i]);
                    exon.relativePosition = {
                        start: exons[i].relativePosition.start,
                        end: exons[i].relativePosition.end
                    };
                    exon.relativePosition.start = Math.max(breakpointPosition, exon.relativePosition.start);
                    exon.relativePosition.end = Math.min(rightBorder, exon.relativePosition.end);
                    if (exon.relativePosition.start === breakpointPosition) {
                        exon.isBreakpoint = true;
                        exon.breakpointPosition = revert ? 'end' : 'start';
                    }
                    exon.domains = [];
                    for (let j = 0; j < exons[i].domains.length; j++) {
                        const domain = Object.assign({}, exons[i].domains[j]);
                        domain.range = {
                            start: exons[i].domains[j].range.start,
                            end: exons[i].domains[j].range.end
                        };
                        exon.domains.push(domain);
                    }
                    result.push(exon);
                }
            }
        }
        if (revert) {
            const revertedResult = [];
            for (let i = 0; i < result.length; i++) {
                const exon = result[result.length - 1 - i];
                if (exon.strand !== null)
                    exon.strand = !exon.strand;
                revertedResult.push(exon);
            }
            result = revertedResult;
        }
        let prevPosition = 0;
        for (let i = 0; i < result.length; i++) {
            const exon = result[i];
            const exonLength = exon.relativePosition.end - exon.relativePosition.start;
            for (let j = 0; j < exon.domains.length; j++) {
                const domain = exon.domains[j];
                domain.range.start -= (exon.relativePosition.start - prevPosition);
                domain.range.end -= (exon.relativePosition.start - prevPosition);
            }
            exon.relativePosition.start = prevPosition;
            exon.relativePosition.end = prevPosition + exonLength;
            if (revert) {
                for (let j = 0; j < exon.domains.length; j++) {
                    const domain = exon.domains[j];
                    const domainLength = domain.range.end - domain.range.start;
                    domain.range.end = exon.relativePosition.end - (domain.range.start - exon.relativePosition.start);
                    domain.range.start = domain.range.end - domainLength;
                }
            }
            prevPosition += exonLength + 1;
        }
        return result;
    }

    mergeExonsSequences(...sequences) {

        const emptyGeneLength = 20;

        const result = [];
        let length = 0;

        const sequenceLength = [];

        for (let s = 0; s < sequences.length; s++) {
            const seq = sequences[s];
            let seqLength = 0;
            sequenceLength.push(emptyGeneLength);
            for (let i = 0; i < seq.length; i++) {
                if (!seq[i].empty) {
                    seqLength += (seq[i].relativePosition.end - seq[i].relativePosition.start);
                }
            }
            if (seqLength) {
                sequenceLength[s] = seqLength;
            }
        }
        let prevLength = 0;
        for (let s = 0; s < sequences.length; s++) {
            const seq = sequences[s];
            for (let i = 0; i < seq.length; i++) {
                const exon = seq[i];
                if (exon.empty) {
                    exon.relativePosition = {
                        start: 0,
                        end: sequenceLength[s] / 4
                    };
                }
                length += (seq[i].relativePosition.end - seq[i].relativePosition.start + 1);
                exon.relativePosition.start += prevLength;
                exon.relativePosition.end += prevLength;
                if (exon.domains) {
                    for (let j = 0; j < exon.domains.length; j++) {
                        exon.domains[j].range.start += prevLength;
                        exon.domains[j].range.end += prevLength;
                    }
                }
                result.push(exon);
            }
            prevLength = length;
        }
        return {
            consensusExons: result,
            breakpoint: length
        };
    }

    async analyzeShortVariant(variant, selectedGeneFile) {
        const visibleRangeInBp = 50 + (variant.length || 0); // Preventing empty spaces after resizing renderer with pre-loaded data.
        const range = {
            start: Math.max(1, Math.round(variant.startIndex - visibleRangeInBp / 2)),
            end: Math.min(this.chromosome.size, Math.round(variant.endIndex + visibleRangeInBp / 2))
        };
        range.end = Math.min(this.chromosome.size, range.start + visibleRangeInBp);
        range.start = Math.max(1, range.end - visibleRangeInBp);
        const referenceData = await this._genomeDataService.loadReferenceTrack({
            id: this.referenceId,
            chromosomeId: this.chromosome.id,
            startIndex: range.start,
            endIndex: range.end,
            scaleFactor: 1
        });

        const reference = referenceData.blocks || [];

        if (!reference || reference.length === 0) {
            return {
                error: 'Error loading variant info'
            };
        }

        for (let i = 0; i < variant.alternativeAllelesInfo.length; i++) {
            variant.alternativeAllelesInfo[i].affectedFeatures = [];
        }
        if (selectedGeneFile) {
            // Downloading exon / intron structure
            let data = await this._geneDataService.loadGeneTranscriptTrack({
                id: selectedGeneFile.id,
                chromosomeId: this.chromosome.id,
                startIndex: range.start,
                endIndex: range.end,
                scaleFactor: 1
            }, this.referenceId);
            if (data !== null && data !== undefined && data.length > 0) {
                data = data.filter(x => !x.status || x.status.toLowerCase() === 'ok');
            }
            if (data !== null && data !== undefined && data.length > 0) {
                for (let j = 0; j < variant.alternativeAllelesInfo.length; j++) {
                    const analyzedGeneData = analyzeGeneStructure(data, {
                        startIndex: variant.startIndexCorrected,
                        endIndex: variant.startIndexCorrected + variant.alternativeAllelesInfo[j].info.length
                    });
                    variant.alternativeAllelesInfo[j].affectedFeatures.push({
                        data: analyzedGeneData,
                        file: selectedGeneFile
                    });
                }
            }
        }
        for (let i = 0; i < variant.alternativeAllelesInfo.length; i++) {
            const all = variant.alternativeAllelesInfo[i];
            all.displaySource = utilities.trimString(all.source);
            generateAffectedFeaturesStructure(all);
        }
        variant.displayReferenceAllele = utilities.trimString(variant.referenceAllele);
        return {
            variantInfo: variant,
            reference,
            chromosome: this.chromosome,
            altFields: variant.alternativeAllelesInfo,
            selectedAltField: variant.alternativeAllelesInfo[0],
            geneFile: selectedGeneFile
        };
    }

}

function generateAffectedFeaturesStructure(alternativeAlleleInfo) {
    const structure = {};
    const affectedGenes = [];
    const affectedGenesData = [];
    const affectedTranscripts = [];
    for (let i = 0; i < alternativeAlleleInfo.affectedFeatures.length; i++) {
        for (let j = 0; j < alternativeAlleleInfo.affectedFeatures[i].data.length; j++) {
            const gene = alternativeAlleleInfo.affectedFeatures[i].data[j];
            if (!structure.hasOwnProperty(gene.name)) {
                structure[gene.name] = {
                    __genes__: []
                };
                affectedGenes.push(gene.name);
                affectedGenesData.push(gene);
            }
            structure[gene.name].__genes__.push({
                file: alternativeAlleleInfo.affectedFeatures[i].file.name,
                gene: gene
            });
            for (let tIndex = 0; tIndex < gene.matchedTranscripts.length; tIndex++) {
                const transcript = gene.matchedTranscripts[tIndex];
                if (!structure[gene.name].hasOwnProperty(transcript.name)) {
                    structure[gene.name][transcript.name] = {
                        __transcripts__: []
                    };
                }
                structure[gene.name][transcript.name].__transcripts__.push({
                    file: alternativeAlleleInfo.affectedFeatures[i].file.name,
                    transcript: transcript
                });

                for (let eIndex = 0; eIndex < transcript.matchedExons.length; eIndex++) {
                    const exon = transcript.matchedExons[eIndex];
                    let exonIndex = transcript.exon.indexOf(exon) + 1;
                    if (gene.strand !== null && gene.strand !== undefined && gene.strand.toLowerCase() === 'negative') {
                        exonIndex = transcript.exon.length - exonIndex + 1;
                    }

                    const exonName = `exon_${  exonIndex}`;

                    if (!structure[gene.name][transcript.name].hasOwnProperty(exonName)) {
                        structure[gene.name][transcript.name][exonName] = {
                            order: exonIndex,
                            __exons__: []
                        };
                    }
                    structure[gene.name][transcript.name][exonName].__exons__.push({
                        file: alternativeAlleleInfo.affectedFeatures[i].file.name,
                        exon: exon
                    });

                    if (exon.matchedDomains === null || exon.matchedDomains === undefined || exon.matchedDomains.length === 0) {
                        affectedTranscripts.push({
                            transcript: {name: transcript.name, value: transcript},
                            exon: {name: exonIndex, value: exon},
                            domain: null,
                            gene: {name: gene.name, value: gene}
                        });
                    }

                    for (let dIndex = 0; dIndex < exon.matchedDomains.length; dIndex++) {
                        const domain = exon.matchedDomains[dIndex];
                        if (!structure[gene.name][transcript.name][exonName].hasOwnProperty(domain.name)) {
                            structure[gene.name][transcript.name][exonName][domain.name] = {
                                __domains__: []
                            };
                            affectedTranscripts.push({
                                transcript: {name: transcript.name, value: transcript},
                                exon: {name: exonIndex, value: exon},
                                domain: {name: domain.name, value: domain},
                                gene: {name: gene.name, value: gene}
                            });
                        }
                        structure[gene.name][transcript.name][exonName][domain.name].__domains__.push({
                            file: alternativeAlleleInfo.affectedFeatures[i].file.name,
                            domain: domain
                        });
                    }

                }

            }
        }
    }
    alternativeAlleleInfo.affectedFeaturesStructure = {
        structure,
        affectedGenesData,
        affectedGenes,
        affectedGenesDisplay: utilities.trimStringsInArray(affectedGenes),
        affectedTranscripts
    };
}

function analyzeGeneStructure(features, variant) {
    const matchedFeatures = [];
    if (features !== null && features !== undefined) {
        for (let i = 0; i < features.length; i++) {
            const feature = features[i];
            if (feature.startIndex <= variant.startIndex && feature.endIndex >= variant.endIndex) {
                if (feature.attributes !== null && feature.attributes !== undefined) {
                    if (feature.attributes.hasOwnProperty('gene_symbol')) {
                        feature.name = feature.attributes.gene_symbol;
                    }
                    else if (feature.attributes.hasOwnProperty('gene_name')) {
                        feature.name = feature.attributes.gene_name;
                    }
                    else if (feature.attributes.hasOwnProperty('gene_id')) {
                        feature.name = feature.attributes.gene_id;
                    }
                    else {
                        feature.name = feature.name || '';
                    }
                }
                if (feature.transcripts !== null && feature.transcripts !== undefined) {
                    feature.matchedTranscripts = analyzeTranscriptStructure(feature.transcripts, feature, variant);
                }
                else {
                    feature.matchedTranscripts = analyzeGeneStructure(feature.items, variant);
                }
                matchedFeatures.push(feature);
            }
        }
    }
    return matchedFeatures;
}

function analyzeTranscriptStructure(transcripts, gene, variant) {
    const matchedFeatures = [];
    if (transcripts !== null && transcripts !== undefined) {
        for (let i = 0; i < transcripts.length; i++) {
            const transcript = transcripts[i];
            transcript.exon = extractCdsRegions(transcript.exon, transcript.utr);
            if (((gene.startIndex + transcript.start) <= variant.startIndex && (gene.startIndex + transcript.end) >= variant.startIndex) ||
                ((gene.startIndex + transcript.start) <= variant.endIndex && (gene.startIndex + transcript.end) >= variant.endIndex)) {
                transcript.matchedExons = analyzeExonStructure(transcript.exon, transcript.domain, gene, variant);
                matchedFeatures.push(transcript);
            }
        }
    }
    return matchedFeatures;
}

function analyzeExonStructure(exons, domains, gene, variant) {
    const matchedFeatures = [];
    if (exons !== null && exons !== undefined) {
        for (let i = 0; i < exons.length; i++) {
            const exon = exons[i];
            if (((gene.startIndex + exon.start) <= variant.startIndex && (gene.startIndex + exon.end) >= variant.startIndex) ||
                ((gene.startIndex + exon.start) <= variant.endIndex && (gene.startIndex + exon.end) >= variant.endIndex)) {
                exon.matchedDomains = analyzeDomainStructure(domains, gene, exon, variant);
                matchedFeatures.push(exon);
            }
        }
    }
    return matchedFeatures;
}

function analyzeDomainStructure(domains, gene, exon, variant) {
    const matchedFeatures = [];
    if (domains !== null && domains !== undefined) {
        for (let i = 0; i < domains.length; i++) {
            const domain = domains[i];
            if (domain.start >= exon.start && domain.end <= exon.end &&
                (((gene.startIndex + domain.start) <= variant.startIndex && (gene.startIndex + domain.end) >= variant.startIndex) ||
                ((gene.startIndex + domain.start) <= variant.endIndex && (gene.startIndex + domain.end) >= variant.endIndex))) {
                matchedFeatures.push(domain);
            }
        }
    }
    return matchedFeatures;
}

function extractCdsRegions(exons, utrs) {
    if (exons === null || exons === undefined)
        return [];
    if (utrs === null || utrs === undefined)
        utrs = [];
    const cdsBlocks = [];
    for (let i = 0; i < exons.length; i++) {
        const cdsFeature = Object.assign({}, exons[i]);
        for (let j = 0; j < utrs.length; j++) {
            const utr = utrs[j];
            if (utr.start === cdsFeature.start) {
                cdsFeature.start = utr.end + 1;
            }
            if (utr.end === cdsFeature.end) {
                cdsFeature.end = utr.start - 1;
            }
        }
        if (cdsFeature.end > cdsFeature.start) {
            cdsBlocks.push(cdsFeature);
        }
    }
    return cdsBlocks;
}