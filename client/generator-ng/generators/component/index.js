'use strict';

const yeoman = require('yeoman-generator');
const path = require('path');
const _ = require('lodash');
const utils = require('./../utils');
const fs = require('fs');

/**
 * Template files for a component.
 * Value is a flag whether this file must be always copied or based on the user choice
 * @type {object}
 */
const templateFiles = {
    'component.js.tpl': true,
    'index.js.tpl': true,
    'tpl.html.tpl': true,
    'service.js.tpl': true,
    'scss.tpl': (userChoice) => userChoice.includeCss,
    'controller.js.tpl': (userChoice) => userChoice.includeCtrl
};

module.exports = yeoman.Base.extend({

    constructor: function constructor() {
        yeoman.Base.apply(this, arguments);
        this.argument('componentName', {type: String, required: false});
    },

    prompting() {

        /*  const componentsFolder = path.join(this.destinationRoot(), this._destinationPath);

         // Need to take only folder - no dots in names.
         const componentsLevels = fs.readdirSync(componentsFolder)
         .filter((fsObj) => !_.includes(fsObj, '.'));

         const levelQuestion = {
         type: 'list',
         name: 'level',
         message: 'What level use to store the component?',
         default: '',
         choices: componentsLevels
         };*/

        const nameQuestion = {
            type: 'input',
            name: 'componentName',
            message: 'Please, input the component name',
            default: '',
            validate: (input) => _.isEmpty(input) ? 'Component name is required' : true
        };


        const questions = [/*levelQuestion*/];

        if (!this.componentName) {
            questions.unshift(nameQuestion);
        }

        return this.prompt(questions).then((answers) => {
            this.userChoice = {
                name: this.componentName || answers.componentName,

                level: 'components',
                includeCss: true,
                includeCtrl: true
            };
        });
    },

    writing() {

        const name = _.camelCase(this.userChoice.name);

        const level = this.userChoice.level;

        const tplParams = this._getTemplateParameters(name, this.userChoice);

        const dest = path.join(this.destinationRoot(), this._destinationPath, level, name);

        const copyFileFn = this._copyTemplateFile.bind(this, dest, tplParams, name);

        _.forOwn(templateFiles, (isRequired, fileName) => {
            const isThisFileRequired = utils.isFileRequired(isRequired, this.userChoice);

            if (isThisFileRequired) {
                copyFileFn(fileName);
            }
        });
    },

    /**
     * Compiles and copies a file from templates to destination
     * @param {string} dest Destination folder.
     * @param {object} params Template parameters.
     * @param {string} prefix File name prefix.
     * @param {string} fileName File name.
     */
    _copyTemplateFile(dest, params, prefix, fileName) {
        this.fs.copyTpl(
            this.templatePath(fileName),
            this.destinationPath(path.join(dest, utils.getDestinationFileName(fileName, prefix))),
            params
        );
    },

    /**
     *
     * @param {string} name Component name
     * @param {object} userChoice User answers on initial dialog
     * @returns {object} Template parameters.
     * @private
     */
    _getTemplateParameters(name, userChoice) {
        return _.assign({}, utils.getDefaultTemplateParameters(name), {
            includeCss: userChoice.includeCss,
            includeCtrl: userChoice.includeCtrl
        });
    },

    /**
     *
     * @returns {string} Destination path relative to the project root.
     * @private
     */
    get _destinationPath() {
        return '../client/app';
    }

});
