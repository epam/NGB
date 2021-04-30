export class VcfHighlightConditionService {
    expressionOperationList = {
        EQ: '==',
        GE: '>=',
        GT: '>',
        IN: 'in',
        LE: '<=',
        LT: '<',
        NE: '!=',
        NI: 'not in'
    };
    conditionOperationList = {
        AND: 'and',
        OR: 'or'
    };
    conditionTypeList = {
        EXPRESSION: 'expression',
        GROUP: 'group'
    };

    _parseExpression(expression) {
        const rawParsedExpression = expression
            .split(new RegExp(`(${Object.values(this.expressionOperationList).join('|')})`))
            .map(e => e.trim());
        const operator = rawParsedExpression[1];
        let value = rawParsedExpression[2];

        if ([this.expressionOperationList.GE,
            this.expressionOperationList.GT,
            this.expressionOperationList.LE,
            this.expressionOperationList.LT].includes(operator)) {
            value = this._removeExtraQuotes(value);
            value = value.includes('.') ? parseFloat(value) : parseInt(value);
        }
        if ([this.expressionOperationList.IN,
            this.expressionOperationList.NI].includes(operator)) {
            value = this._prepareArray(value);
        }
        return {
            field: rawParsedExpression[0],
            operator: operator,
            type: this.conditionTypeList.EXPRESSION,
            value: value
        };
    }

    _prepareArray(stringed) {
        const rawArray = stringed.replace(/(^\s*\[)|(]\s*$)/g, '').split(',');
        const result = [];
        rawArray.forEach(item => result.push(this._removeExtraQuotes(item.trim())));
        return result;
    }

    _removeExtraQuotes(str) {
        return str.replace(/(^('|"|\\"|\\')|('|"|\\"|\\')$)/g, '');
    }

    _prepareCondition(condition) {
        condition = condition.replace(/(^\s*\()|(\)\s*$)/g, '');
        if (Object.values(this.conditionOperationList).some(c => condition.includes(c))) {
            condition = this.parseCondition(condition);
        } else {
            condition = this._parseExpression(condition);
        }
        return condition;
    }

    validateFullCondition(condition) {
        let depth = 0;
        for (let i = 0; i < condition.length; i++) {
            if (condition[i] === '(') depth++;
            if (condition[i] === ')') depth--;
            if (depth < 0) {
                throw new Error(`Invalid condition ${condition} at position ${i}`);
            }
        }
        return true;
    }

    parseFullCondition(condition) {
        return this.validateFullCondition(condition)
            ? this.parseCondition(condition)
            : null;
    }

    parseCondition(condition) {
        let opIndex = condition.length;
        let operator = '';
        let i = 0;

        if (condition[0] === '(') {
            i = 1;
            for (let depth = 1; i < condition.length; i++) {
                if (condition[i] === '(') depth++;
                if (condition[i] === ')') depth--;
                if (!depth) break;
            }
        }

        Object.values(this.conditionOperationList).forEach(op => {
            const index = condition.indexOf(op, i + 1);
            if (~index && (opIndex > index)) {
                opIndex = index;
                operator = op;
            }
        });

        if (opIndex === condition.length) {
            return this._prepareCondition(condition);
        } else {
            return {
                conditions: [
                    this._prepareCondition(condition.substring(0, opIndex).trim()),
                    this._prepareCondition(condition.substring(opIndex + operator.length).trim())
                ],
                operator: operator,
                type: this.conditionTypeList.GROUP
            };
        }
    }

    calculateHighlight(condition, variant) {
        if (!variant || !condition || !condition.type) {
            return false;
        }
        switch (condition.type) {
            case this.conditionTypeList.GROUP: {
                return this.calculateCondition(condition, variant);
            }
            case this.conditionTypeList.EXPRESSION: {
                return this.calculateExpression(condition, variant);
            }
            default: {
                return false;
            }
        }
    }

    calculateCondition(condition, variant) {
        if (!condition.conditions) {
            return false;
        }
        let result = false;
        switch (condition.operator) {
            case this.conditionOperationList.AND: {
                result = true;
                condition.conditions.forEach(c => result = result && this.calculateHighlight(c, variant));
                break;
            }
            case this.conditionOperationList.OR: {
                condition.conditions.forEach(c => result = result || this.calculateHighlight(c, variant));
                break;
            }
        }
        return result;
    }

    calculateExpression(expression, variant) {
        if (!expression.field || !variant[expression.field]) {
            return false;
        }
        let result;
        switch (expression.operator) {
            case this.expressionOperationList.EQ: {
                result = variant[expression.field].toString() === expression.value;
                break;
            }
            case this.expressionOperationList.NE: {
                result = variant[expression.field].toString() !== expression.value;
                break;
            }
            case this.expressionOperationList.GE: {
                result = variant[expression.field] >= expression.value;
                break;
            }
            case this.expressionOperationList.GT: {
                result = variant[expression.field] > expression.value;
                break;
            }
            case this.expressionOperationList.LE: {
                result = variant[expression.field] <= expression.value;
                break;
            }
            case this.expressionOperationList.LT: {
                result = variant[expression.field] < expression.value;
                break;
            }
            case this.expressionOperationList.IN: {
                result = Array.isArray(expression.value) && expression.value.includes(variant[expression.field]);
                break;
            }
            case this.expressionOperationList.NI: {
                result = Array.isArray(expression.value) && !expression.value.includes(variant[expression.field]);
                break;
            }
            default: {
                result = false;
            }
        }
        // TODO: remove before merge
        // console.log(`${JSON.stringify(expression)} result: ${result}`);
        return result;
    }
}