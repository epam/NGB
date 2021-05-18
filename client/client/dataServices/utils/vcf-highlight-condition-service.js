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
    try {
        const exec = /^[ ]*([^'" >=<!]+|'[^']*'|"[^"]*")[ ]*(==|!=|>=|<=|>|<|in|notin)[ ]*(.*)$/i.exec(expression);
        if (!exec || exec.length < 4) {
            throw new Error('Unknown format');
        }
        const name = parseQuotedString(exec[1]);
        const operator = exec[2];
        if (!name) {
            throw new Error('Unknown name format');
        }
        let value = exec[3];
        if (/^(in|notin)$/i.test(operator)) {
            value = extractAsArray(value);
        } else {
            value = _regularizeString(parseQuotedString(value));
        }
        if (/^(>=|<=|>|<)$/.test(operator)) {
            const number = Number(value);
            if (Number.isNaN(number)) {
                throw new Error(`wrong value: ${value}. Correct number formats: "1" "-3" "-0.32" `);
            } else {
                value = number;
            }
        }
        if (value === null || value === undefined) {
            throw new Error('empty value');
        }
        return {
            field: name,
            operator,
            type: conditionTypeList.EXPRESSION,
            value
        };
    } catch (e) {
        // eslint-disable-next-line
        console.warn(`Wrong expression: ${expression}. Error: ${e.message}`);
        return null;
    }
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
            // eslint-disable-next-line
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
            result = Number(variant[expression.field]) >= Number(expression.value);
            break;
        }
        case expressionOperationList.GT: {
            result = Number(variant[expression.field]) > Number(expression.value);
            break;
        }
        case expressionOperationList.LE: {
            result = Number(variant[expression.field]) <= Number(expression.value);
            break;
        }
        case expressionOperationList.LT: {
            result = Number(variant[expression.field]) < Number(expression.value);
            break;
        }
        case expressionOperationList.IN: {
            result = Array.isArray(expression.value) && expression.value.includes(_regularizeString(variant[expression.field]));
            break;
        }
        case expressionOperationList.NI: {
            result = Array.isArray(expression.value) && !expression.value.includes(_regularizeString(variant[expression.field]));
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
    if (Array.isArray(value)) {
        return `[${value.join(', ')}]`;
    }
    if (typeof value === 'string' && (new RegExp('-?[^0-9\\.\\s]')).test(value)) {
        return (value || '').toLowerCase();
    } else if (!isNaN(parseFloat(value))) {
        return parseFloat(value).toString();
    } else if (!isNaN(parseInt(value))) {
        return parseInt(value).toString();
    }
    return value;
}

function removeQuotas (test, quotas) {
    if (!test || !test.startsWith(quotas) || !test.endsWith(quotas)) {
        return null;
    }
    test = test.slice(1, -1);
    if (test.includes(quotas)) {
        return null;
    }
    return test;
}

function parseQuotedString (value) {
    if (!value) {
        throw new Error('empty value');
    }
    const initialValue = value;
    value = value.trim();
    if (value.startsWith('\'')) {
        value = removeQuotas(value, '\'');
        if (value === null) {
            throw new Error(`wrong string: ${initialValue}`);
        }
        return value;
    }
    if (value.startsWith('"')) {
        value = removeQuotas(value, '"');
        if (value === null) {
            throw new Error(`wrong string: ${initialValue}`);
        }
        return value;
    }
    if (/['"]/.test(value)) {
        throw new Error(`wrong string: ${initialValue}`);
    }
    return value;
}

function split (value) {
    const regExp = /[ ]*('[^']*?'|"[^"]*?"|[^'"]*?)(,|$)/;
    const result = [];
    let exec = regExp.exec(value);
    while (value.length && exec && exec.length > 1) {
        result.push(exec[1]);
        value = value.slice(exec[0].length);
        exec = regExp.exec(value);
    }
    return result;
}

function extractAsArray (value) {
    if (!value) {
        return null;
    }
    const exec = /^[ ]*\[(.*)\][ ]*$/i.exec(value);
    if (exec && exec.length === 2) {
        return split((exec[1] || ''))
            .map(o => o.trim())
            .map(parseQuotedString)
            .map(_regularizeString);
    }
    return null;
}
