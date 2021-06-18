function getLink (tracks) {
    const [{projectDataService, bioDataItemId}] = (tracks || [])
        .map(track => ({
            projectDataService: track.projectContext.projectDataService,
            bioDataItemId: track.config.bioDataItemId
        }))
        .filter(Boolean);
    if (projectDataService && bioDataItemId) {
        return projectDataService.getDatasetFileLink(bioDataItemId);
    }
    return null;
}

export default {
    common: true,
    label: 'Download file',
    name: 'general>downloadFile',
    get: getLink,
    type: 'link'
};
