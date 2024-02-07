const TYPE = {
    PARASITE: 'PARASITE',
    DEFAULT: 'DEFAULT'
};

export function getGenes (species, type = 'DEFAULT', genesOfInterest, translationalGenes) {
    const isParasite = type === TYPE.PARASITE;
    if (isParasite) {
        return getParasiteGenes(species, genesOfInterest, translationalGenes);
    } else {
        return groupedBySpecies(species);
    }
}

function groupedBySpecies (species) {
    const groups = species.reduce((acc, curr) => {
        if (!acc[curr.speciesName]) {
            acc[curr.speciesName] = {
                count: 1,
                value: [curr]
            };
        } else {
            acc[curr.speciesName].count += 1;
            acc[curr.speciesName].value.push(curr);
        }
        return acc;
    }, {});
    const grouped = Object.values(groups).reduce((acc, curr) => {
        const getItem = (item) => ({
            speciesName: item.speciesName,
            geneId: item.geneId,
            geneName: item.geneName,
            taxId: item.taxId,
        });
        if (curr.count === 1) {
            const group = curr.value[0];
            acc.push({
                group: false,
                item: false,
                span: `${group.geneName} (${group.speciesName})`,
                chip: `${group.geneName} (${group.speciesName})`,
                hidden: `${group.geneName} ${group.speciesName}`,
                ...getItem(group)
            });
        }
        if (curr.count > 1) {
            const sumChip = curr.value.map(g => g.geneName);
            const head = curr.value[0];
            acc.push({
                group: true,
                item: false,
                span: `${head.speciesName}`,
                hidden: `${sumChip.join(' ')} ${head.speciesName}`,
                ...getItem(head)
            });
            acc = [...acc, ...curr.value.map(group => {
                return {
                    group: false,
                    item: true,
                    span: group.geneName,
                    chip: `${group.geneName} (${group.speciesName})`,
                    hidden: `${group.geneName} ${group.speciesName}`,
                    ...getItem(group)
                };
            })];
        }
        return acc;
    }, []);
    return grouped;
}

function getParasiteGenes (species, genesOfInterest, translationalGenes) {
    return species.map(gene => {
        const selected = [...genesOfInterest, ...translationalGenes]
            .filter(g => g.targetGeneId === gene.targetGeneId);
        return {
            group: false,
            item: false,
            span: `${gene.geneName} (${gene.speciesName}) (${gene.geneId})`,
            chip: `${gene.geneName} (${gene.speciesName}) (${gene.geneId})`,
            hidden: `${gene.geneName} ${gene.speciesName} ${gene.geneId}`,
            speciesName: gene.speciesName,
            geneId: gene.geneId,
            geneName: gene.geneName,
            taxId: gene.taxId,
            targetGeneId: gene.targetGeneId,
            selected: selected && selected.length,
        };
    });
}

export function createFilterFor(text) {
    return (gene) => gene.hidden.toLowerCase().includes(text.toLowerCase());
}
