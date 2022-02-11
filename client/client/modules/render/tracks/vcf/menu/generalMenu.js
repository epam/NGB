import commonMenu from '../../common/menu';

const [resize, header, downloadFile] = commonMenu;

export default {
    fields: [
        resize,
        header,
        {
            label: 'Rename samples',
            name: 'vcf>general>renameSamples',
            perform: renameSamples,
            type: 'button',
            isVisible: (state, tracks, track) => tracks.length === 1 && track.multisample
        },
        downloadFile
    ],
    label:'General',
    name:'vcf>general',
    type: 'submenu'
};

function renameSamples(tracks) {
    const [dispatcher] = (tracks || [])
        .map(track => track.config.dispatcher)
        .filter(Boolean);
    if (dispatcher) {
        dispatcher.emitSimpleEvent('vcf:rename:samples', tracks);
    }
}
