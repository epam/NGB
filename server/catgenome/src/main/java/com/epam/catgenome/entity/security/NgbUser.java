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

package com.epam.catgenome.entity.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.epam.catgenome.entity.user.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class NgbUser {
    public static final String EMAIL_ATTRIBUTE = "email";

    private Long id;
    private String userName;
    private List<Role> roles;
    private List<String> groups;
    private boolean admin;

    private Map<String, String> attributes;

    public NgbUser() {
        this.admin = false;
        this.roles = new ArrayList<>();
        this.groups = new ArrayList<>();
        this.attributes = new HashMap<>();
    }

    public NgbUser(String userName) {
        this();
        this.userName = userName;
    }

    public String getEmail() {
        if (attributes == null) {
            return null;
        }
        return attributes.entrySet()
            .stream()
            .filter(entry -> entry.getKey().equalsIgnoreCase(EMAIL_ATTRIBUTE))
            .map(Map.Entry::getValue).findFirst().orElse(null);
    }

    @JsonIgnore
    public Set<String> getAuthorities() {
        Set<String> authorities = new HashSet<>();
        authorities.add(userName);
        authorities.addAll(roles.stream().map(Role::getName).collect(Collectors.toList()));
        authorities.addAll(groups);
        return authorities;
    }
}
