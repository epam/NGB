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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.epam.catgenome.dao.AbstractDaoTest;
import com.epam.catgenome.entity.security.NgbUser;
import com.epam.catgenome.entity.user.DefaultRoles;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class UserDaoTest extends AbstractDaoTest {
    private static final String TEST_USER1 = "test_user1";
    private static final String TEST_USER2 = "test_user2";
    private static final String TEST_USER3 = "test_user3";
    private static final List<String> TEST_GROUPS_1 = new ArrayList<>();
    private static final List<String> TEST_GROUPS_2 = new ArrayList<>();
    private static final String TEST_GROUP_1 = "test_group_1";
    private static final String TEST_GROUP_2 = "test_group_2";
    private static final String ATTRIBUTES_KEY = "email";
    private static final String ATTRIBUTES_VALUE = "test_email";
    private static final String ATTRIBUTES_VALUE2 = "Mail@epam.com";

    @Autowired
    private UserDao userDao;

    @Test
    public void testSearchUserByPrefix() {
        NgbUser user = new NgbUser();
        user.setUserName(TEST_USER1);
        user.getAttributes().put(ATTRIBUTES_KEY, ATTRIBUTES_VALUE2);
        NgbUser savedUser = userDao.createUser(user, Collections.emptyList());

        Collection<NgbUser> userByNamePrefix = userDao.findUsers(TEST_USER1.substring(0, 2));
        Assert.assertEquals(1, userByNamePrefix.size());
        Assert.assertTrue(userByNamePrefix.stream().anyMatch(u -> u.getId().equals(savedUser.getId())));
    }

    @Test
    public void testUserCRUD() {
        NgbUser user = new NgbUser();
        user.setUserName(TEST_USER1);
        NgbUser savedUser = userDao.createUser(user, Arrays.asList(DefaultRoles.ROLE_ADMIN.getId(),
                                                                   DefaultRoles.ROLE_USER.getId()));
        Assert.assertNotNull(savedUser);
        Assert.assertNotNull(savedUser.getId());

        Collection<NgbUser> users = userDao.loadAllUsers();
        Assert.assertFalse(users.isEmpty());
        Assert.assertTrue(users.stream().anyMatch(u -> u.getId().equals(savedUser.getId())));

        NgbUser userById = userDao.loadUserById(savedUser.getId());
        Assert.assertEquals(savedUser.getId(), userById.getId());

        NgbUser userByName = userDao.loadUserByName(TEST_USER1.toUpperCase());
        Assert.assertEquals(savedUser.getId(), userByName.getId());

        savedUser.setUserName(TEST_USER2);
        userDao.updateUser(savedUser);
        NgbUser userByChangedName = userDao.loadUserByName(TEST_USER2);
        Assert.assertEquals(savedUser.getId(), userByChangedName.getId());

        Collection<NgbUser> loadedUsers = userDao.loadUsersByNames(Collections.singletonList(TEST_USER2));
        Assert.assertFalse(loadedUsers.isEmpty());

        userDao.updateUserRoles(savedUser, Collections.singletonList(DefaultRoles.ROLE_USER.getId()));
        NgbUser userUpdatedRoles = userDao.loadUserByName(TEST_USER2);
        Assert.assertEquals(1, userUpdatedRoles.getRoles().size());
        Assert.assertEquals(DefaultRoles.ROLE_USER.name(),  userUpdatedRoles.getRoles().get(0).getName());

        userDao.deleteUserRoles(savedUser.getId());
        userDao.deleteUser(savedUser.getId());

        Assert.assertNull(userDao.loadUserById(savedUser.getId()));
        Collection<NgbUser> usersAfterDeletion = userDao.loadAllUsers();
        Assert.assertTrue(usersAfterDeletion.stream().noneMatch(u -> u.getId().equals(savedUser.getId())));
    }

    @Test
    public void testUserGroups() {
        NgbUser user1 = new NgbUser();
        user1.setUserName(TEST_USER1);
        TEST_GROUPS_1.add(TEST_GROUP_1);
        user1.setGroups(TEST_GROUPS_1);
        NgbUser savedUser = userDao.createUser(user1, Collections.singletonList(DefaultRoles.ROLE_USER.getId()));
        Assert.assertNotNull(savedUser);

        NgbUser user2 = new NgbUser();
        user2.setUserName(TEST_USER2);
        TEST_GROUPS_1.add(TEST_GROUP_2);
        user2.setGroups(TEST_GROUPS_1);
        NgbUser savedUser2 = userDao.createUser(user2, Collections.singletonList(DefaultRoles.ROLE_USER.getId()));
        Assert.assertNotNull(savedUser2);

        NgbUser user3 = new NgbUser();
        user3.setUserName(TEST_USER3);
        TEST_GROUPS_2.add(TEST_GROUP_2);
        user3.setGroups(TEST_GROUPS_2);
        NgbUser savedUser3 = userDao.createUser(user3, Arrays.asList(DefaultRoles.ROLE_ADMIN.getId(),
                                                                     DefaultRoles.ROLE_USER.getId()));
        Assert.assertNotNull(savedUser3);

        Collection<NgbUser> userByGroup = userDao.loadUsersByGroup(TEST_GROUP_1);
        Assert.assertEquals(2, userByGroup.size());
        Assert.assertTrue(userByGroup.stream().anyMatch(u ->
                                                            u.getUserName().equals(TEST_USER1)));
        Assert.assertTrue(userByGroup.stream().noneMatch(u ->
                                                             u.getUserName().equals(TEST_USER3)));

        Map<Long, List<String>> groupMap = userDao.loadGroups(Arrays.asList(user1.getId(), user2.getId(),
                                                                            user3.getId()));

        Assert.assertEquals(1, groupMap.get(user1.getId()).size());
        Assert.assertEquals(2, groupMap.get(user2.getId()).size());
        Assert.assertEquals(1, groupMap.get(user3.getId()).size());

        List<String> foundGroups = userDao.findGroups("TEST_");
        Assert.assertEquals(TEST_GROUPS_1.size(), foundGroups.size());
        Assert.assertTrue(TEST_GROUPS_1.containsAll(foundGroups));

        Assert.assertFalse(userDao.isUserInGroup(user1.getUserName(), "TEST_GROUP_5"));
        Assert.assertTrue(userDao.isUserInGroup(user1.getUserName(), TEST_GROUP_1));

        List<String> allLoadedGroups = userDao.loadAllGroups();
        Collections.sort(allLoadedGroups);
        Assert.assertEquals(TEST_GROUPS_1, allLoadedGroups);

        userDao.deleteUserGroups(user1.getId());
        userDao.deleteUserRoles(user1.getId());
        userDao.deleteUser(user1.getId());
        Assert.assertNull(userDao.loadUserById(user1.getId()));
    }

    @Test
    public void testUserCRUDWithAttributes() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(ATTRIBUTES_KEY, ATTRIBUTES_VALUE);
        NgbUser user = new NgbUser();
        user.setUserName(TEST_USER1);
        user.setAttributes(attributes);
        NgbUser savedUser = userDao.createUser(user,
                Arrays.asList(DefaultRoles.ROLE_ADMIN.getId(), DefaultRoles.ROLE_USER.getId()));
        Assert.assertNotNull(savedUser);
        Assert.assertNotNull(savedUser.getId());
        Assert.assertNotNull(savedUser.getAttributes());

        Collection<NgbUser> users = userDao.loadAllUsers();
        Assert.assertFalse(users.isEmpty());
        Assert.assertTrue(users.stream()
                              .anyMatch(u -> u.getId().equals(savedUser.getId())
                                             && assertUserAttributes(attributes, u.getAttributes())));

        NgbUser userById = userDao.loadUserById(savedUser.getId());
        Assert.assertEquals(savedUser.getId(), userById.getId());
        Assert.assertTrue(assertUserAttributes(attributes, userById.getAttributes()));

        NgbUser userByName = userDao.loadUserByName(TEST_USER1.toUpperCase());
        Assert.assertEquals(savedUser.getId(), userByName.getId());
        Assert.assertTrue(assertUserAttributes(attributes, userByName.getAttributes()));

        savedUser.setUserName(TEST_USER2);
        userDao.updateUser(savedUser);
        NgbUser userByChangedName = userDao.loadUserByName(TEST_USER2);
        Assert.assertEquals(savedUser.getId(), userByChangedName.getId());
        Assert.assertTrue(assertUserAttributes(attributes, userByChangedName.getAttributes()));

        userDao.deleteUserRoles(savedUser.getId());
        userDao.deleteUser(savedUser.getId());

        Assert.assertNull(userDao.loadUserById(savedUser.getId()));
        Collection<NgbUser> usersAfterDeletion = userDao.loadAllUsers();
        Assert.assertTrue(usersAfterDeletion.stream().noneMatch(u -> u.getId().equals(savedUser.getId())));
    }

    private boolean assertUserAttributes(Map<String, String> expectedAttributes, Map<String, String> actualAttributes) {
        return CollectionUtils.isEqualCollection(expectedAttributes.entrySet(), actualAttributes.entrySet());
    }
}