export function groupedBySpecies (species) {
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
            acc = [...acc, ...curr.value.map(group => ({
                group: false,
                item: true,
                span: `${group.geneName}`,
                chip: `${group.geneName} (${group.speciesName})`,
                hidden: `${group.geneName} ${group.speciesName}`,
                ...getItem(group)
            }))];
        }
        return acc;
    }, []);
    return grouped;
}

export function createFilterFor(text) {
    return (gene) => gene.hidden.toLowerCase().includes(text.toLowerCase());
}
