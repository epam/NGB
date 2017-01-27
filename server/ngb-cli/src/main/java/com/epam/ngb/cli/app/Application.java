/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.epam.ngb.cli.app;

import static com.epam.ngb.cli.constants.MessageConstants.ERROR_MISSING_COMMAND;
import static com.epam.ngb.cli.constants.MessageConstants.ERROR_PARSE_COMMAND_LINE;
import static com.epam.ngb.cli.constants.MessageConstants.ERROR_UNKNOWN_COMMAND;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.CommandConfiguration;
import com.epam.ngb.cli.manager.command.CommandManager;

/**
 * Main Application Class. It is used to parse command line and launch a specified tool.
 * Application supports a set of commands for sending HTTP requests to NGB server and setting
 * application configuration.
 * Command line arguments are divided in to options (with preceding '-' or '--' symbols) and
 * arguments. Options are handled with the help of args4j library and arguments are processed
 * by the CLI classes.
 */
public final class Application {

    //TODO: move documentation to an outer file
    private static final String HELP = "CLI for NGB server\n"
            + "All objects can be addressed by biologicalDataItemID or by name.\n\n"
            + "REFERENCE commands:\n"
            + "rr\treg_ref\t\t: registers a reference file\t{rr \\path\\to\\file.fa -n grch38}\n"
            + "dr\tdel_ref\t\t: unregisters a reference file\t{dr grch38}\n"
            + "lr\tlist_ref\t: lists all reference files, registered on the server\t{lr}\n"
            + "ag\tadd_genes\t: adds a gene file to the reference\t{ag grch38 genes.gtf}\n"
            + "rg\tremove_genes\t: removes a gene file from the reference\t{ag grch38 genes.gtf}\n\n"
            + "FILE commands:\n"
            + "rf\tref_file\t: registers a feature file for a specified reference\t"
            + "{rf grch38 \\path\\to\\file.bam?\\path\\to\\file.bam.bai -n my_vcf}\n"
            + "df\tdel_file\t: deletes a feature file one\t{df my_vcf}\n"
            + "if\tindex_file\t\t: creates a feature index for a file. \t {if genes.gtf}\n\n"
            + "SEARCH commands:\n"
            + "s\tsearch\t\t: finds a reference or feature file by it's name, "
            + "search can be configured by a '-c' option"
            + "\t{s -l vcf}\n\n"
            + "DATASET commands:\n"
            + "rd\treg_dataset\t: creates a new dataset (ex project) for a specified reference\t"
            + "{rd grch38 my_dataset}\n"
            + "add\tadd_dataset\t: adds files to a dataset\t"
            + "{add my_dataset sample.bam sample.vcf}\n"
            + "rmd\tremove_dataset\t: removes files from a dataset\t{rmd my_dataset my_vcf}\n"
            + "dd\tdel_dataset\t: removes a dataset\t{dd my_dataset}\n"
            + "md\tmove_dataset\t: changes the dataset parent to the dataset specified by the \"-p\" option,"
            + " if option isn't provided, the dataset will be moved to the top level of the datasets hierarchy"
            + "\t{md my_dataset -p parent}\n"
            + "ld\tlist_dataset\t: lists all datasets, registered on the server\t{ld}\n\n"
            + "CONFIGURATION COMMANDS\n"
            + "srv\tset_srv\t\t: sets working server url for CLI\tsrv http://{SERVER_IP_OR_NAME}:"
            + "{SERVER_PORT}/catgenome\n\n"
            + "Available options (options may go before, after or between the arguments):\n";


    @Option(name = "-n", usage = "explicitly specifies file name for registration", aliases = {
            "--name"})
    private String name;

    @Option(name = "-t", usage = "output request's result in a table, otherwise the output of all "
            + "commands will be ignored, excluding search and list commands", aliases = {
            "--table"})
    private boolean printTable = false;

    @Option(name = "-j", usage = "output request's result in a json, otherwise the output of all "
            + "commands will be ignored, excluding search and list commands", aliases = {
            "--json"})
    private boolean printJson = false;

    @Option(name = "-c", usage = "path to the configuration file", metaVar = "PATH", aliases = {
            "--config", "--configuration"})
    private String config;

    @Option(name = "-l", usage = "use non-strict search for file finding", aliases = {"--like"})
    private boolean nonStrictSearch = false;

    @Option(name = "-p", usage = "specifies dataset parent for registration", aliases = {"--parent"})
    private String parent;

    @Option(name = "-g", usage = "specifies a gene file for reference registration", aliases = {"--genes"})
    private String genes;

    @Option(name = "-ni", usage = "defines if a feature index should not be created for registered VCF or GFF/GTF file",
        aliases = {"--no_index"})
    private boolean doNotIndex = false;

    @Option(name = "-h", usage = "prints help", aliases = {"--help"})
    private boolean helpOption;

    @Argument
    private List<String> arguments;

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    Application() {
        //no op
    }

    /**
     * Application entry point
     * @param args
     */
    public static void main(String[] args) {
        Application app = new Application();
        app.run(args);
    }

    //method is package local for the purpose of unit testing
    int run(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
            if (helpOption) {
                LOGGER.info(HELP);
                parser.printUsage(System.out);
                return 0;
            }
            checkArguments(parser);
            return createAndLaunchCommandManager();
        } catch (CmdLineException e) {
            LOGGER.debug(e.getMessage(), e);
            LOGGER.error(MessageConstants.getMessage(ERROR_PARSE_COMMAND_LINE, Arrays.stream(args)
                    .collect(Collectors.joining(" "))));
            LOGGER.info(HELP);
            parser.printUsage(System.out);
        } catch (ApplicationException | IllegalArgumentException e) {
            LOGGER.debug(e.getMessage(), e);
            LOGGER.error(e.getMessage());
        }
        return 1;
    }

    private void checkArguments(CmdLineParser parser) throws CmdLineException {
        if (arguments == null || arguments.isEmpty()) {
            throw new CmdLineException(parser, MessageConstants.getMessage(ERROR_MISSING_COMMAND),
                    new ApplicationException(MessageConstants.getMessage(ERROR_MISSING_COMMAND)));
        }
    }

    private int createAndLaunchCommandManager() {
        String command = arguments.get(0);
        ConfigurationLoader configLoader = new ConfigurationLoader();
        Properties commandProperties = configLoader.loadCommandProperties();
        String fullCommand = commandProperties.getProperty(command);
        if (fullCommand == null) {
            throw new ApplicationException(MessageConstants.getMessage(ERROR_UNKNOWN_COMMAND,
                    command));
        }
        Properties serverProperties = configLoader.loadServerConfiguration(config);
        ApplicationOptions options = getOptions();
        CommandConfiguration configuration = configLoader.loadCommandConfiguration(fullCommand);
        CommandManager commandManager =
                new CommandManager(fullCommand, configuration, serverProperties);
        return commandManager.run(arguments.subList(1, arguments.size()), options);
    }

    private ApplicationOptions getOptions() {
        ApplicationOptions options = new ApplicationOptions();
        if (name != null) {
            options.setName(name);
        }
        options.setParent(parent);
        options.setPrintTable(printTable);
        options.setPrintJson(printJson);
        options.setStrictSearch(!nonStrictSearch);
        options.setGeneFile(genes);
        if (doNotIndex) {
            options.setDoIndex(false);
        }
        return options;
    }
}
