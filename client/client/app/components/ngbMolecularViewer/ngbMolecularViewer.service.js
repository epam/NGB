import {EventGeneInfo} from '../../shared/utils/events';
export default class ngbMolecularViewerService {

    static instance(projectDataService, geneDataService, dispatcher) {
        return new ngbMolecularViewerService(projectDataService, geneDataService, dispatcher);
    }

    constructor(projectDataService, geneDataService, dispatcher) {
        this.dispatcher = dispatcher;
        this.geneDataService = geneDataService;
        this.projectDataService = projectDataService;
    }

    async loadPDB(geneTrack, event: EventGeneInfo) {
        const {startIndex, endIndex, geneId, transcriptId} = event;
        const pdbIds = [];
        let blocks = (await this.geneDataService.loadGeneTranscriptTrack({
            id: +geneTrack.id,
            chromosomeId: +geneTrack.chromosomeId,
            startIndex: startIndex,
            endIndex: endIndex,
            scaleFactor: 1
        })) || [];
        if (geneId) {
            blocks = blocks.filter(block => block.transcripts && block.transcripts.length > 0 && block.attributes && block.attributes.gene_id && block.attributes.gene_id.toLowerCase() === geneId.toLowerCase());
        }
        else {
            blocks = blocks.filter(block => block.transcripts && block.transcripts.length > 0);
        }
        if (blocks.length > 0) {
            blocks.forEach(block => {
                if (block.status === 'ERROR') {
                    throw ({message: block.message});
                }
                if (transcriptId) {
                    block.transcripts = block.transcripts.filter(x => (x.id && x.id.toLowerCase() === transcriptId.toLowerCase()) || (x.attributes && (x.attributes.transcript_id.toLowerCase() === transcriptId.toLowerCase() || x.attributes.id.toLowerCase() === transcriptId.toLowerCase())));
                }
                block.transcripts.forEach(t=> {
                    t.pdb && t.pdb.forEach(pdb=> {
                        pdb.transcript = {
                            exon: t.exon,
                            utr: t.utr,
                            domain: t.domain,
                            strand: block.strand,
                            gene: {
                                startIndex: block.startIndex,
                                endIndex: block.endIndex
                            }
                        };
                        pdbIds.push(pdb);
                    });
                }
                );
            });
        }

        return pdbIds;
    }

    async loadPdbDescription(pdbId) {
        return await this.geneDataService.getPdbDescription(pdbId);
    }


}
