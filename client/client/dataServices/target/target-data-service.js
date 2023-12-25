import {DataService} from '../data-service';

const SOURCE = {
    OPEN_TARGETS: 'OPEN_TARGETS',
    TXGNN: 'TXGNN',
    DGI_DB: 'DGI_DB',
    PHARM_GKB: 'PHARM_GKB'
};

const ExternalDBApi = {
    [SOURCE.OPEN_TARGETS]: 'opentargets',
    [SOURCE.TXGNN]: 'txgnn',
    [SOURCE.DGI_DB]: 'dgidb',
    [SOURCE.PHARM_GKB]: 'pharmgkb'
};

const ExternalDBNames = {
    [SOURCE.OPEN_TARGETS]: 'Open Targets',
    [SOURCE.TXGNN]: 'TxGNN',
    [SOURCE.DGI_DB]: 'DGIdb',
    [SOURCE.PHARM_GKB]: 'PharmGKB'
};

const ExternalDBFields = {
    [SOURCE.OPEN_TARGETS]: 'opentargets',
    [SOURCE.DGI_DB]: 'dgidb',
    [SOURCE.PHARM_GKB]: 'pharmGKB'
};

const PDB_SOURCE = {
    PROTEIN_DATA_BANK: 'Protein Data Bank',
    LOCAL_FILES: 'Local Files'
};

function getQueryString(query = {}) {
    const params = Object.entries(query)
        .filter(([, value]) => value !== undefined)
        .map(([name, value]) => `${encodeURIComponent(name)}=${encodeURIComponent(value)}`)
        .join('&');
    if (params.length > 0) {
        return `?${params}`;
    }
    return '';
}

export class TargetDataService extends DataService {

