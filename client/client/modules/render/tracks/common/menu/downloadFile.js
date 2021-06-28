function getLink (tracks) {
    const [{bioDataItemId}] = (tracks || [])
        .map(track => ({
            projectDataService: track.projectContext.projectDataService,
            bioDataItemId: track.config.bioDataItemId
        }))
        .filter(Boolean);
    return bioDataItemId;
}

export default {
    common: true,
    label: 'Download file',
    name: 'general>downloadFile',
    get: getLink,
    type: 'link',
    isVisible: (state, tracks) => tracks.length === 1
};
