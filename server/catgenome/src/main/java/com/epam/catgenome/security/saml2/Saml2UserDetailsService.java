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

package com.epam.catgenome.security.saml2;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.security.NgbUser;
import com.epam.catgenome.entity.user.DefaultRoles;
import com.epam.catgenome.entity.user.Role;
import com.epam.catgenome.manager.user.RoleManager;
import com.epam.catgenome.manager.user.UserManager;
import com.epam.catgenome.security.UserContext;
import com.epam.catgenome.security.acl.GrantPermissionManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.stereotype.Service;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(value = "security.acl.enable", havingValue = "true")
public class Saml2UserDetailsService {

    private static final String ATTRIBUTES_DELIMITER = "=";
    public static final String LDAP_CN_FIELD = "CN";

    @Value("${saml.authorities.attribute.names: null}")
    private List<String> authorities;

    @Value(
            "#{catgenome['saml.user.attributes'] != null ? catgenome['saml.user.attributes'].split(',') : new String[0]}")
    private Set<String> samlAttributes;

    @Value("${saml.user.auto.create: EXPLICIT}")
    private Saml2UserRegisterStrategy autoCreateUsers;

    @Value("${security.default.admin:}")
    private String defaultAdmin;

    @Value("#{catgenome['saml.user.role.mapping'] != null ? catgenome['saml.user.role.mapping'].split(',') " +
            ": new String[0]}")
    private Set<String> samlRoleMappings;

    @Autowired
    private UserManager userManager;

    @Autowired
    private RoleManager roleManager;

    @Autowired
    private GrantPermissionManager permissionManager;

    public UserContext loadUserBySaml2(Saml2AuthenticatedPrincipal principal) throws UsernameNotFoundException {
        String userName = principal.getName().toUpperCase();
        List<String> groups = readAuthorities(principal);
        Map<String, Long> requiredGroupByRole = readSamlRolesMapping();
        Set<Long> requiredRoles = getRequiredRoleIds(groups, requiredGroupByRole);
        Map<String, String> attributes = readAttributes(principal);
        NgbUser loadedUser = userManager.loadUserByName(userName);

        log.debug("SAML user name: {}, groups: {}, attributes: {}", userName, groups, attributes);

        if (loadedUser == null) {
            log.debug(MessageHelper.getMessage(MessagesConstants.ERROR_USER_NAME_NOT_FOUND, userName));

            List<Long> roles = roleManager.getDefaultRolesIds();
            if (!userName.equalsIgnoreCase(defaultAdmin)) {
                checkAbilityToCreate(userName, groups);
            } else {
                roles.add(DefaultRoles.ROLE_ADMIN.getId());
            }

            addRequiredRoles(requiredRoles, roles);

            NgbUser createdUser = userManager.createUser(userName, roles, groups, attributes);
            log.debug("Created user {} with groups {}", userName, groups);

            UserContext userContext = new UserContext(userName);
            userContext.setUserId(createdUser.getId());
            userContext.setGroups(createdUser.getGroups());
            userContext.setRoles(createdUser.getRoles());
            return userContext;
        } else {
            log.debug("Found user by name {}", userName);
            loadedUser.setUserName(userName);

            List<Long> roles = buildUserRoles(groups, requiredGroupByRole, requiredRoles, loadedUser);

            boolean needToUpdateToAdmin = userName.equalsIgnoreCase(defaultAdmin)
                    && roles.stream().noneMatch(roleId -> DefaultRoles.ROLE_ADMIN.getId().equals(roleId));

            if (userManager.userUpdateRequired(groups, attributes, loadedUser) || needToUpdateToAdmin) {
                if (needToUpdateToAdmin) {
                    roles.add(DefaultRoles.ROLE_ADMIN.getId());
                }
                loadedUser = userManager.updateUserSAMLInfo(loadedUser.getId(), userName, roles, groups, attributes);
                log.debug("Updated user groups {} ", groups);
            }

            return new UserContext(loadedUser);
        }
    }

    private void checkAbilityToCreate(final String userName, final List<String> groups) {
        switch (autoCreateUsers) {
            case EXPLICIT:
                throw new UsernameNotFoundException(
                        MessageHelper.getMessage(MessagesConstants.ERROR_USER_NAME_NOT_FOUND, userName));
            case EXPLICIT_GROUP:
                if (!permissionManager.isGroupRegistered(groups)) {
                    throw new UsernameNotFoundException(
                            MessageHelper.getMessage(MessagesConstants.ERROR_NO_GROUP_WAS_FOUND, userName));
                }
                break;
            default:
                break;
        }
    }

