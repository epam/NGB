export default class VcfHighlightConditionService {
    static expressionOperationList = {
        EQ: '==',
        GE: '>=',
        GT: '>',
        IN: 'in',
        LE: '<=',
        LT: '<',
        NE: '!=',
        NI: 'not in'
    };
    static conditionOperationList = {
        AND: 'and',
        OR: 'or'
    };
    static conditionTypeList = {
        EXPRESSION: 'expression',
        GROUP: 'group'
    };

    static _parseExpression(expression) {
        const rawParsedExpression = expression
            .split(new RegExp(`(${Object.values(VcfHighlightConditionService.expressionOperationList).join('|')})`))
            .map(e => e.trim());
        const operator = rawParsedExpression[1];
        let value = rawParsedExpression[2];

        if (!operator) {
            throw new Error(`Invalid expression ${expression}`);
        }
        if ([VcfHighlightConditionService.expressionOperationList.EQ,
            VcfHighlightConditionService.expressionOperationList.NE].includes(operator)) {
            value = VcfHighlightConditionService._removeExtraQuotes(value);
            value = VcfHighlightConditionService._regularizeString(value);
        }
        if ([VcfHighlightConditionService.expressionOperationList.GE,
            VcfHighlightConditionService.expressionOperationList.GT,
            VcfHighlightConditionService.expressionOperationList.LE,
            VcfHighlightConditionService.expressionOperationList.LT].includes(operator)) {
            value = VcfHighlightConditionService._removeExtraQuotes(value);
            value = value.includes('.') ? parseFloat(value) : parseInt(value);
        }
        if ([VcfHighlightConditionService.expressionOperationList.IN,
            VcfHighlightConditionService.expressionOperationList.NI].includes(operator)) {
            value = VcfHighlightConditionService._prepareArray(value);
        }
        return {
            field: VcfHighlightConditionService._removeExtraQuotes(rawParsedExpression[0]),
            operator: operator,
            type: VcfHighlightConditionService.conditionTypeList.EXPRESSION,
            value: value
        };
    }

    static _prepareArray(stringed) {
        const rawArray = stringed.replace(/(^\s*\[)|(]\s*$)/g, '').split(',');
        const result = [];
        rawArray.forEach(item => result.push(VcfHighlightConditionService._removeExtraQuotes(item.trim())));
        return result;
    }

    static _removeExtraQuotes(str) {
        return str.replace(/(^('|"|\\"|\\')|('|"|\\"|\\')$)/g, '');
    }

    static _prepareCondition(condition) {
        condition = condition.replace(/(^\s*\()|(\)\s*$)/g, '');
        if (Object.values(VcfHighlightConditionService.conditionOperationList).some(c => condition.includes(c))) {
            condition = VcfHighlightConditionService._parseCondition(condition);
        } else {
            condition = VcfHighlightConditionService._parseExpression(condition);
        }
        return condition;
    }

    static _validateFullCondition(condition) {
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

    static parseFullCondition(condition) {
        return VcfHighlightConditionService._validateFullCondition(condition)
            ? VcfHighlightConditionService._parseCondition(condition)
            : null;
    }

    static _parseCondition(condition) {
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

        Object.values(VcfHighlightConditionService.conditionOperationList).forEach(op => {
            const index = condition.indexOf(op, i + 1);
            if (~index && (opIndex > index)) {
                opIndex = index;
                operator = op;
            }
        });

        if (opIndex === condition.length) {
            return VcfHighlightConditionService._prepareCondition(condition);
        } else {
            return {
                conditions: [
                    VcfHighlightConditionService._prepareCondition(condition.substring(0, opIndex).trim()),
                    VcfHighlightConditionService._prepareCondition(condition.substring(opIndex + operator.length).trim())
                ],
                operator: operator,
                type: VcfHighlightConditionService.conditionTypeList.GROUP
            };
        }
    }

    static isHighlighted(variant, condition) {
        if (!variant || !condition || !condition.type) {
            return false;
        }
        switch (condition.type) {
            case VcfHighlightConditionService.conditionTypeList.GROUP: {
                return VcfHighlightConditionService._calculateCondition(condition, variant);
            }
            case VcfHighlightConditionService.conditionTypeList.EXPRESSION: {
                return VcfHighlightConditionService._calculateExpression(condition, variant);
            }
            default: {
                return false;
            }
        }
    }

    static _calculateCondition(condition, variant) {
        if (!condition.conditions) {
            return false;
        }
        let result = false;
        switch (condition.operator) {
            case VcfHighlightConditionService.conditionOperationList.AND: {
                result = true;
                condition.conditions.forEach(c => result = result && VcfHighlightConditionService.isHighlighted(variant, c));
                break;
            }
            case VcfHighlightConditionService.conditionOperationList.OR: {
                condition.conditions.forEach(c => result = result || VcfHighlightConditionService.isHighlighted(variant, c));
                break;
            }
        }
        return result;
    }

    // eslint-disable-next-line complexity
    static _calculateExpression(expression, variant) {
        if (!expression.field || !variant[expression.field]) {
            return false;
        }
        let result;
        switch (expression.operator) {
            case VcfHighlightConditionService.expressionOperationList.EQ: {
                result = expression.value === VcfHighlightConditionService._regularizeString(variant[expression.field]);
                break;
            }
            case VcfHighlightConditionService.expressionOperationList.NE: {
                result = expression.value === VcfHighlightConditionService._regularizeString(variant[expression.field]);
                break;
            }
            case VcfHighlightConditionService.expressionOperationList.GE: {
                result = variant[expression.field] >= expression.value;
                break;
            }
            case VcfHighlightConditionService.expressionOperationList.GT: {
                result = variant[expression.field] > expression.value;
                break;
            }
            case VcfHighlightConditionService.expressionOperationList.LE: {
                result = variant[expression.field] <= expression.value;
                break;
            }
            case VcfHighlightConditionService.expressionOperationList.LT: {
                result = variant[expression.field] < expression.value;
                break;
            }
            case VcfHighlightConditionService.expressionOperationList.IN: {
                result = Array.isArray(expression.value) && expression.value.includes(variant[expression.field]);
                break;
            }
            case VcfHighlightConditionService.expressionOperationList.NI: {
                result = Array.isArray(expression.value) && !expression.value.includes(variant[expression.field]);
                break;
            }
            default: {
                result = false;
            }
        }
        return result;
    }

    static getFieldSet(parsedHighlightProfile) {
        const result = parsedHighlightProfile.reduce(
            (acc, profile) =>
                new Set([...acc, ...VcfHighlightConditionService._getFieldsFromCondition(profile.parsedCondition)]),
            new Set()
        );
        return Array.from(result);
    }

    static _getFieldsFromCondition(condition) {
        let result = new Set();
        if (condition.type === VcfHighlightConditionService.conditionTypeList.GROUP) {
            result = new Set([
                ...result,
                ...condition.conditions.reduce(
                    (acc, condition) =>
                        new Set([...acc, ...VcfHighlightConditionService._getFieldsFromCondition(condition)]),
                    result
                )
            ]);
        } else {
            result.add(condition.field);
        }
        return result;
    }

    static _regularizeString(value) {
        if (!isNaN(parseFloat(value))) {
            return parseFloat(value).toString();
        } else if (!isNaN(parseInt(value))) {
            return parseInt(value).toString();
        }
        return value.toLowerCase();
    }
}