import {default as RowInfo} from './tableRowInfo';
export default class TableInfo {
    rows = [];
    constructor(config, rowVals = []) {
        if(!Array.isArray(config)) throw new Error('Incorrect table configuration');

        this.config = config;
        this.addRange(rowVals);
    }

    getRowValue(index){
        if(this.rows.length < index) throw new Error('Index must be less than rows number');
        return this.rows[index].value;
    }

    addRow(rowData){
        this.rows.push(new RowInfo(this.config, rowData));
    }

    addRange(rowsData){
        if(!Array.isArray(rowsData)) return;

        rowsData.forEach((rowData) => {
            this.addRow(rowData);
        });
    }
}
