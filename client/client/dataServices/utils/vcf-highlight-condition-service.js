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
        return {
            field: rawParsedExpression[0],
            operator: rawParsedExpression[1],
            type: this.conditionTypeList.EXPRESSION,
            value: rawParsedExpression[2]
        };
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
}