    getTargetsResult(request) {
        return new Promise((resolve, reject) => {
            this.post('target/filter', request)
                .then(data => {
                    if (data && data.items) {
                        resolve([data.items, data.totalCount]);
                    } else {
                        resolve([[], data.totalCount]);
                    }
                })
                .catch(error => {
                    const message = 'Error fetching targets';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    postNewTarget(request) {
        return new Promise((resolve, reject) => {
            this.post('target', request)
                .then(data => {
                    if (data && data.targetId) {
                        resolve(data);
                    } else {
                        resolve(false);
                    }
                })
                .catch(error => {
                    const message = 'Error creating target';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getTargetById(targetId) {
        return new Promise((resolve) => {
            this.get(`target/${targetId}`)
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve([]);
                    }
                });
        });
    }

    updateTarget(request) {
        return new Promise((resolve, reject) => {
            this.put('target', request)
                .then(data => {
                    if (data) {
                        resolve(data);
                    }
                })
                .catch(error => {
                    const message = 'Error updating target';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    deleteTarget(targetId) {
        return new Promise((resolve, reject) => {
            this.delete(`target/${targetId}`)
                .then((data) => {
                    resolve(data || null);
                })
                .catch((error) => {
                    const message = 'Error removing target';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getTargetFieldValue(field) {
        return new Promise((resolve) => {
            this.get(`target/fieldValues?field=${field}`)
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve([]);
                    }
                });
        });
    }

    searchGenes(prefix) {
        return new Promise((resolve) => {
            this.get(`target/genes/${prefix}`)
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve([]);
                    }
                })
                .catch(err => {
                    resolve([]);
                });
        });
    }

    postTargetIdentification(request) {
        return new Promise((resolve, reject) => {
            this.post('target/identification', request)
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve({});
                    }
                })
                .catch(error => {
                    const message = 'Error launching target';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getDrugsResults(request, source) {
        return new Promise((resolve, reject) => {
            this.post(`target/${ExternalDBApi[source]}/drugs`, request)
                .then(data => {
                    if (data && data.items) {
                        resolve([data.items, data.totalCount]);
                    } else {
                        resolve([[], data.totalCount]);
                    }
                })
                .catch(error => {
                    const message = `Error getting drugs from ${ExternalDBNames[source]}`;
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getDiseasesResults(request, source) {
        return new Promise((resolve, reject) => {
            this.post(`target/${ExternalDBApi[source]}/diseases`, request)
                .then(data => {
                    if (data && data.items) {
                        resolve([data.items, data.totalCount]);
                    } else {
                        resolve([[], data.totalCount]);
                    }
                })
                .catch(error => {
                    const message = `Error getting diseases from ${ExternalDBNames[source]}`;
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getAllDiseasesResults(request, source) {
        return new Promise((resolve, reject) => {
            this.post(`target/${ExternalDBApi[source]}/diseases/all`, request)
                .then(data => resolve(data || []))
                .catch(error => {
                    const message = `Error getting all diseases from ${ExternalDBNames[source]}`;
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getOntology(source) {
        return new Promise((resolve, reject) => {
            this.get(`target/${ExternalDBApi[source]}/diseases/ontology`)
                .then(data => resolve(data || []))
                .catch(error => {
                    const message = `Error getting diseases ontology from ${ExternalDBNames[source]}`;
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getPublications(request) {
        return new Promise((resolve, reject) => {
            this.post('target/publications', request)
                .then(data => {
                    if (data && data.items) {
                        resolve([data.items, data.totalCount]);
                    } else {
                        resolve([[], 0]);
                    }
                })
                .catch(error => {
                    const message = 'Error getting publications';
                    reject(new Error((error && error.message) || message));
            });
        });
    }

    getLlmSummary(request, modelOptions) {
        const {
            type: provider,
            ...rest
        } = modelOptions || {};
        if (!provider) {
            return Promise.reject(new Error('LLM provider not specified'));
        }
        return new Promise((resolve, reject) => {
            this.post(`llm/summary${getQueryString({provider, ...rest})}`, request)
                .then(data => {
                    resolve(data);
                })
                .catch(error => {
                    const message = 'Error generating publications summary';
                    reject(new Error((error && error.message) || message));
            });
        });
    }

    getAbstracts(request) {
        return new Promise((resolve, reject) => {
            this.post('target/abstracts', request)
                .then(data => {
                    resolve(data);
                })
                .catch(error => {
                    const message = 'Error generating publications summary';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    llmChat(request, modelOptions) {
        const {
            type: provider,
            ...rest
        } = modelOptions || {};
        if (!provider) {
            return Promise.reject(new Error('LLM provider not specified'));
        }
        return new Promise((resolve, reject) => {
            this.post(`llm/chat${getQueryString({provider, ...rest})}`, request)
                .then(data => {
                    resolve(data);
                })
                .catch(error => {
                    const message = 'Error generating publications summary';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getDrugsFieldValues(source, geneIds) {
        return new Promise((resolve) => {
            this.get(`target/${ExternalDBFields[source]}/drugs/fieldValues?geneIds=${geneIds}`)
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve([]);
                    }
                });
        });
    }

    getSequencesTableResults(geneIds) {
        const getComments = false;
        return new Promise((resolve, reject) => {
            this.get(`target/sequences/table${getQueryString({geneIds, getComments})}`)
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve([]);
                    }
                })
                .catch(error => {
                    const message = 'Error getting sequences data';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getStructureResults(request, source) {
        const name = (() => {
            if (source === PDB_SOURCE.PROTEIN_DATA_BANK) { return 'target/structures'; }
            if (source === PDB_SOURCE.LOCAL_FILES) { return 'pdb/filter'; }
            return;
        })();
        if (!name) return Promise.reject(new Error('Unknown source'));
        return new Promise((resolve, reject) => {
            this.post(name, request)
                .then(data => {
                    if (data && data.items) {
                        resolve([data.items, data.totalCount]);
                    } else {
                        resolve([[], data.totalCount]);
                    }
                })
                .catch(error => {
                    const message = 'Error getting structure data';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getPdbDescription(pbdID) {
        return new Promise((resolve, reject) => {
            this.post(`gene/pbd/${pbdID}/get`, pbdID)
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve(false);
                    }
                })
                .catch(error => {
                    const message = `Error getting ${pbdID} pdb description`;
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getTargetExport(genesOfInterest, translationalGenes, source) {
        const format = 'CSV';
        const includeHeader = true;
        return new Promise((resolve, reject) => {
            this.downloadFile(
                'get',
                `target/export${getQueryString({
                    genesOfInterest,
                    translationalGenes,
                    format,
                    source,
                    includeHeader
                })}`,
                undefined,
                {customResponseType: 'arraybuffer'}
            )
                .catch((response) => resolve({...response, error: true}))
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve([]);
                    }
                }, reject);
        });
    }

    getTargetExportGeneId(geneId, source) {
        const format = 'CSV';
        const includeHeader = true;
        return new Promise((resolve, reject) => {
            this.downloadFile(
                'get',
                `target/export/${geneId}${getQueryString({
                    format,
                    source,
                    includeHeader
                })}`,
                undefined,
                {customResponseType: 'arraybuffer'}
            )
                .catch((response) => resolve({...response, error: true}))
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve([]);
                    }
                }, reject);
        });
    }

    getTargetAlignment (targetId, sequenceIds) {
        return new Promise((resolve, reject) => {
            this.get(`target/alignment/${targetId}${getQueryString({...sequenceIds})}`)
                .then(data => {
                    resolve(data);
                })
                .catch(error => {
                    const message = 'Error getting alignmment result';
                    reject(new Error((error && error.message) || message));
            });
        });
    }

    getSequence (db, id) {
        return new Promise((resolve, reject) => {
            this.get(`sequence/${id}?database=${db}`)
                .then(data => {
                    resolve(data);
                })
                .catch(error => {
                    const message = 'Error getting sequence';
                    reject(new Error((error && error.message) || message));
            });
        });
    }

    getHomologeneLoad(geneIds) {
        return new Promise((resolve, reject) => {
            this.get(`homologene/search${getQueryString({...geneIds})}`)
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve({});
                    }
                })
                .catch(error => {
                    const message = 'Error getting homologene';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getOrthoParaLoad(geneIds) {
        return new Promise((resolve, reject) => {
            this.get(`homolog/search${getQueryString({...geneIds})}`)
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        reject({});
                    }
                })
                .catch(error => {
                    const message = 'Error getting orthologs or paralogs';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getDisease(name) {
        return new Promise((resolve, reject) => {
            this.get(`disease?name=${name}`)
                .then(data => {
                    resolve(data);
                })
                .catch(error => {
                    const message = 'Error getting diseases list';
                    reject(new Error((error && error.message) || message));
            });
        });
    }

    getDiseaseData(diseaseId) {
        return new Promise((resolve, reject) => {
            this.get(`disease/${diseaseId}`)
                .then(data => {
                    resolve(data);
                })
                .catch(error => {
                    const message = 'Error getting disease data';
                    reject(new Error((error && error.message) || message));
            });
        });
    }

    getTargetsResults(diseaseId, request) {
        return new Promise((resolve, reject) => {
            this.post(`disease/targets/${diseaseId}`, request)
                .then(data => {
                    if (data && data.items) {
                        resolve([data.items, data.totalCount]);
                    } else {
                        resolve([[], data.totalCount]);
                    }
                })
                .catch(error => {
                    const message = 'Error getting targets results';
                    reject(new Error((error && error.message) || message));
            });
        });
    }

    getDiseasesDrugsResults(diseaseId, request) {
        return new Promise((resolve, reject) => {
            this.post(`disease/drugs/${diseaseId}`, request)
                .then(data => {
                    if (data && data.items) {
                        resolve([data.items, data.totalCount]);
                    } else {
                        resolve([[], data.totalCount]);
                    }
                })
                .catch(error => {
                    const message = 'Error getting drugs results';
                    reject(new Error((error && error.message) || message));
            });
        });
    }

    getDiseasesDrugsFieldValues(diseaseId) {
        return new Promise((resolve) => {
            this.get(`disease/drugs/fieldValues/${diseaseId}`)
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve([]);
                    }
                });
        });
    }

    getDiseasesExport(diseaseId, source) {
        const format = 'CSV';
        const includeHeader = true;
        return new Promise((resolve, reject) => {
            this.downloadFile(
                'get',
                `disease/${source}/export${getQueryString({diseaseId, format, includeHeader})}`,
                undefined,
                {customResponseType: 'arraybuffer'}
            )
                .catch((response) => resolve({...response, error: true}))
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve([]);
                    }
                }, reject);
        });
    }

    getTargetExcelReport(genesOfInterest, translationalGenes) {
        return new Promise((resolve, reject) => {
            this.downloadFile(
                'get',
                `target/report${getQueryString({genesOfInterest, translationalGenes})}`,
                undefined,
                {customResponseType: 'arraybuffer'}
            )
                .catch((response) => resolve({...response, error: true}))
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve([]);
                    }
                }, reject);
        });
    }

    getDiseaseIdentification(diseaseId) {
        return new Promise((resolve, reject) => {
            this.get(`disease/identification/${diseaseId}`)
                .then(data => {
                    resolve(data);
                })
                .catch(error => {
                    const message = 'Error getting disease total counts';
                    reject(new Error((error && error.message) || message));
            });
        });
    }

    searchTarget(geneName) {
        return new Promise((resolve, reject) => {
            this.get(`target?geneName=${geneName}`)
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve([]);
                    }
                })
                .catch(error => {
                    const message = `Error getting ${geneName} targets list`;
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    postIdentification(request) {
        return new Promise((resolve, reject) => {
            this.post('identification', request)
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve(false);
                    }
                })
                .catch(error => {
                    const message = 'Error saving identification';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    deleteIdentification(identificationId) {
        return new Promise((resolve, reject) => {
            this.delete(`identification/${identificationId}`)
                .then(() => {
                    resolve();
                })
                .catch((error) => {
                    const message = 'Error removing identification';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getTargetHtmlReport(genesOfInterest, translationalGenes, targetId) {
        return new Promise((resolve, reject) => {
            this.downloadFile(
                'get',
                `target/html${getQueryString({genesOfInterest, translationalGenes, targetId})}`,
                undefined,
                {customResponseType: 'arraybuffer'}
            )
                .catch((response) => resolve({...response, error: true}))
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve([]);
                    }
                }, reject);
        });
    }

    getTargetExcelReportGeneId(geneId) {
        return new Promise((resolve, reject) => {
            this.downloadFile(
                'get',
                `target/report/${geneId}`,
                undefined,
                {customResponseType: 'arraybuffer'}
            )
                .catch((response) => resolve({...response, error: true}))
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve([]);
                    }
                }, reject);
        });
    }

    getTargetHtmlReportGeneId(geneId) {
        return new Promise((resolve, reject) => {
            this.downloadFile(
                'get',
                `target/html/${geneId}`,
                undefined,
                {customResponseType: 'arraybuffer'}
            )
                .catch((response) => resolve({...response, error: true}))
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve([]);
                    }
                }, reject);
        });
    }

    getPatentsByProteinName(request) {
        return new Promise((resolve, reject) => {
            this.post('patents/proteins/ncbi', request)
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve(false);
                    }
                })
                .catch(error => {
                    const message = 'Error getting patents';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getPatentsByProteinId(targetId, sequenceId) {
        return new Promise((resolve, reject) => {
            this.get(`patents/proteins/${targetId}?sequenceId=${sequenceId}`)
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve(false);
                    }
                })
                .catch(error => {
                    const message = 'Error getting patents';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getBlastTaskBySequence(sequence) {
        return new Promise((resolve, reject) => {
            const message = 'Error getting blast task';
            this.get(`patents/proteins?sequence=${sequence}`)
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        reject(new Error(message));
                    }
                })
                .catch(error => {
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getDrugs(geneIds) {
        return new Promise((resolve, reject) => {
            this.get(`target/drugs?geneIds=${geneIds}`)
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve([]);
                    }
                })
                .catch(error => {
                    const message = 'Error getting drugs';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    searchPatentsByDrug(request) {
        return new Promise((resolve, reject) => {
            this.post('patents/drugs/ncbi', request)
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve(false);
                    }
                })
                .catch(error => {
                    const message = 'Error getting patents';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    searchPatentsByStructure(id) {
        return new Promise((resolve, reject) => {
            this.get(`patents/drugs/ncbi?id=${id}`)
                .then(data => {
                    if (data) {
                        const result = {
                            items: data,
                            totalCount: data.length
                        };
                        resolve(result);
                    } else {
                        const result = { totalCount: 0 };
                        resolve(result);
                    }
                })
                .catch(error => {
                    const message = 'Error getting patents';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getCompound(name) {
        return new Promise((resolve, reject) => {
            this.get(`compound/smiles/${name}`)
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve(false);
                    }
                })
                .catch(error => {
                    const message = 'Error getting identifier';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    generateTMAP(geneIds) {
        return new Promise((resolve, reject) => {
            this.get(`drug/tmap?geneIds=${geneIds}`)
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve();
                    }
                })
                .catch(error => {
                    const message = 'Error generating TMAP';
                    reject(new Error((error && error.message) || message));
                });
        });
    }
}
