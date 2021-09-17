/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2021 EPAM Systems
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.epam.ngb.cli.manager.command.handler.http;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.Project;
import com.epam.ngb.cli.entity.ProjectNote;
import com.epam.ngb.cli.manager.command.handler.Command;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

import static com.epam.ngb.cli.constants.MessageConstants.ERROR_PROJECT_NOTE_NOT_FOUND;
import static com.epam.ngb.cli.constants.MessageConstants.ERROR_PROJECT_NOT_FOUND;
import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

@Command(type = Command.Type.REQUEST, command = {"remove_note"})
public class DatasetNoteDeleteHandler extends AbstractHTTPCommandHandler {

    private Project project;
    private long noteId;

    /**
      * Verifies input arguments
      * @param arguments command line arguments for 'remove_note' command
      * @param options
      */
    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.size() != 2) {
            throw new IllegalArgumentException(MessageConstants.getMessage(
                    ILLEGAL_COMMAND_ARGUMENTS, getCommand(), 2, arguments.size()));
        }
        project = loadProjectByName(arguments.get(0));
        if (project == null) {
            throw new IllegalArgumentException(MessageConstants.getMessage(
                    ERROR_PROJECT_NOT_FOUND, arguments.get(0)));
        }
        noteId = Long.parseLong(arguments.get(1));
    }

    @Override public int runCommand() {
        List<ProjectNote> projectNotes = project.getNotes();
        if (CollectionUtils.isEmpty(projectNotes)) {
            throw new IllegalArgumentException(MessageConstants.getMessage(
                    ERROR_PROJECT_NOTE_NOT_FOUND, noteId));
        }
        ProjectNote projectNote = projectNotes.stream()
                .filter(p -> p.getNoteId().equals(noteId))
                .findFirst()
                .orElse(null);
        if (projectNote == null) {
            throw new IllegalArgumentException(MessageConstants.getMessage(
                    ERROR_PROJECT_NOTE_NOT_FOUND, noteId));
        }
        projectNotes.removeIf(p -> p.getNoteId().equals(noteId));
        project.setNotes(projectNotes);
        saveProject(project);
        return 0;
    }
}
