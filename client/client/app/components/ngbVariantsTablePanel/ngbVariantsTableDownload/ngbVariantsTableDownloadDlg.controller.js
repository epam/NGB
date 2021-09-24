export default class ngbVariantsTableDownloadDlgController {

    isLoading = false;
    includeHeader = false;
    formatList = [
        {
            name: 'CSV',
            mimeType: 'text/csv'
        },
        {
            name: 'TSV',
            mimeType: 'text/tab-separated-values'
        }
    ];
    downloadFormat;
    error;

    constructor($mdDialog, projectContext, $scope) {
        this.$mdDialog = $mdDialog;
        this.projectContext = projectContext;
        this.$scope = $scope;

        this.downloadFormat = this.formatList[0];
    }

    static get UID() {
        return 'ngbVariantsTableDownloadDlgController';
    }

    download(event) {
        this.isLoading = true;
        this.projectContext.downloadVcfTable(
            this.projectContext.reference.id,
            this.downloadFormat.name,
            this.includeHeader
        ).then(data => {
            if (data.error) {
                this.error = 'Error during export happened';
            } else {
                this.error = '';
                const linkElement = document.createElement('a');
                try {
                    const blob = new Blob([data], {type: this.downloadFormat.mimeType});
                    const url = window.URL.createObjectURL(blob);

                    linkElement.setAttribute('href', url);
                    linkElement.setAttribute('download',
                        `VariantsTable-${this.projectContext.reference.id}.${this.downloadFormat.name.toLowerCase()}`);

                    const clickEvent = new MouseEvent('click', {
                        'view': window,
                        'bubbles': true,
                        'cancelable': false
                    });
                    linkElement.dispatchEvent(clickEvent);
                    this.close();
                } catch (ex) {
                    // eslint-disable-next-line no-console
                    console.error(ex);
                }
            }
            this.isLoading = false;
            this.$scope.$apply();
        });
        event.stopImmediatePropagation();
        return false;
    }

    close() {
        this.$mdDialog.hide();
    }
}
