/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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

package com.epam.catgenome.entity.blast;

import java.util.HashMap;
import java.util.Map;


public enum TaskStatus {
    CREATED(1, false),
    SUBMITTED(2, false),
    RUNNING(3, false),
    CANCELED(4, true),
    FAILED(5, true),
    DONE(6, true);

    private long id;
    private boolean finalStatus;
    private static Map<Long, TaskStatus> idMap = new HashMap<>((int) CREATED.getId());

    static {
        idMap.put(CREATED.id, CREATED);
        idMap.put(SUBMITTED.id, SUBMITTED);
        idMap.put(RUNNING.id, RUNNING);
        idMap.put(CANCELED.id, CANCELED);
        idMap.put(FAILED.id, FAILED);
        idMap.put(DONE.id, DONE);
    }

    TaskStatus(long id, boolean finalStatus) {
        this.id = id;
        this.finalStatus = finalStatus;
    }

    public long getId() {
        return id;
    }

    public boolean isFinal() {
        return finalStatus;
    }

    public static TaskStatus getById(Long id) {
        if (id == null) {
            return null;
        }

        return idMap.get(id);
    }
}
