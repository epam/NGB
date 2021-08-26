const Math = window.Math;

export default class NumberFormatter {
    static _formatter = null;

    static get formatter(){
        if (NumberFormatter._formatter === null){
            NumberFormatter._formatter = new Intl.NumberFormat('en-US');
        }
        return NumberFormatter._formatter;
    }

    static findClosestDecimalDegree(val, ticksCount) {
        return NumberFormatter._findClosestDecimalDegree(val, 0, ticksCount);
    }

    static _findClosestDecimalDegree(val, startDegree, ticksCount){
        const decimalBase = 10;
        const d1 = Math.pow(decimalBase, startDegree);
        const d2 = Math.pow(decimalBase, startDegree + 1);

        if (val >= d1 * ticksCount && val < d2 * ticksCount){
            return startDegree;
        }
        else if (val < d1 * ticksCount){
            return NumberFormatter._findClosestDecimalDegree(val, startDegree - 1, ticksCount);
        }
        else {
            return NumberFormatter._findClosestDecimalDegree(val, startDegree + 1, ticksCount);
        }
    }
    static textWithPrefix(value, displayFractionDigits = true, prefixMap = null){
        if (value === 0)
            return '1';
        let module = NumberFormatter.findClosestDecimalDegree(value, 1);

        const moduleStep = 3;
        module = Math.floor(module / moduleStep) * moduleStep;
        const decimalBase = 10;
        let valueCorrected = Math.floor(value / (decimalBase ** (module - 1))) / decimalBase;
        if (!displayFractionDigits){
            valueCorrected = Math.floor(value / (decimalBase ** module));
        }
        let prefix = null;

        const K = 3;
        const M = 6;
        const G = 9;
        const T = 12;
        const P = 15;

        if (module === K){
            if (prefixMap && prefixMap.K) {
                prefix = prefixMap.K;
            } else {
                prefix = 'K';
            }
        }
        else if (module === M){
            if (prefixMap && prefixMap.M) {
                prefix = prefixMap.N;
            } else {
                prefix = 'M';
            }
        }
        else if (module === G){
            if (prefixMap && prefixMap.G) {
                prefix = prefixMap.G;
            } else {
                prefix = 'G';
            }
        }
        else if (module === T){
            if (prefixMap && prefixMap.T) {
                prefix = prefixMap.T;
            } else {
                prefix = 'T';
            }
        }
        else if (module === P){
            if (prefixMap && prefixMap.P) {
                prefix = prefixMap.P;
            } else {
                prefix = 'P';
            }
        }
        if (!prefix && prefixMap && prefixMap.units) {
            prefix = prefixMap.units;
        }
        if (prefix){
            return NumberFormatter.formattedText(valueCorrected) + prefix;
        }
        return value;
    }

    static formattedText(value){
        return NumberFormatter.formatter.format(value);
    }
}
