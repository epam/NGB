import {DataService} from '../data-service';
/**
 * data service for externaldb
 * @extends DataService
 */
export class ExternaldbDataService extends DataService {


    getNcbiGeneInfo(id) {
        return this.get(`externaldb/ncbi/gene/${id}/get`);
    }

    getEnsemblGeneInfo(id){
        return this.get(`externaldb/ensembl/${id}/get`);
    }

    getUniprotGeneInfo(id){
        return this.get(`/externaldb/uniprot/${id}/get`);
    }
}
