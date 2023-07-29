export default function buildMainInfoBlocks (identificationData) {
    if (!identificationData) {
        return [];
    }
    return [
        {
            key: 'drugs',
            title: 'Known drugs',
            items: [
                {
                    item: 'drug',
                    count: identificationData.knownDrugsCount || 0
                }
            ]
        },
        {
            key: 'diseases',
            title: 'Associated diseases',
            items: [
                {
                    item: 'disease',
                    count: identificationData.diseasesCount || 0
                }
            ]
        },
        {
            key: 'sequences',
            title: 'Sequences',
            items: [
                {
                    item: 'DNA',
                    single: true,
                    count: identificationData.sequencesDNACount || 0
                },
                {
                    item: 'RNA',
                    single: true,
                    count: identificationData.sequencesRNACount || 0
                },
                {
                    item: 'protein',
                    count: identificationData.sequencesProteinsCount || 0
                }
            ]
        },
        {
            key: 'genomics',
            title: 'Comparative genomics',
            items: [
                {
                    item: 'paralog',
                    count: identificationData.paralogsCount || 0
                },
                {
                    item: 'ortholog',
                    count: identificationData.orthologsCount || 0
                }
            ]
        },
        {
            key: 'structure',
            title: 'Structure',
            items: [
                {
                    item: 'model',
                    count: identificationData.modelsCount || 0
                }
            ]
        },
        {
            key: 'bibliography',
            title: 'Bibliography',
            items: [
                {
                    item: 'publication',
                    count: identificationData.publicationsCount || 0
                }
            ]
        }
    ]
}
