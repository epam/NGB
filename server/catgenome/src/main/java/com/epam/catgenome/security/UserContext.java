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

package com.epam.catgenome.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.epam.catgenome.entity.user.Role;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.epam.catgenome.entity.security.JwtRawToken;
import com.epam.catgenome.entity.security.JwtTokenClaims;
import com.epam.catgenome.entity.security.NgbUser;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Class represents information about user
 */
@Getter
@Setter
@NoArgsConstructor
public class UserContext implements UserDetails {
    private List<String> groups = new ArrayList<>();
    private List<Role> roles = new ArrayList<>();
    private Map<String, String> attributes;
    private JwtRawToken jwtRawToken;
    private Long userId;
    private String userName;
    private String orgUnitId;

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
}
