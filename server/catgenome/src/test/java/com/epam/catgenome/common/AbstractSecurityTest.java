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

package com.epam.catgenome.common;

import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.epam.catgenome.app.AclSecurityConfiguration;
import com.epam.catgenome.app.JWTSecurityConfiguration;
import com.epam.catgenome.app.SAMLSecurityConfiguration;

/**
 * Of the three imported configurations only {@link JWTSecurityConfiguration} actually contributes
 * beans: {@code test-catgenome-auth.properties} sets {@code jwt.security.enable=true} and sets
 * neither {@code saml.security.enable} nor {@code security.acl.enable}, so the
 * {@code @ConditionalOnProperty} on the other two excludes them. They stay on the import list
 * because that is what the class has always declared, and because it documents which configurations
 * a subclass is allowed to switch on with its own {@code @TestPropertySource}. In particular, do not
 * switch SAML on without also providing a key store and identity provider metadata -
 * {@link SAMLSecurityConfiguration} reads both eagerly while building the relying party registration.
 */
@Import({JWTSecurityConfiguration.class, SAMLSecurityConfiguration.class, AclSecurityConfiguration.class})
@TestPropertySource(locations = "classpath:test-catgenome-auth.properties")
@ContextConfiguration({"classpath:applicationContext-test.xml", "classpath:catgenome-servlet-test.xml"})
@EnableWebSecurity
@ImportResource("classpath:conf/catgenome/acl-test-dao.xml")
public abstract class AbstractSecurityTest {
}
