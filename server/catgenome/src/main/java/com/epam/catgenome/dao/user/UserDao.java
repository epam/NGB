/*
 * MIT License
 *
 * Copyright (c) 2018 EPAM Systems
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

package com.epam.catgenome.dao.user;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.epam.catgenome.entity.security.NgbUser;

public class UserDao {

    public NgbUser loadUserByName(String userName) {
        return null;
    }

    public NgbUser createUser(NgbUser user, List<Long> userRoles) {
        return null;
    }

    public Collection<NgbUser> loadAllUsers() {
        return null;
    }

    public List<NgbUser> loadUsersByNames(Collection<String> names) {
        if (names.isEmpty()) {
            return Collections.emptyList();
        }

        return null;
    }

    public NgbUser loadUserById(Long id) {
        return null;
    }

    public void deleteUserRoles(Long id) {

    }

    public void deleteUser(Long id) {

    }

    public void insertUserRoles(Long id, List<Long> roles) {

    }

    public void updateUser(NgbUser user) {

    }

    public List<NgbUser> findUsers(String prefix) {
        return null;
    }

    public List<String> loadAllGroups() {
        return null;
    }

    public List<String> findGroups(String prefix) {
        return null;
    }

    public Collection<NgbUser> loadUsersByGroup(String group) {
        return null;
    }

    public boolean isUserInGroup(String userName, String group) {
        return false;
    }
}
