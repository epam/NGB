/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2018-2021 EPAM Systems
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

package com.epam.catgenome.security.acl;

public final class SecurityExpressions {

    public static final String OR = " OR ";
    public static final String AND = " AND ";
    public static final String ROLE_USER = "hasRole('USER')";
    public static final String ROLE_ADMIN = "hasRole('ADMIN')";
    public static final String ROLE_HEATMAP_MANAGER = "hasRole('ROLE_HEATMAP_MANAGER')";
    public static final String ROLE_PROJECT_MANAGER = "hasRole('PROJECT_MANAGER')";
    public static final String ROLE_SEG_MANAGER = "hasRole('SEG_MANAGER')";
    public static final String ROLE_VCF_MANAGER = "hasRole('VCF_MANAGER')";
    public static final String ROLE_BAM_MANAGER = "hasRole('BAM_MANAGER')";
    public static final String ROLE_BED_MANAGER = "hasRole('BED_MANAGER')";
    public static final String ROLE_GENE_MANAGER = "hasRole('GENE_MANAGER')";
    public static final String ROLE_WIG_MANAGER = "hasRole('WIG_MANAGER')";
    public static final String ROLE_REFERENCE_MANAGER = "hasRole('REFERENCE_MANAGER')";
    public static final String READ_PROJECT_BY_ID = "hasPermissionOnProject(#projectId, 'READ')";
    public static final String READ_ON_FILTER_OBJECT = "isAllowed(filterObject, 'READ')";

    private SecurityExpressions() {

    }
}
