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

package com.epam.ngb.cli.manager.command.handler.http.reference;

import com.epam.ngb.cli.manager.command.handler.Command;

/**
 * {@code {@link AnnotationReferenceAddingHandler }} represents a tool for handling 'add_ann' command and
 * adding a annotation files to a reference, registered on NGB server. This command requires strictly two arguments:
 * - reference ID or name
 * - annotation file IDs, names or paths to files (only for registration as a new file)
 */
@Command(type = Command.Type.REQUEST, command = {"add_annotation"})
public class AnnotationReferenceAddingHandler extends AbstractAnnotationReferenceHandler {

    private static final boolean REMOVING_FLAG = false;

    @Override
    protected boolean isRemoving() {
        return REMOVING_FLAG;
    }
}
