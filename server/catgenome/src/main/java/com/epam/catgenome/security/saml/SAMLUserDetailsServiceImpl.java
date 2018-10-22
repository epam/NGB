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

package com.epam.catgenome.security.saml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;
import org.springframework.stereotype.Service;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.security.NgbUser;
import com.epam.catgenome.manager.user.RoleManager;
import com.epam.catgenome.manager.user.UserManager;
import com.epam.catgenome.entity.user.DefaultRoles;
import com.epam.catgenome.entity.user.Role;
import com.epam.catgenome.security.UserContext;

@Service
public class SAMLUserDetailsServiceImpl implements SAMLUserDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SAMLUserDetailsServiceImpl.class);
    private static final String ATTRIBUTES_DELIMITER = "=";

    @Value("${saml.authorities.attribute.names: null}")
    private List<String> authorities;

    @Value(
        "#{catgenome['saml.user.attributes'] != null ? catgenome['saml.user.attributes'].split(',') : new String[0]}")
    private Set<String> samlAttributes;

    @Value("${saml.user.auto.create: false}")
    private boolean autoCreateUsers;

    @Value("${security.default.admin:}")
    private String defaultAdmin;

    @Autowired
    private UserManager userManager;

    @Autowired
    private RoleManager roleManager;

    @Override
    public Object loadUserBySAML(SAMLCredential credential) throws UsernameNotFoundException {
        String userName = credential.getNameID().getValue().toUpperCase();
        List<String> groups = readAuthorities(credential);
        Map<String, String> attributes = readAttributes(credential);
        NgbUser loadedUser = userManager.loadUserByName(userName);

        if (loadedUser == null) {
            if (!autoCreateUsers) {
                throw new UsernameNotFoundException(
                    MessageHelper.getMessage(MessagesConstants.ERROR_USER_NAME_NOT_FOUND, userName));
            }
            LOGGER.debug(MessageHelper.getMessage(MessagesConstants.ERROR_USER_NAME_NOT_FOUND, userName));


            List<Long> roles = roleManager.getDefaultRolesIds();
            if (defaultAdmin.equalsIgnoreCase(userName)) {
                roles.add(DefaultRoles.ROLE_ADMIN.getId());
            }

            NgbUser createdUser = userManager.createUser(userName, roles, groups, attributes);
            LOGGER.debug("Created user {} with groups {}", userName, groups);

            UserContext userContext = new UserContext(userName);
            userContext.setUserId(createdUser.getId());
            userContext.setGroups(createdUser.getGroups());
            userContext.setRoles(createdUser.getRoles());
            return userContext;
        } else {
            LOGGER.debug("Found user by name {}", userName);
            loadedUser.setUserName(userName);
            List<Long> roles = loadedUser.getRoles().stream().map(Role::getId).collect(Collectors.toList());
            boolean shouldAddAdmin = !roles.contains(DefaultRoles.ROLE_ADMIN.getId()) &&
                                     defaultAdmin.equalsIgnoreCase(userName);
            if (shouldAddAdmin) {
                roles.add(DefaultRoles.ROLE_ADMIN.getId());
            }

            if (userManager.userUpdateRequired(groups, attributes, loadedUser) || shouldAddAdmin) {
                loadedUser = userManager.updateUserSAMLInfo(loadedUser.getId(), userName, roles, groups, attributes);
                LOGGER.debug("Updated user groups {} ", groups);
            }

            return new UserContext(loadedUser);
        }
    }

    private List<String> readAuthorities(SAMLCredential credential) {
        if (CollectionUtils.isEmpty(authorities)) {
            return Collections.emptyList();
        }
        List<String> grantedAuthorities = new ArrayList<>();
        authorities.stream().forEach(auth -> {
            if (StringUtils.isEmpty(auth)) {
                return;
            }
            String[] attributeValues = credential.getAttributeAsStringArray(auth);
            if (attributeValues != null && attributeValues.length > 0) {
                grantedAuthorities.addAll(
                    Arrays.stream(attributeValues)
                        .map(String::toUpperCase)
                        .collect(Collectors.toList()));
            }
        });
        return grantedAuthorities;
    }

    private Map<String, String> readAttributes(SAMLCredential credential) {
        if (CollectionUtils.isEmpty(samlAttributes)) {
            return Collections.emptyMap();
        }
        Map<String, String> parsedAttributes = new HashMap<>();
        for (String attribute : samlAttributes) {
            if (attribute.contains(ATTRIBUTES_DELIMITER)) {
                String[] splittedRecord = attribute.split(ATTRIBUTES_DELIMITER);
                String key = splittedRecord[0];
                String value = splittedRecord[1];
                if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
                    LOGGER.error("Can not parse saml user attributes property.");
                    continue;
                }
                String attributeValues = credential.getAttributeAsString(value);
                if (StringUtils.isNotEmpty(attributeValues)) {
                    parsedAttributes.put(key, attributeValues);
                }
            }
        }
        return parsedAttributes;
    }
}
