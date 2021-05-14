const expressionOperationList = {
    EQ: '==',
    GE: '>=',
    GT: '>',
    IN: 'in',
    LE: '<=',
    LT: '<',
    NE: '!=',
    NI: 'notin'
};
const conditionOperationList = {
    AND: 'and',
    OR: 'or'
};
const conditionTypeList = {
    EXPRESSION: 'expression',
    GROUP: 'group'
};

function _parseExpression(expression) {
    const rawParsedExpression = expression
        .split(new RegExp(`(${Object.values(expressionOperationList).join('|')})`))
        .map(e => e.trim());
    const operator = rawParsedExpression[1];
    let value = rawParsedExpression[2];

    if (!operator) {
        console.warn(`Invalid expression ${expression}`);
        return null;
    }
    if ([expressionOperationList.EQ,
        expressionOperationList.NE].includes(operator)) {
        value = _removeExtraQuotes(value);
        value = _regularizeString(value);
    }
    if ([expressionOperationList.GE,
        expressionOperationList.GT,
        expressionOperationList.LE,
        expressionOperationList.LT].includes(operator)) {
        value = _removeExtraQuotes(value);
        value = value.includes('.') ? parseFloat(value) : parseInt(value);
    }
    if ([expressionOperationList.IN,
        expressionOperationList.NI].includes(operator)) {
        value = _prepareArray(value);
    }
    return {
        field: _removeExtraQuotes(rawParsedExpression[0]),
        operator: operator,
        type: conditionTypeList.EXPRESSION,
        value: value
    };
}

function _prepareArray(stringed) {
    const rawArray = stringed.replace(/(^\s*\[)|(]\s*$)/g, '').split(',');
    const result = [];
    rawArray.forEach(item => result.push(_removeExtraQuotes(item.trim())));
    return result;
}

function _removeExtraQuotes(str) {
    return str.replace(/(^('|"|\\"|\\')|('|"|\\"|\\')$)/g, '');
}

function _prepareCondition(condition) {
    condition = condition.replace(/(^\s*\()|(\)\s*$)/g, '');
    if (Object.values(conditionOperationList).some(c => condition.includes(c))) {
        condition = _parseCondition(condition);
    } else {
        condition = _parseExpression(condition);
    }
    return condition;
}

function _validateFullCondition(condition) {
    let depth = 0;
    for (let i = 0; i < condition.length; i++) {
        if (condition[i] === '(') depth++;
        if (condition[i] === ')') depth--;
        if (depth < 0) {
            console.warn(`Invalid condition ${condition} at position ${i}`);
            return false;
        }
    }
    return true;
}

export function parseFullCondition(condition) {
    return _validateFullCondition(condition)
        ? _parseCondition(condition)
        : null;
}

function _parseCondition(condition) {
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

    Object.values(conditionOperationList).forEach(op => {
        const index = condition.indexOf(op, i + 1);
        if (~index && (opIndex > index)) {
            opIndex = index;
            operator = op;
        }
    });

    if (opIndex === condition.length) {
        return _prepareCondition(condition);
    } else {
        return {
            conditions: [
                _prepareCondition(condition.substring(0, opIndex).trim()),
                _prepareCondition(condition.substring(opIndex + operator.length).trim())
            ],
            operator: operator,
            type: conditionTypeList.GROUP
        };
    }
}

export function isHighlighted(variant, condition) {
    return _calculateHighlighted(variant, condition);
}

function _calculateHighlighted(variant, condition) {
    if (!variant || !condition || !condition.type) {
        return null;
    }
    switch (condition.type) {
        case conditionTypeList.GROUP: {
            return _calculateCondition(condition, variant);
        }
        case conditionTypeList.EXPRESSION: {
            return _calculateExpression(condition, variant);
        }
        default: {
            return false;
        }
    }
}

function _calculateCondition(condition, variant) {
    if (!condition.conditions) {
        return false;
    }
    let result = false;
    let calculated = null;
    switch (condition.operator) {
        case conditionOperationList.AND: {
            result = true;
            for (const c of condition.conditions) {
                calculated = _calculateHighlighted(variant, c);
                if (calculated === null) {
                    result = false;
                    break;
                }
                result = result && calculated;
            }
            break;
        }
        case conditionOperationList.OR: {
            for (const c of condition.conditions) {
                calculated = _calculateHighlighted(variant, c);
                if (calculated === null) {
                    result = false;
                    break;
                }
                result = result || calculated;
            }
            break;
        }
    }
    return result;
}

// eslint-disable-next-line complexity
function _calculateExpression(expression, variant) {
    if (!expression.field || !variant[expression.field]) {
        return false;
    }
    let result;
    switch (expression.operator) {
        case expressionOperationList.EQ: {
            result = expression.value === _regularizeString(variant[expression.field]);
            break;
        }
        case expressionOperationList.NE: {
            result = expression.value !== _regularizeString(variant[expression.field]);
            break;
        }
        case expressionOperationList.GE: {
            result = variant[expression.field] >= expression.value;
            break;
        }
        case expressionOperationList.GT: {
            result = variant[expression.field] > expression.value;
            break;
        }
        case expressionOperationList.LE: {
            result = variant[expression.field] <= expression.value;
            break;
        }
        case expressionOperationList.LT: {
            result = variant[expression.field] < expression.value;
            break;
        }
        case expressionOperationList.IN: {
            result = Array.isArray(expression.value) && expression.value.includes(variant[expression.field]);
            break;
        }
        case expressionOperationList.NI: {
            result = Array.isArray(expression.value) && !expression.value.includes(variant[expression.field]);
            break;
        }
        default: {
            result = false;
        }
    }
    return result;
}

export function getFieldSet(parsedHighlightProfile) {
    const result = parsedHighlightProfile.reduce(
        (acc, profile) =>
            new Set([...acc, ..._getFieldsFromCondition(profile.parsedCondition)]),
        new Set()
    );
    return Array.from(result);
}

function _getFieldsFromCondition(condition) {
    let result = new Set();
    if (!condition) {
        return result;
    }
    if (condition.type === conditionTypeList.GROUP) {
        result = new Set([
            ...result,
            ...condition.conditions.reduce(
                (acc, condition) =>
                    new Set([...acc, ..._getFieldsFromCondition(condition)]),
                result
            )
        ]);
    } else {
        result.add(condition.field);
    }
    return result;
}

function _regularizeString(value) {
    if (!isNaN(parseFloat(value))) {
        return parseFloat(value).toString();
    } else if (!isNaN(parseInt(value))) {
        return parseInt(value).toString();
    }
    return value.toLowerCase();
}
