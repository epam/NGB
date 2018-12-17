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

package com.epam.catgenome.manager.user;

import java.util.ArrayList;
import java.util.List;

import com.epam.catgenome.controller.vo.NgbUserVO;
import com.epam.catgenome.entity.security.JwtRawToken;
import com.epam.catgenome.manager.AuthManager;
import com.epam.catgenome.security.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.epam.catgenome.entity.security.NgbUser;

import static com.epam.catgenome.security.acl.SecurityExpressions.*;

@Service
@ConditionalOnProperty(value = "security.acl.enable", havingValue = "true")
public class UserSecurityService {

    private UserManager userManager;

    private AuthManager authManager;


    @Autowired
    public UserSecurityService(UserManager userManager, AuthManager authManager) {
        this.userManager = userManager;
        this.authManager = authManager;
    }

    @PreAuthorize(ROLE_ADMIN)
    public List<NgbUser> loadAllUsers() {
        return userManager.loadAllUsers();
    }
    @PreAuthorize(ROLE_ADMIN)
    public NgbUser loadUserByName(String name) {
        return userManager.loadUserByName(name);
    }


    /**
     * Registers a new user
     * @param userVO specifies user to create
     * @return created user
     */
    @PreAuthorize(ROLE_ADMIN)
    public NgbUser createUser(NgbUserVO userVO) {
        return userManager.createUser(userVO);
    }

    /**
     * Updates existing user, currently only defaultStorageId is supported for update
     * @param id
     * @param userVO
     * @return
     */
    @PreAuthorize(ROLE_ADMIN)
    public NgbUser updateUser(final Long id, final NgbUserVO userVO) {
        return userManager.updateUserSAMLInfo(id, userVO.getUserName(), userVO.getRoleIds(), null, null);
    }

    @PreAuthorize(ROLE_ADMIN)
    public NgbUser loadUser(Long id) {
        return userManager.loadUserById(id);
    }

    @PreAuthorize(ROLE_ADMIN)
    public void deleteUser(Long id) {
        userManager.deleteUser(id);
    }

    @PreAuthorize(ROLE_ADMIN)
    public NgbUser updateUserRoles(Long id, List<Long> roles) {
        return userManager.updateUser(id, roles);
    }

    @PreAuthorize(ROLE_ADMIN)
    public List<NgbUser> loadUsers() {
        return new ArrayList<>(userManager.loadAllUsers());
    }

    @PreAuthorize(ROLE_USER)
    public JwtRawToken issueTokenForCurrentUser(Long expiration) {
        return authManager.issueTokenForCurrentUser(expiration);
    }

    @PreAuthorize(ROLE_USER)
    public UserContext getUserContext() {
        return authManager.getUserContext();
    }
}
