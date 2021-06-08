/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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

package com.epam.catgenome.constant;

/**
 * Created with IntelliJ IDEA.
 * Date: 19.10.15
 * Time: 11:45
 * <p>
 * This class contains constants for system messages from catgenome-messages.properties
 * </p>
 */
public final class MessagesConstants {

    public static final String ERROR_FILES_MISSING_RESOURCE_AT_PATH = "error.files.missing.resource.at.path";
    public static final String ERROR_NO_CHROMOSOME_NAME = "error.no.chromosome.name";
    public static final String ERROR_NO_SUCH_FILE = "error.no.such.file";
    public static final String ERROR_CHROMOSOME_ID_NOT_FOUND = "";
    public static final String ERROR_REGISTER_FILE = "error.register.file";
    public static final String ERROR_CHROMOSOME_NAME_NOT_FOUND = "error.chromosome.name.not.found";
    public static final String ERROR_LENGTH_ABOVE_ZERO = "error.length.above.zero";
    public static final String ERROR_START_POSITION_ABOVE_ZERO = "error.start.position.above.zero";
    public static final String ERROR_WRONG_SIGNATURE = "wrong.signature";
    public static final String ERROR_LOGIC_LENGTH = "error.logic.length.longer.real";
    public static final String ERROR_NOT_SKIP_ENOUGH_NUCLEOTIDE = "error.not.skip.enough.nucleotide";
    public static final String ERROR_ARRAY_SO_SMALL = "error.array.so.small";
    public static final String ERROR_NO_NEW_ID = "error.no.new.id";
    public static final String ERROR_UNKNOWN_FASTA_SYMBOL = "error.unknown.fasta.symbol";
    public static final String ERROR_UNKNOWN_NIB_CODE = "error.unknown.code";
    public static final String ERROR_CHAR_CODE_ABOVE_ZERO = "error.char.code.above.zero";
    public static final String ERROR_INVALID_RESULT = "error.invalid.result";
    public static final String ERROR_INVALID_PARAM = "error.invalid.param";
    public static final String ERROR_NULL_PARAM = "error.null.param";
    public static final String ERROR_FILE_PROCESSING = "error.file.processing";
    public static final String ERROR_NOT_DELETE_FILE = "error.not.delete.file";
    public static final String ERROR_READ_FILE = "error.read.file";
    public static final String ERROR_INCORRECT_NAME_FILE = "error.incorrect.name.file";
    public static final String ERROR_START_POSITION = "error.start.position";
    public static final String INFO_FILES_STATUS_RESOURCE_AT_PATH = "info.files.status.resource.at.path";
    public static final String INFO_FILES_STATUS_ALREADY_EXISTS = "info.files.status.already.exists";
    public static final String ERROR_FILES_STATUS_ALREADY_EXISTS = "error.files.status.already.exists";
    public static final String INFO_FILE_DELETE = "info.files.delete";
    public static final String ERROR_FILE_DELETE = "error.files.delete";
    public static final String ERROR_FILE_IN_USE = "error.files.in.use";
    public static final String ERROR_FILE_IN_USE_AS_ANNOTATION = "error.files.in.use.as.annotation";
    public static final String ERROR_FILE_IN_LINK = "error.files.in.link";
    public static final String ERROR_EMPTY_FOLDER= "error.empty.folder";
    public static final String ERROR_FEATURE_FILE_READING = "error.feature.file.reading";
    public static final String ERROR_FILE_NAME_EXISTS = "error.file.name.already.exists";

    public static final String ERROR_GENEID_NOT_SPECIFIED = "error.no.geneid";

    public static final String ERROR_VARIATIONID_NOT_SPECIFIED = "error.no.variationid";
    public static final String ERROR_SPECIES_NOT_SPECIFIED = "error.no.species";
    public static final String ERROR_INDEX_NOT_SPECIFIED = "error.no.index.cache";
    public static final String ERROR_INDEX_URL_NOT_SPECIFIED = "error.no.index.url.cache";
    public static final String ERROR_CHROMOSOME_NOT_SPECIFIED = "error.no.chromosome";
    public static final String ERROR_STARTPOSITION_NOT_SPECIFIED = "error.no.startposition";
    public static final String ERROR_FINISHPOSITION_NOT_SPECIFIED = "error.no.finishposition";

    public static final String ERROR_PROJECT_NAME_EXISTS = "error.project.name.already.exists";
    public static final String ERROR_PROJECT_NOT_FOUND = "error.project.not.found";

