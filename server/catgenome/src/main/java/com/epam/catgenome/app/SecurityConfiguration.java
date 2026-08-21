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

package com.epam.catgenome.app;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * The two authenticating configurations that used to be imported here, JWTSecurityConfiguration and
 * SAMLSecurityConfiguration, are out of the build for Phase 3 of the Java 21 migration: they are
 * built on spring-security-saml2-core / OpenSAML 2, which cannot work with Spring Security 6 at all.
 * Phase 4 rewrites them against spring-security-saml2-service-provider and puts them back. Until
 * then the only web-security configuration is the anonymous one, so AUTH_MODE=none is the only mode
 * that works; ACL authorization is untouched and still switched by security.acl.enable.
 */
@Configuration
@Import({NoSecurityConfiguration.class, AclSecurityConfiguration.class})
public class SecurityConfiguration {
}
