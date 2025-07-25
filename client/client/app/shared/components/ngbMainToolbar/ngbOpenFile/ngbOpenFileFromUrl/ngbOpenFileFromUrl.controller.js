function getUrlInfo(url) {
    if (!url) {
        return undefined;
    }
    let path = url;
    if (/^https?:\/\//.test(url)) {
        try {
            const urlObj = new URL(url);
            path = urlObj.pathname || url;
        } catch {
            // noop
        }
    }
    const lastPart = path.split(/[\/\\]/).pop();
    const parts = lastPart.split('.');
    let extension = parts.pop();
    if (extension && extension.toLowerCase() === 'gz') {
        extension = parts.pop();
    }
    return {
        name: lastPart,
        url,
        extension: (extension || '').toLowerCase(),
    };
}

export default class ngbOpenFileFromUrlController {
    static get UID() {
        return 'ngbOpenFileFromUrlController';
    }

    projectContext;
    references;
    referenceId;
    filePath;
    indexFilePath;
    switchingReference = false;

    constructor($scope, projectContext) {
        this.projectContext = projectContext;
        this.references = this.projectContext.references;
        this.referenceId = this.projectContext.reference ? this.projectContext.reference.id : null;
        if (!this.referenceId && this.references && this.references.length > 0) {
            this.referenceId = this.references[0].id;
        }
        $scope.$watch('$ctrl.filePath', ::this.check);
        $scope.$watch('$ctrl.indexFilePath', ::this.check);
        $scope.$watch('$ctrl.referenceId', ::this.check);
        (async() => {
            await this.projectContext.refreshReferences(true);
            this.references = this.projectContext.references;
            if (!this.referenceId && this.references && this.references.length > 0) {
                this.referenceId = this.references[0].id;
            }
        })();
    }

    check() {
        this.switchingReference = this.projectContext.reference && this.projectContext.reference.id !== +this.referenceId;
        if (this.referenceId && this.filePath && this.filePath.length &&
            (!this.trackNeedsIndexFile || (this.indexFilePath && this.indexFilePath.length)) && this.trackFormat()){
            const [reference] = this.references.filter(r => r.id === +this.referenceId);
            this.tracks = [{
                index: this.indexFilePath,
                path: this.filePath,
                reference: reference,
                format: this.trackFormat(),
                name: this.fileName()
            }];
        } else {
            this.tracks = [];
        }
    }

    get trackNeedsIndexFile() {
        const extension = this.fileExtension();
        return extension === 'bam' ||
            extension === 'vcf' ||
            extension === 'bed' ||
            extension === 'gff' ||
            extension === 'gff3' ||
            extension === 'gtf';

    }

    fileExtension() {
        if (!this.filePath || !this.filePath.length) {
            return null;
        }
        const info = getUrlInfo(this.filePath);
        if (info) {
            return info.extension;
        }
        return null;
    }

    fileName() {
        if (!this.filePath || !this.filePath.length) {
            return null;
        }
        const info = getUrlInfo(this.filePath);
        if (info) {
            return info.name;
        }
        return null;
    }

    trackFormat() {
        const extension = this.fileExtension();
        if (extension) {
            switch (extension.toLowerCase()) {
                case 'bam': return 'BAM';
                case 'vcf': return 'VCF';
                case 'bed': return 'BED';
                case 'gff': return 'GENE';
                case 'gff3': return 'GENE';
                case 'gtf': return 'GENE';
            }
        }
        return null;
    }

}
