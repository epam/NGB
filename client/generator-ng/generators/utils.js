'use strict';

const _ = require('lodash');

const Utils = {};

/**
 *
 * @param {string} fileName File name.
 * @param {string} prefix File name prefix.
 * @returns {string} Destination file name.
 */
Utils.getDestinationFileName = (fileName, prefix) => {
    const name = fileName.replace('.tpl', '');
    if (_.includes(['index.js', 'readme.md'], name)) {
        return name;
    }
    return `${prefix}.${name}`;
};

/**
 *
 * @param {boolean|function} isRequired Template file required flag.
 * @param {object} userChoice User answers on initial dialog
 * @returns {Boolean} Is the flag true.
 */
Utils.isFileRequired = (isRequired, userChoice) => _.isFunction(isRequired) ? isRequired(userChoice) : isRequired;

/**
 *
 * @param {string} name Component or Service name
 * @returns {{name: {string}, camelName: {string}, CapitalName: string}} Default template parameters.
 */
Utils.getDefaultTemplateParameters = (name) => ({
    name: name,
    camelName: _.camelCase(name),
    CapitalName: _.upperFirst(_.camelCase(name))
});

module.exports = Utils;
