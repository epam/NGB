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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum BlastTaskStatus {
    CREATED(1, false),
    SUBMITTED(2, false),
    RUNNING(3, false),
    CANCELED(4, true),
    FAILED(5, true),
    DONE(6, true);

    private final int id;
    private final boolean finalStatus;
    private static final Map<Integer, BlastTaskStatus> ID_MAP = new HashMap<>(DONE.getId());

    static {
        ID_MAP.put(CREATED.id, CREATED);
        ID_MAP.put(SUBMITTED.id, SUBMITTED);
        ID_MAP.put(RUNNING.id, RUNNING);
        ID_MAP.put(CANCELED.id, CANCELED);
        ID_MAP.put(FAILED.id, FAILED);
        ID_MAP.put(DONE.id, DONE);
    }

    BlastTaskStatus(int id, boolean finalStatus) {
        this.id = id;
        this.finalStatus = finalStatus;
    }

    public int getId() {
        return id;
    }

    public boolean isFinal() {
        return finalStatus;
    }

    public static List<String> getNotFinalStatuses() {
        final List<String> statuses = new ArrayList<>();
        statuses.add(String.valueOf(BlastTaskStatus.CREATED.getId()));
        statuses.add(String.valueOf(BlastTaskStatus.SUBMITTED.getId()));
        statuses.add(String.valueOf(BlastTaskStatus.RUNNING.getId()));
        return statuses;
    }

    public static BlastTaskStatus getById(int id) {
        return ID_MAP.get(id);
    }
}
