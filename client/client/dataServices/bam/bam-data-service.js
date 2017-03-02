import {DataService} from '../data-service';

class BamDataInit {
    id: number;
    chromosomeId: number;
    startIndex: number;
    endIndex: number;
    scaleFactor: number;
    count: number;
    frame: number;
    file: string;
    index: string;
}

/**
 * data service for bam tracks
 * @extends DataService
 */
export class BamDataService extends DataService {
    /**
     * Returns metadata for all BAM files filtered by a reference genome
     * @returns {promise}
     */
    getAllFileList(referenceId) { return this.get(`bam/${referenceId}/loadAll`); }

    /**
     * Register BAM file on server
     * @returns {promise}
     */
    register(referenceId, path ,indexPath, name, type = 'FILE', s3BucketId = null) {
        if (s3BucketId)
            return this.post('bam/register', {referenceId, path, indexPath, name, type, s3BucketId});
        return this.post('bam/register', {referenceId, path, indexPath, name, type});
    }

    getReads(parameters, showClipping, filter) {
        const self = this;
        const getReadsFn = function(singleParameters) {
            const {side, ...props} = singleParameters;
            let loadDataFn = null;
            switch (side) {
                case 'center': {
                    loadDataFn = ::self.getFullCenter;
                }
                    break;
                case 'left': {
                    loadDataFn = ::self.getFullLeft;
                }
                    break;
                case 'right': {
                    loadDataFn = ::self.getFullRight;
                }
                    break;
                default: {
                    return Promise.resolve();
                }
            }
            if (loadDataFn) {
                const resolveFn = function(resolve) {
                    const catchFn = function() {
                        resolve({});
                    };
                    const thenFn = function(data) {
                        resolve(data);
                    };
                    loadDataFn(props, showClipping, filter).catch(catchFn).then(thenFn);
                };
                return new Promise(resolveFn);
            }
        };
        if (parameters instanceof Array) {
            const promises = [];
            for (let i = 0; i < parameters.length; i++) {
                promises.push(getReadsFn(parameters[i]));
            }
            return new Promise((resolve) => {
                Promise.all(promises).then(values => {
                    let result = {
                        baseCoverage: [],
                        blocks: [],
                        downsampleCoverage: [],
                        chromosome: null,
                        endIndex: null,
                        id: null,
                        minPosition: null,
                        referenceBuffer: null,
                        scaleFactor: null,
                        startIndex: null,
                        type: 'BAM'
                    };
                    for (let i = 0; i < values.length; i++) {
                        const localResult = values[i];
                        // merge info
                        if (!result.id) {
                            result.id = localResult.id;
                        }
                        if (!result.scaleFactor) {
                            result.scaleFactor = localResult.scaleFactor;
                        }
                        if (!result.chromosome) {
                            result.chromosome = localResult.chromosome;
                        }
                        if (!result.startIndex || result.startIndex < localResult.startIndex) {
                            result.startIndex = localResult.startIndex;
                        }
                        if (!result.endIndex || result.endIndex > localResult.endIndex) {
                            result.endIndex = localResult.endIndex;
                        }
                        // merge baseCoverage;
                        if (localResult.baseCoverage) {
                            for (let j = 0; j < localResult.baseCoverage.length; j++) {
                                result.baseCoverage.push(localResult.baseCoverage[j]);
                            }
                        }
                        //downsample coverage
                        if (localResult.downsampleCoverage) {
                            for (let j = 0; j < localResult.downsampleCoverage.length; j++) {
                                result.downsampleCoverage.push(localResult.downsampleCoverage[j]);
                            }
                        }
                        // merge reference buffer
                        if (localResult.referenceBuffer) {
                            if (!result.referenceBuffer) {
                                result.referenceBuffer = localResult.referenceBuffer;
                                result.minPosition = localResult.minPosition;
                            } else if (result.minPosition > localResult.minPosition) {
                                let newBuffer = localResult.referenceBuffer;
                                const delta = localResult.minPosition + newBuffer.length - result.minPosition;
                                if (delta > 0) {
                                    newBuffer = newBuffer.slice(0, delta);
                                } else {
                                    for (let b = 0; b < Math.abs(delta); b++) {
                                        newBuffer += ' ';
                                    }
                                }
                                result.referenceBuffer = newBuffer + result.referenceBuffer;
                                result.minPosition = localResult.minPosition;
                            } else {
                                let newBuffer = result.referenceBuffer;
                                const delta = result.minPosition + newBuffer.length - localResult.minPosition;
                                if (delta > 0) {
                                    newBuffer = newBuffer.slice(0, delta);
                                } else {
                                    for (let b = 0; b < Math.abs(delta); b++) {
                                        newBuffer += ' ';
                                    }
                                }
                                result.referenceBuffer = newBuffer + localResult.referenceBuffer;
                            }
                        }

                        // merge reads
                        if (localResult.blocks) {
                            for (let j = 0; j < localResult.blocks.length; j++) {
                                result.blocks.push(localResult.blocks[j]);
                            }
                        }
                    }
                    resolve(result);
                    result = null;
                });
            });
        } else {
            return getReadsFn(parameters);
        }
    }

