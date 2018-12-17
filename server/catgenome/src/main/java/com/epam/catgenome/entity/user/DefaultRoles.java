/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2018 EPAM Systems
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.epam.catgenome.entity.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Provides access to predefined roles, created during deploy
 */
@Getter
@AllArgsConstructor
public enum DefaultRoles {

    ROLE_ADMIN(new Role(1L, "ROLE_ADMIN", true, false)),
    ROLE_USER(new Role(2L, "ROLE_USER", true, true)),
    ROLE_REFERENCE_MANAGER(new Role(3L, "ROLE_REFERENCE_MANAGER", true, false)),
    ROLE_BAM_MANAGER(new Role(4L, "ROLE_BAM_MANAGER", true, false)),
    ROLE_VCF_MANAGER(new Role(5L, "ROLE_VCF_MANAGER", true, false)),
    ROLE_GENE_MANAGER(new Role(6L, "ROLE_GENE_MANAGER", true, false)),
    ROLE_BED_MANAGER(new Role(7L, "ROLE_BED_MANAGER", true, false)),
    ROLE_WIG_MANAGER(new Role(8L, "ROLE_WIG_MANAGER", true, false)),
    ROLE_SEG_MANAGER(new Role(9L, "ROLE_SEG_MANAGER", true, false));
    private Role role;

    public Long getId() {
        return role.getId();
    }

    public String getName() {
        return role.getName();
    }
}
