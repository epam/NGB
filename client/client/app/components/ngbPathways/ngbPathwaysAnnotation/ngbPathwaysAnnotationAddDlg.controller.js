function parseConfigStr(rawStr) {
    const result = [];
    const strList = rawStr.split('\n');
    strList.forEach(str => {
        const [term, ...colors] = str.split(/\s+/);
        const colorList = colors.join().split(',');
        result.push({
            term: term,
            backgroundColor: colorList[0] ? colorList[0].trim() : undefined,
            foregroundColor: colorList[1] ? colorList[1].trim() : undefined
        });
    });
    return result;
}

function stringifyConfig(config) {
    let result = '';
    config.forEach(configStr => {
        result += `${configStr.term}\t`;
        result += configStr.backgroundColor;
        if (configStr.foregroundColor) {
            result += `,${configStr.foregroundColor}`;
        }
        result += '\n';
    });
    return result;
}

export default class ngbPathwaysAnnotationAddDlgController {
    annotationTypeList = {
        HEATMAP: 0,
        CSV: 1,
        MANUAL: 2
    };
    isLoading = false;
    configStr = null;
    annotation = {};

    constructor(ngbPathwaysService, $mdDialog, annotation) {
        this.ngbPathwaysService = ngbPathwaysService;
        this.$mdDialog = $mdDialog;
        if (annotation) {
            this.annotation = {
                id: annotation.id,
                name: annotation.name,
                type: annotation.type
            };
            if (this.annotation.type === this.annotationTypeList.MANUAL) {
                this.configStr = stringifyConfig(annotation.value);
            }
        } else {
            this.annotation = {
                name: undefined,
                type: undefined,
                configStr: null
            };
        }
    }

    static get UID() {
        return 'ngbGenesTableDownloadDlgController';
    }

    save() {
        let config = [];
        if (this.annotation.type === this.annotationTypeList.MANUAL) {
            config = parseConfigStr(this.configStr);
        }

        this.ngbPathwaysService.setAnnotation({
            ...this.annotation,
            value: config
        });
        this.close();
    }

    close() {
        this.$mdDialog.hide();
    }
}