    /**
     * Returns data matched the given query to fill in a bam track.
     * Result does not contain the reads which have an intersection with the right boundary
     * @returns {promise}
     */
    getFullLeft(props: BamDataInit, showClipping, filter) {
        const payload = {
            chromosomeId: props.chromosomeId,
            endIndex: props.endIndex,
            id: props.id,
            option: {
                count: props.count,
                filterDuplicate: filter.pcrOpticalDuplicates,
                filterNotPrimary: filter.secondaryAlignments,
                filterSupplementaryAlignment: filter.supplementaryAlignments,
                filterVendorQualityFail: filter.failedVendorChecks,
                frame: props.frame,
                showClipping,
                showSpliceJunction: true,
                trackDirection: 'LEFT'
            },
            scaleFactor: props.scaleFactor,
            startIndex: props.startIndex
        };
        let url = 'bam/track/get';
        if (props.file && props.index) {
            url = `bam/track/get?fileUrl=${encodeURIComponent(props.file)}&indexUrl=${encodeURIComponent(props.index)}`;
        }
        return this.post(url, payload);
    }

    /**
     * Returns data matched the given query to fill in a bam track. Returns all information about read's
     * @returns {promise}
     */
    getFullCenter(props: BamDataInit, showClipping, filter) {
        const payload = {
            chromosomeId: props.chromosomeId,
            endIndex: props.endIndex,
            id: props.id,
            option: {
                count: props.count,
                filterDuplicate: filter.pcrOpticalDuplicates,
                filterNotPrimary: filter.secondaryAlignments,
                filterSupplementaryAlignment: filter.supplementaryAlignments,
                filterVendorQualityFail: filter.failedVendorChecks,
                frame: props.frame,
                showClipping,
                showSpliceJunction: true,
                trackDirection: 'MIDDLE'
            },
            scaleFactor: props.scaleFactor,
            startIndex: props.startIndex
        };
        let url = 'bam/track/get';
        if (props.file && props.index) {
            url = `bam/track/get?fileUrl=${encodeURIComponent(props.file)}&indexUrl=${encodeURIComponent(props.index)}`;
        }
        return this.post(url, payload);
    }

    /**
     * Returns data matched the given query to fill in a bam track.
     * Result does not contain the reads which have an intersection with the left boundary
     * @returns {promise}
     */
    getFullRight(props: BamDataInit, showClipping, filter) {
        const payload = {
            chromosomeId: props.chromosomeId,
            endIndex: props.endIndex,
            id: props.id,
            option: {
                count: props.count,
                filterDuplicate: filter.pcrOpticalDuplicates,
                filterNotPrimary: filter.secondaryAlignments,
                filterSupplementaryAlignment: filter.supplementaryAlignments,
                filterVendorQualityFail: filter.failedVendorChecks,
                frame: props.frame,
                showClipping,
                showSpliceJunction: true,
                trackDirection: 'RIGHT'
            },
            scaleFactor: props.scaleFactor,
            startIndex: props.startIndex
        };
        let url = 'bam/track/get';
        if (props.file && props.index) {
            url = `bam/track/get?fileUrl=${encodeURIComponent(props.file)}&indexUrl=${encodeURIComponent(props.index)}`;
        }
        return this.post(url, payload);
    }

    /**
     * Provides extended data about the particular read
     */
    loadRead(props) {
        let {id, chromosomeId, startIndex, endIndex, name, openByUrl, file, index} = props;
        if (openByUrl) {
            const payload = {
                chromosomeId: chromosomeId,
                startIndex: startIndex,
                endIndex: endIndex,
                name: name
            };
            return this.post(`bam/read/load?fileUrl=${file}&indexUrl=${index}`, payload);
        } else {
            const payload = {
                id: id,
                chromosomeId: chromosomeId,
                startIndex: startIndex,
                endIndex: endIndex,
                name: name
            };
            return this.post('bam/read/load', payload);
        }
    }
}