    public static final String ERROR_UNSUPPORTED_FEATURE_FILE_TYPE = "error.unsupported.featurefiletype";
    public static final String ERROR_UNSUPPORTED_FEATURE_FILE_SORT_TYPE = "error.unsupported.featurefilesorttype";
    public static final String INFO_BOUNDS_METADATA_WRITE = "info.bounds.metadata.write";
    public static final String INFO_BOUNDS_METADATA_LOAD = "info.bounds.metadata.load";
    public static final String INFO_HISTOGRAM_WRITE = "info.histogram.write";
    public static final String INFO_HISTOGRAM_LOAD = "info.histogram.load";

    //Track validation errors
    public static final String ERROR_INVALID_PARAM_TRACK_INDEXES_BELOW_ZERO = "error.invalid.param.track.indexes." +
                                                                              "below.zero";
    public static final String ERROR_INVALID_PARAM_TRACK_SCALE_FACTOR_BELOW_ZERO = "error.invalid.param.track.scale." +
                                                                                   "factor.below.zero";
    public static final String ERROR_INVALID_PARAM_TRACK_START_GREATER_THEN_END = "error.invalid.param.track.indexes" +
                                                                                  ".start.greater.end";
    public static final String ERROR_INVALID_PARAM_QUERY_SO_LARGE = "error.invalid.param.large.query";
    public static final String ERROR_INVALID_PARAM_TRACK_END_GREATER_CHROMOSOME_SIZE = "error.invalid.param.track.end" +
                                                                                       ".greater.chromosome";
    public static final String ERROR_INVALID_PARAM_TRACK_IS_NULL = "error.invalid.param.query.null";

    //Common to all feature files
    public static final String ERROR_INVALID_CONTIG = "error.file.illegal.contig";
    public static final String ERROR_UNSORTED_FILE = "error.file.unsorted";
    public static final String ERROR_REFERENCE_ID_NULL = "error.reference.id.null";
    public static final String ERROR_REFERENCE_READING = "error.reference.reading";
    public static final String ERROR_REFERENCE_REGISTRATION_PARAMS = "error.reference.registration.params.ambiguous";
    public static final String ERROR_DURING_SORTING = "error.file.sorting";
    public static final String INFO_SORT_SUCCESS = "info.file.sorting";
    public static final String ERROR_ILLEGAL_FEATURE_FILE_FORMAT = "error.file.feature.illegal.file.format";
    public static final String ERROR_ANNOTATION_FILE_ALREADY_EXIST = "error.annotation.file.feature.already.exist";
    public static final String ERROR_ANNOTATION_FILE_NOT_EXIST = "error.annotation.file.feature.not.exist";
    public static final String ERROR_ILLEGAL_REFERENCE_FOR_ANNOTATION = "error.illegal.reference.for.annotation";
    public static final String ERROR_UNSUPPORTED_OPERATION = "error.unsupported.operation";

    //Feature index
    public static final String INFO_FEATURE_INDEX_NOT_FOUND = "info.feature.index.not.found";
    public static final String INFO_FEATURE_INDEX_LOADING = "info.feature.index.loading";
    public static final String INFO_FEATURE_INDEX_WRITING = "info.feature.index.writing";
    public static final String ERROR_FEATURE_INEDX_TOO_LARGE = "error.feature.index.too.large";
    public static final String INFO_FEATURE_INDEX_DONE = "info.feature.index.done";
    public static final String ERROR_FEATURE_INDEX_WRITING = "error.feature.index.writing";
    public static final String INFO_FEATURE_INDEX_WRITING_FOR_PROJECT = "info.feature.index.writing.for.project";
    public static final String INFO_FEATURE_INDEX_CHROMOSOME_WROTE = "info.feature.index.chromosome.wrote";
    public static final String ERROR_FEATURE_INDEX_WRITING_WRONG_PARAMETER_TYPE="error.feature.index.writing.wrong." +
            "parameter.type";
    public static final String DEBUG_FEATURE_INDEX_QUERY_TIME = "debug.feature.index.query.time";
    public static final String ERROR_FEATURE_INDEX_SEARCH_FAILED = "error.feature.index.search.failed";
    public static final String ERROR_FEATURE_INDEX_INVALID_NUMBER_FORMAT = "error.feature.index.invalid.number.format";

