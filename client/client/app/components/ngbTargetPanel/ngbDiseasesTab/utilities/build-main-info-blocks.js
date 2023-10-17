export default function buildMainInfoBlocks (diseasesData) {
    if (!diseasesData) {
        return [];
    }
    return [
        {
            key: 'drugs',
            title: 'Known drugs',
            items: [
                {
                    item: 'drug',
                    count: diseasesData.knownDrugsCount || 0
                },
                {
                    item: 'record',
                    count: diseasesData.knownDrugsRecordsCount || 0
                }
            ]
        },
        {
            key: 'targets',
            title: 'Associated targets',
            items: [
                {
                    item: 'target',
                    count: diseasesData.targetsCount || 0
                }
            ]
        }
    ]
}
