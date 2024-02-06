const TYPE = {
    PARASITE: 'PARASITE',
    DEFAULT: 'DEFAULT'
};

export function groupedBySpecies (species, type = 'DEFAULT') {
    const isParasite = type === TYPE.PARASITE;
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
            const span = isParasite
                ? `${group.geneName} (${group.speciesName}) (${group.geneId})`
                : `${group.geneName} (${group.speciesName})`;
            const chip = isParasite
                ?`${group.geneName} (${group.speciesName}) (${group.geneId})`
                : `${group.geneName} (${group.speciesName})`;
            const hidden = isParasite
                ?`${group.geneName} ${group.speciesName} ${group.geneId}`
                : `${group.geneName} ${group.speciesName}`;

            acc.push({
                group: false,
                item: false,
                span,
                chip,
                hidden,
                ...getItem(group)
            });
        }
        if (curr.count > 1) {
            const sumChip = curr.value.map(g => g.geneName);
            const sumId = curr.value.map(g => g.geneId);
            const head = curr.value[0];
            const hidden = isParasite
                    ?`${sumChip.join(' ')} ${sumId.join(' ')} ${head.speciesName}`
                    : `${sumChip.join(' ')} ${head.speciesName}`;
            acc.push({
                group: true,
                item: false,
                span: `${head.speciesName}`,
                hidden,
                ...getItem(head)
            });
            acc = [...acc, ...curr.value.map(group => {
                const span = isParasite ? `${group.geneName} (${group.geneId})` : group.geneName;
                const chip = isParasite
                    ?`${group.geneName} (${group.speciesName}) (${group.geneId})`
                    : `${group.geneName} (${group.speciesName})`;
                const hidden = isParasite
                    ?`${group.geneName} ${group.speciesName} ${group.geneId}`
                    : `${group.geneName} ${group.speciesName}`;
                return {
                    group: false,
                    item: true,
                    span,
                    chip,
                    hidden,
                    ...getItem(group)
                };
            })];
        }
        return acc;
    }, []);
    return grouped;
}

export function createFilterFor(text) {
    return (gene) => gene.hidden.toLowerCase().includes(text.toLowerCase());
}
