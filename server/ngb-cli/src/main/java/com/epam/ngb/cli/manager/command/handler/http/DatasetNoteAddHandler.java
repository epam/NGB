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
import org.apache.http.client.methods.HttpPost;

import java.util.ArrayList;
import java.util.List;

import static com.epam.ngb.cli.constants.MessageConstants.ERROR_PROJECT_NOT_FOUND;
import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

@Command(type = Command.Type.REQUEST, command = {"add_note"})
public class DatasetNoteAddHandler extends AbstractHTTPCommandHandler {

    private Project project;

    /**
      * Verifies input arguments
      * @param arguments command line arguments for 'add_note' command
      * @param options
      */
    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.size() != 3) {
            throw new IllegalArgumentException(MessageConstants.getMessage(
                    ILLEGAL_COMMAND_ARGUMENTS, getCommand(), 3, arguments.size()));
        }
        project = loadProjectByName(arguments.get(0));
        if (project == null) {
            throw new IllegalArgumentException(MessageConstants.getMessage(
                    ERROR_PROJECT_NOT_FOUND, arguments.get(0)));
        }
        String title = arguments.get(1);
        String content = arguments.get(2);
        List<ProjectNote> projectNotes = project.getNotes() == null ? new ArrayList<>() : project.getNotes();

        ProjectNote projectNote = ProjectNote.builder()
                .projectId(project.getId())
                .title(title)
                .content(content)
                .build();
        projectNotes.add(projectNote);
        project.setNotes(projectNotes);
    }

    @Override public int runCommand() {
        HttpPost request = (HttpPost) getRequestFromURLByType("POST",
                getServerParameters().getServerUrl() + getServerParameters().getProjectSaveUrl());
        getPostResult(project, request);
        return 0;
    }
}
