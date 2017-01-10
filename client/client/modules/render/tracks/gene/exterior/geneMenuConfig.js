export default [
    {
        displayName: state => state.geneTranscript,
        fields: [
            {
                label: 'Collapsed',
                name: 'gene>transcript>collapsed',
                type: 'checkbox'
            },
            {
                label: 'Expanded',
                name: 'gene>transcript>expanded',
                type: 'checkbox'
            }
        ],
        label: 'Transcript View',
        name: 'gene>view',
        type: 'submenu'
    }
];
