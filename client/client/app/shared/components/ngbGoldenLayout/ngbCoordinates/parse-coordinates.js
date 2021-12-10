// "chr x: start - end"
const CHR_START_END_regExp = /^\s*([^:]+):\s*(\d+)\s*-\s*(\d+)\s*/;
// "chr: position"
const CHR_POSITION_regExp = /^\s*([^:]+):\s*(\d+)\s*/;
// "chr:"
const CHR_regExp = /^\s*([^:]+):\s*/;
// "start - end"
const START_END_regExp = /^\s*(\d+)\s*-\s*(\d+)\s*/;
// "position"
const POSITION_regExp = /^\s*(\d+)\s*/;
// "feature"
const FEATURE_regExp = /^\s*([^\s]+)\s*/;

function getChromosome(name, projectContext) {
    if (!projectContext) {
        return undefined;
    }
    return projectContext.getChromosome({name});
}

function correctPosition (chromosome, position) {
    if (!chromosome || Number.isNaN(Number(position))) {
        return undefined;
    }
    return Math.max(
        1,
        Math.min(
            chromosome.size,
            Number(position)
        )
    );
}

export function getCoordinatesText(navigation) {
    if (
        !navigation ||
        !navigation.chromosome ||
        !navigation.chromosome.name
    ) {
        return undefined;
    }
    const {
        chromosome,
        position,
        viewport = {}
    } = navigation;
    if (position) {
        return `${chromosome.name}: ${position}`;
    }
    if (viewport && viewport.start && viewport.end) {
        return `${chromosome.name}: ${viewport.start} - ${viewport.end}`;
    }
    return undefined;
}

function findBestFeatureFn (featureName) {
    return function filter (o) {
        return (o.featureId || '').toLowerCase() === featureName.toLowerCase() ||
            (o.name || '').toLowerCase() === featureName.toLowerCase() ||
            (o.featureName || '').toLowerCase() === featureName.toLowerCase();
    };
}

async function findFeature(featureName, cache = [], projectDataService, referenceId) {
    if (!featureName) {
        return undefined;
    }
    const find = findBestFeatureFn(featureName);
    const [o] = cache.filter(cacheItem => cacheItem.tag && find(cacheItem));
    let feature = o && o.tag ? o.tag : undefined;
    if (!feature && projectDataService && referenceId) {
        const result = await projectDataService.searchGenes(referenceId, featureName);
        if (result.entries && result.entries.length > 0) {
            const [best] = result.entries.filter(find);
            feature = best;
        }
    }
    if (
        feature &&
        feature.chromosome &&
        (feature.chromosome.name || feature.chromosome.id) &&
        feature.startIndex &&
        feature.endIndex
    ) {
        const {
            chromosome,
            startIndex,
            endIndex
        } = feature;
        return {
            chromosome,
            viewport: {
                start: startIndex,
                end: endIndex
            }
        };
    }
    return undefined;
}

export default async function parseCoordinates (
    string,
    projectContext,
    currentChromosomeName,
    searchResultsCache = [],
    projectDataService,
    referenceId
) {
    if (!string) {
        return [];
    }
    let rest = string.slice();
    const coordinates = [];
    let currentChromosome = getChromosome(currentChromosomeName, projectContext);
    do {
        const chrStartEnd = CHR_START_END_regExp.exec(rest);
        const chrPosition = CHR_POSITION_regExp.exec(rest);
        const chr = CHR_regExp.exec(rest);
        const startEnd = START_END_regExp.exec(rest);
        const position = POSITION_regExp.exec(rest);
        const feature = FEATURE_regExp.exec(rest);
        const matchedRegExp = chrStartEnd ||
            chrPosition ||
            chr ||
            startEnd ||
            position ||
            feature;
        if (chrStartEnd) {
            const [, chrName, start, end] = chrStartEnd;
            const chromosome = getChromosome(chrName, projectContext);
            const viewport = {
                start: correctPosition(chromosome, start),
                end: correctPosition(chromosome, end)
            };
            if (chromosome && viewport.start && viewport.end) {
                coordinates.push({chromosome, viewport});
                currentChromosome = chromosome;
            }
        } else if (chrPosition) {
            const [, chrName, _position] = chrPosition;
            const chromosome = getChromosome(chrName, projectContext);
            const positionPayload = correctPosition(chromosome, _position);
            if (chromosome && positionPayload) {
                coordinates.push({chromosome, position: positionPayload});
                currentChromosome = chromosome;
            }
        } else if (chr) {
            const [, chrName] = chr;
            const chromosome = getChromosome(chrName, projectContext);
            if (chromosome) {
                const viewport = {
                    start: 1,
                    end: currentChromosome.size
                };
                coordinates.push({chromosome, viewport});
                currentChromosome = chromosome;
            }
        } else if (startEnd && currentChromosome) {
            const [, start, end] = startEnd;
            const viewport = {
                start: correctPosition(currentChromosome, start),
                end: correctPosition(currentChromosome, end)
            };
            if (viewport.start && viewport.end) {
                coordinates.push({chromosome: currentChromosome, viewport});
            }
        } else if (position && currentChromosome) {
            const [, _position] = position;
            const positionPayload = correctPosition(currentChromosome, _position);
            if (positionPayload) {
                coordinates.push({chromosome: currentChromosome, position: positionPayload});
            }
        } else if (feature) {
            const featureNavigation = await findFeature(
                feature[1],
                searchResultsCache,
                projectDataService,
                referenceId
            );
            if (featureNavigation) {
                coordinates.push(featureNavigation);
            }
        }
        if (matchedRegExp) {
            rest = rest.slice(matchedRegExp.index + matchedRegExp[0].length);
        } else {
            rest = undefined;
        }
    } while (rest);
    return coordinates;
}