    public static final String INFO_UNREGISTER = "info.unregister.done";
    //Genes
    public static final String ERROR_UNSUPPORTED_GENE_FILE_EXTESION = "error.unsupported.gene.file.extension";
    public static final String INFO_GENE_INDEX_WRITING = "info.gene.index.write";
    public static final String INFO_GENE_REGISTER = "info.gene.register";
    public static final String INFO_GENE_UPLOAD = "info.gene.upload";
    public static final String ERROR_GENE_BATCH_LOAD = "error.gene.batch.load";
    public static final String DEBUG_GENE_BATCH_LOAD = "debug.gene.batch.load";
    public static final String DEBUG_GENE_EXONS_LOAD = "debug.gene.exons.load";
    public static final String ERROR_UNSUPPORTED_GENE_FILE_TYPE = "error.unsupported.genefiletype";
    public static final String ERROR_HELPER_FILE_DOES_NOT_EXIST = "error.helper.file.does.not.exist";
    public static final String ERROR_INVALID_NUCLEOTIDE = "error.invalid.nucleotide";

    //VCF
    public static final String ERROR_UNSUPPORTED_VCF_FILE_EXTESION = "error.unsupported.vcf.file.extension";
    public static final String INFO_VCF_INDEX_WRITING = "info.vcf.index.write";
    public static final String INFO_VCF_REGISTER = "info.vcf.register";
    public static final String INFO_VCF_UPLOAD = "info.vcf.upload";
    public static final String ERROR_VCF_ID_INVALID = "error.vcf.id.invalid";
    public static final String ERROR_VCF_HEADER = "error.vcf.header";
    public static final String ERROR_VCF_INDEX = "error.vcf.index";
    public static final String ERROR_VCF_READING = "error.vcf.reading";
    public static final String ERROR_NO_SUCH_VARIATION = "error.no.such.variation";
    public static final String ERROR_ILLEGAL_TEMPLATE_FORMAT = "error.vcf.illegal.template";
    public static final String ERROR_ILLEGAL_INFO_FORMAT = "error.vcf.wrong.info.format";

    //BAM
    public static final String WRONG_BAM_INDEX_FILE = "error.bam.index.file";
    public static final String WRONG_HEADER_BAM_FILE = "error.header.bam.file";
    public static final String WRONG_HEADER_BAM_FILE_EMPTY_FILE = "error.header.bam.file.empty";

    //BLAT SEARCH
    public static final String NULL_SPECIES_FOR_GENOME = "error.reference.species.empty";
    public static final String ERROR_NO_SUCH_SPECIES = "error.no.such.species";
    public static final String ERROR_SPECIES_EXISTS = "error.species.already.exists";
    public static final String INFO_UNREGISTERED_SPECIES = "info.unregistered.species";

    //WIG
    public static final String WRONG_WIG_FILE = "error.wig.file";
    public static final String WRONG_BED_GRAPH_FILE = "error.bedgraph.file";


    //S3
    public static final String ERROR_S3_BUCKET = "error.not.s3.bucket";

    //CACHE
    public static final String INFO_RECORD_IN_CACHE = "info.record.in.cache";

    //DEBUG INFO
    public static final String DEBUG_FILE_READING = "debug.file.reading.operation";
    public static final String DEBUG_FILE_OPENING = "debug.file.opening";
    public static final String DEBUG_GET_ITERATOR_QUERY = "debug.get.iterator.query";
    public static final String DEBUG_HEAVY_PROCESSING_OPERATION = "debug.heavy.processing.operation";
    public static final String DEBUG_THREAD_STARTS = "debug.thread.starts";
    public static final String DEBUG_THREAD_ENDS = "debug.thread.ends";
    public static final String DEBUG_THREAD_READER_CREATED = "debug.thread.reader.created";
    public static final String DEBUG_THREAD_INTERVAL = "debug.thread.interval";
    public static final String DEBUG_THREAD_QUERY_TIME = "debug.thread.query.time";
    public static final String DEBUG_QUERY_TIME = "debug.query.time";
    public static final String DEBUG_THREAD_WALKTHROUGH_TIME = "debug.thread.walkthrough.time";
    public static final String DEBUG_WALKTHROUGH_TIME = "debug.walkthrough.time";

