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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.epam.catgenome.dao.AbstractDaoTest;
import com.epam.catgenome.entity.security.NgbUser;
import com.epam.catgenome.security.DefaultRoles;
import com.epam.catgenome.security.Role;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class RoleDaoTest extends AbstractDaoTest
{
    private static final int EXPECTED_DEFAULT_ROLES_NUMBER = 10;
    private static final String TEST_USER1 = "test_user1";
    private static final String TEST_ROLE = "ROLE_TEST";
    private static final String TEST_ROLE_UPDATED = "NEW_ROLE";
    private static final String TEST_STORAGE_PATH = "test";

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private UserDao userDao;

    private NgbUser user;

    @Before
    public void setUp() throws Exception {
        user = new NgbUser("test");
        userDao.createUser(user, Arrays.asList(DefaultRoles.ROLE_ADMIN.getId(), DefaultRoles.ROLE_USER.getId(),
                                               DefaultRoles.ROLE_VCF_MANAGER.getId()));
    }

    @Test
    public void testLoadRolesWithUsers() {
        roleDao.createRole(TEST_ROLE);
        Collection<Role> roles = roleDao.loadAllRoles();
        assertEquals(EXPECTED_DEFAULT_ROLES_NUMBER + 1, roles.size());
        assertTrue(roles.stream().anyMatch(role -> role.getName().equals(TEST_ROLE)));
    }

    @Test
    public void testRoleCRUD() {
        Role testRole = roleDao.createRole(TEST_ROLE);
        assertNotNull(testRole);
        assertNotNull(testRole.getId());
        assertEquals(TEST_ROLE, testRole.getName());

        Role loadedRole = roleDao.loadRole(testRole.getId()).get();
        assertEquals(testRole.getName(), loadedRole.getName());
        assertEquals(testRole.getId(), loadedRole.getId());
        assertEquals(testRole.isUserDefault(), loadedRole.isUserDefault());

        Collection<Role> allRoles = roleDao.loadAllRoles();
        assertFalse(allRoles.isEmpty());
        assertTrue(isRolePresent(testRole, allRoles));

        List<Role> rolesByList = roleDao.loadRoles(Arrays.asList(testRole.getId(), DefaultRoles.ROLE_USER.getId()));
        assertEquals(2, rolesByList.size());
        assertTrue(isRolePresent(testRole, rolesByList));
        assertTrue(isRolePresent(DefaultRoles.ROLE_USER.getRole(), rolesByList));

        testRole.setName(TEST_ROLE_UPDATED);
        testRole.setUserDefault(true);
        roleDao.updateRole(testRole);

        loadedRole = roleDao.loadRoleByName(testRole.getName()).get();
        assertEquals(testRole.getName(), loadedRole.getName());
        assertEquals(testRole.getId(), loadedRole.getId());
        assertEquals(testRole.isUserDefault(), loadedRole.isUserDefault());

        userDao.updateUserRoles(user, Collections.singletonList(testRole.getId()));

        roleDao.deleteRoleReferences(testRole.getId());
        roleDao.deleteRole(testRole.getId());
        assertTrue(!roleDao.loadRole(testRole.getId()).isPresent());
        assertTrue(roleDao.loadAllRoles().stream().noneMatch(r -> r.equals(testRole)));

    }

    @Test
    public void testLoadRolesByUserIds() {
        Map<Long, List<Role>> roles = roleDao.loadRolesByUserIds(Collections.singletonList(user.getId()));
        Assert.assertEquals(3, roles.get(user.getId()).size());
    }

    private boolean isRolePresent(Role roleToFind, Collection<Role> roles) {
        return roles.stream().anyMatch(r -> r.equals(roleToFind));
    }
}