    private List<String> readAuthorities(Saml2AuthenticatedPrincipal principal) {
        if (CollectionUtils.isEmpty(authorities)) {
            return Collections.emptyList();
        }
        List<String> grantedAuthorities = new ArrayList<>();
        authorities.forEach(auth -> {
            if (StringUtils.isEmpty(auth)) {
                return;
            }
            List<String> attributeValues = principal.getAttribute(auth);
            if (attributeValues != null && !attributeValues.isEmpty()) {
                String[] valuesArray = attributeValues.toArray(new String[0]);
                valuesArray = getParsedLdapGroupName(valuesArray.clone());
                grantedAuthorities.addAll(
                        Arrays.stream(valuesArray)
                                .filter(StringUtils::isNotBlank)
                                .map(String::toUpperCase)
                                .collect(Collectors.toList()));
            }
        });
        return grantedAuthorities;
    }

    private String[] getParsedLdapGroupName(String[] attributeValues) {
        for (int i = 0; i < attributeValues.length; i++) {
            try {
                if (StringUtils.isBlank(attributeValues[i])) {
                    continue;
                }

                LdapName ldapName = new LdapName(attributeValues[i]);
                for (Rdn rdn : ldapName.getRdns()) {
                    if (rdn.getType().equalsIgnoreCase(LDAP_CN_FIELD)) {
                        attributeValues[i] = rdn.getValue().toString();
                    }
                }
            } catch (InvalidNameException e) {
                log.info("SAML attribute is not LDAP name, will leave as original value.");
            }
        }
        return attributeValues;
    }

    private Map<String, String> readAttributes(Saml2AuthenticatedPrincipal principal) {
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
                    log.error("Can not parse saml user attributes property.");
                    continue;
                }
                String attributeValues = principal.getFirstAttribute(value);
                if (StringUtils.isNotEmpty(attributeValues)) {
                    parsedAttributes.put(key, attributeValues);
                }
            }
        }
        return parsedAttributes;
    }

    private Map<String, Long> readSamlRolesMapping() {
        final Map<String, Long> roles = new HashMap<>();
        for (String attribute : samlRoleMappings) {
            if (!attribute.contains(ATTRIBUTES_DELIMITER)) {
                continue;
            }

            final String[] splittedRecord = attribute.split(ATTRIBUTES_DELIMITER);
            final String key = StringUtils.upperCase(splittedRecord[0]);
            final String value = StringUtils.upperCase(splittedRecord[1]);
            if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
                log.error("Can not parse saml roles mappings property.");
                continue;
            }

            final Optional<Role> role = roleManager.findRoleByName(value);
            if (role.isPresent()) {
                roles.putIfAbsent(key, role.get().getId());
                continue;
            }
            log.warn("Requested role '{}' doesn't exist.", value);
        }
        return roles;
    }

    private void addRequiredRoles(final Set<Long> requiredRoles, final List<Long> roles) {
        requiredRoles.stream()
                .filter(roleId -> !roles.contains(roleId))
                .forEach(roles::add);
    }

    private List<Long> buildUserRoles(final List<String> groups, final Map<String, Long> requiredGroupByRole,
                                      final Set<Long> requiredRoles, final NgbUser loadedUser) {
        final Set<Long> roleIdsToDelete = ListUtils.emptyIfNull(loadedUser.getGroups()).stream()
                .map(StringUtils::upperCase)
                .filter(group -> !groups.contains(group))
                .filter(requiredGroupByRole::containsKey)
                .map(requiredGroupByRole::get)
                .collect(Collectors.toSet());

        final List<Long> roles = ListUtils.emptyIfNull(loadedUser.getRoles()).stream()
                .map(Role::getId)
                .filter(roleId -> !roleIdsToDelete.contains(roleId))
                .collect(Collectors.toList());

        addRequiredRoles(requiredRoles, roles);
        return roles;
    }

    private Set<Long> getRequiredRoleIds(final List<String> groups, final Map<String, Long> requiredGroupByRole) {
        return groups.stream()
                .map(StringUtils::upperCase)
                .filter(requiredGroupByRole::containsKey)
                .map(requiredGroupByRole::get)
                .collect(Collectors.toSet());
    }
}
