function getLink (tracks) {
    const [{bioDataItemId}] = (tracks || [])
        .map(track => ({
            projectDataService: track.projectContext.projectDataService,
            bioDataItemId: track.config.bioDataItemId
        }))
        .filter(Boolean);
    return bioDataItemId;
}

function downloadResourceAvailable (state, tracks, track) {
    const downloadDisabled = track &&
        track.projectContext &&
        !track.projectContext.resourceDownloadAvailable;
    return tracks.length === 1 && !downloadDisabled;
}

export default {
    common: true,
    label: 'Download file',
    name: 'general>downloadFile',
    get: getLink,
    type: 'link',
    isVisible: downloadResourceAvailable
};
