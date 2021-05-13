/*
 * MIT License
 *
 * Copyright (c) 2016-2021 EPAM Systems
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
public class Application {

    //TODO: move documentation to an outer file
    private static final String HELP = "CLI for NGB server\n"
            + "All objects can be addressed by biologicalDataItemID or by name.\n\n"
            + "REFERENCE commands:\n"
            + "rr\treg_ref\t\t: registers a reference file\t{rr \\path\\to\\file.fa -n grch38}\n"
            + "dr\tdel_ref\t\t: unregisters a reference file\t{dr grch38}\n"
            + "lr\tlist_ref\t: lists all reference files, registered on the server\t{lr}\n"
            + "ag\tadd_genes\t: adds a gene file to the reference\t{ag grch38 genes.gtf}\n"
            + "an\tadd_ann\t: adds a annotation file to the reference\t{an grch38 annotations.gtf}\n"
            + "ran\tremove_ann\t: remove a annotation file from the list of reference annotation files"
            + "\t{ran grch38 annotations.gtf}\n"
            + "rg\tremove_genes\t: removes a gene file from the reference\t{rg grch38}\n\n"
            + "SPECIES commands:\n"
            + "rs\treg_spec\t: registers a species\t{rs \\species\\name \\species\\version}\n"
            + "ds\tdel_spec\t: unregisters a species \t{ds \"hg19\"}\n"
            + "as\tadd_spec\t: adds a registered species to the registered reference\t{as grch38 \"hg19\"}\n"
            + "remove_spec\t: removes a species from the reference\t{remove_spec grch38}\n"
            + "list_spec\t: lists all species, registered on the server\t{list_spec}\n\n"
            + "FILE commands:\n"
            + "rf\treg_file\t: registers a feature file for a specified reference\t"
            + "{rf grch38 \\path\\to\\file.bam?\\path\\to\\file.bam.bai -n my_vcf}\n"
            + "df\tdel_file\t: deletes a feature file one\t{df my_vcf}\n"
            + "if\tindex_file\t: creates a feature index for a file. \t {if genes.gtf}\n\n"
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
            + "ADDITIONAL commands:\n"
            + "url\t\t: generate url for displaying required files. "
                                       + "{url my_dataset}\n\n"
            + "SECURITY commands:\n"
            + "ru\treg_user\t: registers user (roles can be specified by -gr (--groups) option) " +
              "{ru example@example.com -gr Developers,OA}\n"
            + "du\tdel_user\t: deletes existing user by id or name {du example@example.com}\n"
            + "rgrp\treg_group\t: registers new group (option -u (--users) can be used to assign the " +
              "group on list of users) {rgrp example_group -u example@example.com,example2@example.com}\n"
            + "dgrp\tdel_group\t: deletes existing user group by id or name {dgrp group_name}\n"
            + "agrp\tadd_group\t: adds existing users to an existing group (users can be specified with option " +
              "-u (--users) by names or ids) {agrp group_name -u example@example.com,example2@example.com}\n"
            + "chmod\t\t: command to be used for granting permission {chmod rw+ --files <filename> " +
              "--users <username>}\n"
            + "TOOLS commands:\n"
            + "sort\t\t: sorts given feature file. If target path is not specified, sorted file will be stored in the "
            + "same folder as the original one with the `.sorted.` suffix in the name.\n"
            + "CONFIGURATION commands:\n"
            + "srv\tset_srv\t\t: sets working server url for CLI\tsrv http://{SERVER_IP_OR_NAME}:"
            + "{SERVER_PORT}/catgenome\n"
            + "v\tversion\t\t: prints CLI version to the console standard output\n"
            + "st\tset_token\t\t: sets JWT token to authorize CLI calls to NGB server\n\n"
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

    @Option(name = "-loc", usage = "location of view port in format: chr:start-end",  aliases = {"--location"})
    private String location;

    @Option(name = "-ngc", usage = "specifies if GC content shouldn't be calculated during reference registration",
            aliases = {"--nogccontent"})
    private boolean noGCContent = false;

    @Option(name = "-m", usage = "specifies amount of memory in megabytes to use when sorting (default: 500)",
            aliases = {"--max_memory"})
    private int maxMemory = 0;

    @Option(name = "-f", usage = "defines if a dataset will be force deleted",
            aliases = {"--force"})
    private boolean forceDeletion = false;

    @Option(name = "-pt", usage = "pretty name for datasets or biological data file",
            aliases = {"--pretty"})
    private String prettyName;

    @Option(name = "-nt", usage = "defines if tabix index shouldn't be rewritten during file reindexing",
            aliases = {"--no-tabix"})
    private boolean doNotCreateTabixIndex = false;

    @Option(name = "-s", usage = "specifies reference genome species version for registration", aliases = {"--species"})
    private String speciesVersion;

    @Option(name = "-ds", usage = "Datasets for permission update",  aliases = {"--datasets"})
    private String datasets;

    @Option(name = "-fl", usage = "Files for permission update",  aliases = {"--files"})
    private String files;

    @Option(name = "-u", usage = "Users for permission update",  aliases = {"--users"})
    private String users;

    @Option(name = "-gr", usage = "Groups for permission update",  aliases = {"--groups"})
    private String groups;

    @Option(name = "-perm", usage = "shows permissions", aliases = {"--permissions"})
    private boolean showPermissions = false;

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
        app.exit(app.run(args));
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
            exit(1);
        } catch (ApplicationException | IllegalArgumentException e) {
            LOGGER.debug(e.getMessage(), e);
            LOGGER.error(e.getMessage());
            exit(1);
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
        options.setLocation(location);
        options.setNoGCContent(noGCContent);
        options.setForceDeletion(forceDeletion);
        options.setPrettyName(prettyName);
        options.setSpeciesVersion(speciesVersion);
        options.setUsers(users);
        options.setGroups(groups);
        options.setFiles(files);
        options.setDatasets(datasets);
        if (doNotCreateTabixIndex) {
            options.setCreateTabixIndex(false);
        }
        if (doNotIndex) {
            options.setDoIndex(false);
        }
        options.setMaxMemory(maxMemory);
        options.setShowPermissions(showPermissions);
        return options;
    }

    protected void exit(int code) {
        System.exit(code);
    }
}
