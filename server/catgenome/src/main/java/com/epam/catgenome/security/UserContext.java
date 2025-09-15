package com.epam.catgenome.security;

import com.epam.catgenome.entity.security.JwtRawToken;
import com.epam.catgenome.entity.security.JwtTokenClaims;
import com.epam.catgenome.entity.security.NgbUser;
import com.epam.catgenome.entity.user.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class represents information about user
 * Now implements Saml2AuthenticatedPrincipal for SAML 2.0 integration
 */
@Getter
@Setter
@NoArgsConstructor
public class UserContext implements UserDetails, Saml2AuthenticatedPrincipal {
    private List<String> groups = new ArrayList<>();
    private List<Role> roles = new ArrayList<>();
    private Map<String, String> attributes;
    private JwtRawToken jwtRawToken;
    private Long userId;
    private String userName;
    private String orgUnitId;

    // SAML 2.0 specific fields
    private Map<String, List<Object>> saml2Attributes;
    private String registrationId;
    private String idpEntityId;

    public UserContext(JwtRawToken jwtRawToken, JwtTokenClaims claims) {
        this.jwtRawToken = jwtRawToken;
        this.userId = claims.getUserId();
        this.userName = claims.getUserName();
        this.orgUnitId = claims.getOrgUnitId();
        this.groups = ListUtils.emptyIfNull(claims.getGroups());
        this.roles = ListUtils.emptyIfNull(claims.getRoles()).stream().map(Role::new).collect(Collectors.toList());
    }

    public UserContext(String userName) {
        this.userName = userName;
    }

    public UserContext(NgbUser user) {
        this.userName = user.getUserName();
        this.userId = user.getId();
        this.roles = user.getRoles();
        this.groups = user.getGroups();
    }

    // Constructor for SAML 2.0 authentication
    public UserContext(Saml2AuthenticatedPrincipal principal, String registrationId, String idpEntityId) {
        this.userName = principal.getName();
        this.saml2Attributes = principal.getAttributes();
        this.registrationId = registrationId;
        this.idpEntityId = idpEntityId;
    }

    public JwtTokenClaims toClaims() {
        return JwtTokenClaims.builder()
                .userId(userId)
                .userName(userName)
                .orgUnitId(orgUnitId)
                .roles(ListUtils.emptyIfNull(roles).stream().map(Role::getName).collect(Collectors.toList()))
                .groups(groups)
                .build();
    }

    @Override
    public List<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> result = new ArrayList<>();

        if (!CollectionUtils.isEmpty(roles)) {
            result = roles.stream()
                    .map(role -> new SimpleGrantedAuthority(role.getName()))
                    .collect(Collectors.toList());
        }

        if (!CollectionUtils.isEmpty(groups)) {
            result.addAll(groups.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));
        }

        return result;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return false;
    }

    @Override
    public boolean isAccountNonLocked() {
        return false;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // Saml2AuthenticatedPrincipal implementation
    @Override
    public String getName() {
        return userName;
    }

    @Override
    public Map<String, List<Object>> getAttributes() {
        return saml2Attributes;
    }

    // Override the getAttribute method to match Saml2AuthenticatedPrincipal interface
    @Override
    public List<Object> getAttribute(String name) {
        return saml2Attributes != null ? saml2Attributes.get(name) : null;
    }

    // Helper method to get a single attribute value as String
    public String getFirstAttribute(String name) {
        if (saml2Attributes != null && saml2Attributes.containsKey(name)) {
            List<Object> values = saml2Attributes.get(name);
            return values != null && !values.isEmpty() ? values.get(0).toString() : null;
        }
        return null;
    }

    // Helper method to get all values of an attribute
    public List<Object> getAttributeValues(String name) {
        return saml2Attributes != null ? saml2Attributes.get(name) : null;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public String getIdpEntityId() {
        return idpEntityId;
    }
}
