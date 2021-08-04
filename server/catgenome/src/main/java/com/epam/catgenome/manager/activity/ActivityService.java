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

package com.epam.catgenome.manager.activity;

import com.epam.catgenome.dao.activity.ActivityDao;
import com.epam.catgenome.entity.activity.Activity;
import com.epam.catgenome.manager.AuthManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityService {

    protected final ActivityDao activityDao;
    protected final AuthManager authManager;

    public List<Activity> getByItemIdAndUid(final Long fileId, final String uid) {
        return ListUtils.emptyIfNull(activityDao.getByItemIdAndUid(fileId, uid));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Activity save(final Activity activity) {
        activity.setDatetime(LocalDateTime.now());
        activity.setUsername(authManager.getAuthorizedUser());
        activityDao.save(activity);
        return activity;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteByFileId(final Long itemId) {
        activityDao.deleteByItemId(itemId);
    }
}