    //FILE
    public static final String ERROR_FILE_NOT_FOUND = "error.file.not.found";
    public static final String ERROR_FILE_HEADER_READING = "error.file.header.reading";
    public static final String ERROR_BIO_ID_NOT_FOUND = "error.file.id.not.found";
    public static final String ERROR_BIO_NAME_NOT_FOUND = "error.file.name.not.found";
    public static final String ERROR_UNSUPPORTED_FILE_FORMAT = "error.unsupported.file.format";
    public static final String ERROR_FILE_CORRUPTED_OR_EMPTY = "error.file.corrupted.or.empty";
    public static final String ERROR_DIRECTORY_NOT_FOUND = "error.directory.not.found";

    //PROJECT
    public static final String INFO_PROJECT_DELETED = "info.project.deleted";
    public static final String ERROR_PROJECT_FEATURE_INDEX_NOT_FOUND = "error.project.feature.index.not.found";
    public static final String ERROR_PROJECT_FILE_NOT_FOUND = "error.project.file.not.found";
    public static final String ERROR_PROJECT_INVALID_REFERENCE = "error.project.reference.invalid";
    public static final String ERROR_PROJECT_NON_MATCHING_REFERENCE = "error.project.reference.not.match";
    public static final String ERROR_PROJECT_DELETE_HAS_NESTED = "error.project.delete.has.nested";

    //DOWNLOAD FILE
    public static final String INFO_START_DOWNLOAD_FILE = "info.start.download.file";

    public static final String ERROR_LARGE_FILE_FOR_DOWNLOAD = "error.large.file.for.download";
    public static final String ERROR_DOWNLOAD_TIMEOUT = "error.download.timeout";
    public static final String ERROR_UNKNOWN_HOST = "error.unknown.host";


    //EXTERNAL_DB
    public static final String ERROR_PARSING = "error.parsing.exception";
    public static final String ERROR_NO_DATA_FOR_URL = "error.no.data.for.url";
    public static final String ERROR_UNEXPECTED_FORMAT = "error.error.unexpected.format";
    public static final String ERROR_NO_RESULT_BY_EXTERNAL_DB = "error.no.result.by.external.db";

    //BED
    public static final String ERROR_BED_PARSING = "error.bed.parsing.exception";

    //LOGGER
    public static final String ERROR_LOGGER_JSON_FILE_INVALID = "logger.error.json.file.invalid";

    //SHORT_URLS
    public static final String ERROR_URL_WAS_EXPIRED = "error.url.was.expired";
    public static final String INFO_ALIAS_ALREADY_EXIST_MASSAGE = "info.alias.already.exist";

    //JWT
    public static final String ERROR_NO_JWT_PRIVATE_KEY_CONFIGURED = "error.no.jwt.private.key.configured";

    //ACL SERVICES MESSAGES
    public static final String ERROR_MUTABLE_ACL_RETURN = "error.mutable.acl.return";
    // AUTHORIZATION
    public static final String ERROR_USER_NAME_NOT_FOUND = "error.user.name.not.found";
    public static final String ERROR_ACL_CLASS_NOT_SUPPORTED = "error.acl.class.not.supported";
    public static final String ERROR_PERMISSION_PARAM_REQUIRED = "error.permission.param.required";
    public static final String ERROR_ROLE_OR_USER_NOT_FOUND = "error.role.or.user.not.found";

    // USER
    public static final String ERROR_USER_NAME_REQUIRED = "error.user.name.required";
    public static final String ERROR_USER_NAME_EXISTS = "error.user.name.exists";
    public static final String ERROR_ROLE_ID_NOT_FOUND = "error.role.id.not.found";
    public static final String ERROR_USER_ID_NOT_FOUND = "error.user.id.not.found";
    public static final String ERROR_USER_LIST_EMPTY = "error.user.list.empty";
    public static final String ERROR_ROLE_NAME_REQUIRED = "error.role.name.required";
    public static final String ERROR_ROLE_ALREADY_EXIST = "error.role.already.exists";
    public static final String ERROR_NO_GROUP_WAS_FOUND = "error.no.group.was.found";

    // BLAST TASK
    public static final String ERROR_TASK_NOT_FOUND = "error.blast.task.not.found";
    public static final String ERROR_TASK_CAN_NOT_BE_DELETED = "error.blast.task.cant.be.deleted";
    public static final String ERROR_BLAST_REQUEST = "error.blast.request.unexpected";
    public static final String ERROR_BLAST_ORGANISMS = "error.blast.organisms";
    public static final String ERROR_DATABASE_NOT_FOUND = "error.blast.database.not.found";

    private MessagesConstants() {
        // No-op
    }
}
