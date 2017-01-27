export default class RowInfo {
    constructor(columnsConfigs, rowData) {
        columnsConfigs.forEach((columnProperties) => {
            const dataFields = Object.getOwnPropertyNames(columnProperties.data);
            dataFields.forEach((dField) => {
                const key = columnProperties.data[dField];
                this[key] = this._valueToString(rowData[key]);
            });
        });
    }

    _valueToString(value){
        return Array.isArray(value) ? value.join('; ') : value;
    }
    get value(){
        return this;
    }
}