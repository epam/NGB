export default class ngbDatasetItemDownloadUrlController {
    static get UID() {
        return 'ngbDatasetItemDownloadUrlController';
    }
    constructor($scope, projectDataService) {
        this.url = undefined;
        this.size = undefined;
        this.fetching = true;
        this.error = undefined;
        this.size = 0;
        if (this.id) {
            projectDataService.getDatasetFileInfo(this.id)
                .then(info => {
                    if (!info.error) {
                        const {
                            url,
                            size
                        } = info;
                        this.error = undefined;
                        this.size = size;
                        this.url = url;
                    } else {
                        this.error = 'Download url is not available';
                    }
                    this.fetching = false;
                })
                .then(() => $scope.$apply());
        } else {
            this.fetching = false;
        }
    }
}
