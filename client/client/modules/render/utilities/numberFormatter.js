const Math = window.Math;

export default class NumberFormatter{

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
    static textWithPrefix(value, displayFractionDigits = true){
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
            prefix = 'K';
        }
        else if (module === M){
            prefix = 'M';
        }
        else if (module === G){
            prefix = 'G';
        }
        else if (module === T){
            prefix = 'T';
        }
        else if (module === P){
            prefix = 'P';
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
