export default function buildMainInfoBlocks (identificationData) {
    if (!identificationData) {
        return [];
    }
    if (!identificationData.sequencesCount) {
        identificationData.sequencesCount = {};
    }
    return [
        {
            key: 'drugs',
            title: 'Known drugs',
            items: [
                {
                    item: 'drug',
                    count: identificationData.knownDrugsCount || 0
                },
                {
                    item: 'record',
                    count: identificationData.knownDrugsRecordsCount || 0
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
                    count: identificationData.sequencesCount.dnas || 0
                },
                {
                    item: 'RNA',
                    single: true,
                    count: identificationData.sequencesCount.mrnas || 0
                },
                {
                    item: 'protein',
                    count: identificationData.sequencesCount.proteins || 0
                }
            ]
        },
        {
            key: 'genomics',
            title: 'Comparative genomics',
            items: [
                {
                    item: 'homolog',
                    count: identificationData.homologsCount || 0
                }
            ]
        },
        {
            key: 'structure',
            title: 'Structure',
            items: [
                {
                    item: 'model',
                    count: identificationData.structuresCount || 0
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
        },
        {
            key: 'patents',
            title: 'Patents',
        }
    ]
}
