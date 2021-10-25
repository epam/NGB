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

package com.epam.ngb.cli.manager.command.handler.http;


import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.BaseEntity;
import com.epam.ngb.cli.entity.FeatureFile;
import com.epam.ngb.cli.entity.Reference;
import com.epam.ngb.cli.manager.command.handler.Command;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

/**
 * {@code {@link ReferenceRegistrationHandler}} represents a tool handling "delete_reference" command and
 * sending request to NGB server for reference deletion. This command requires strictly one argument:
 * ID or name of the reference.
 */
@Command(type = Command.Type.REQUEST, command = {"delete_reference"})
@Slf4j
public class ReferenceDeletionHandler extends AbstractHTTPCommandHandler {

    private Long referenceId;
    private boolean force;

    /**
     * Verifies that input arguments contain the required parameters:
     * first and the only argument must be ID or name of the reference file.
     * @param arguments command line arguments for 'delete_reference' command
     * @param options aren't used in this command
     */
    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException(MessageConstants.getMessage(ILLEGAL_COMMAND_ARGUMENTS,
                    getCommand(), 1, arguments.size()));
        }
        referenceId = loadReferenceId(arguments.get(0));
        force = options.isForceDeletion();
    }

    /**
     * Performs a reference deletion request to NGB server
     * @return 0 if request completed successfully
     */
    @Override public int runCommand() {
        if (force) {
            final List<BaseEntity> files = loadAllFiles(referenceId);
            if (CollectionUtils.isEmpty(files)) {
                final Reference reference = loadReferenceById(referenceId);
                final Long geneFileId = reference.getGeneFile() == null ? null : reference.getGeneFile().getId();
                final List<Long> annotationFileIds = reference.getAnnotationFiles() == null ?
                        Collections.emptyList() :
                        reference.getAnnotationFiles().stream()
                                .map(FeatureFile::getBioDataItemId)
                                .collect(Collectors.toList());
                removeGenes(referenceId);
                if (geneFileId != null) {
                    deleteGeneFile(geneFileId);
                }
                for (Long annotationId: annotationFileIds) {
                    removeAnnotation(referenceId, annotationId);
                    deleteItem(annotationId);
                }
                runDeletion(referenceId);
            } else {
                log.error(String.format("Can't delete reference with ID %d: used in files: %s", referenceId,
                        files.stream().map(BaseEntity::getName).collect(Collectors.joining(", "))));
            }
        } else {
            runDeletion(referenceId);
        }
        return 0;
    }
}
