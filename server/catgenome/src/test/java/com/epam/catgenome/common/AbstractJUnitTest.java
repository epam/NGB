/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
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

import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import com.epam.catgenome.controller.vo.registration.ReferenceRegistrationRequest;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.helper.FileTemplates;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.reference.ReferenceManager;
import com.google.common.io.Files;

/**
 * Source:      AbstractJUnitTest.java
 * Created:     11/18/15, 4:20 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code AbstractJUnitTest} represents a test template, provides required procedures
 * of initialization to setup each test that should be run in a transactional context.
 * <p>
 * Also it provides miscellaneous util methods for JUnit tests, e.g. it includes method
 * to retrieve test resources associated with files saved under the given classpath in
 * ./templates directory.
 *
 * @author Denis Medvedev
 */
public abstract class AbstractJUnitTest {

    protected static final String TEMPLATES_CLASSPATH = "classpath:templates/";
    protected static final String FMT_TEMP_TEMPLATE_PATH = "%s" + File.separator + "%s";
    protected static final String CLASSPATH_TEMPLATES_FELIS_CATUS_VCF = "classpath:templates/Felis_catus.vcf";
    protected static final String CLASSPATH_TEMPLATES_GENES_SORTED = "classpath:templates/genes_sorted.gtf";

    protected static final String CHR_A1 = "chrA1";
    protected static final String CHR_A5 = "chrA5";
    protected static final String HP_GENOME = "Harry Potter v1.0";

    @Autowired
    protected FileManager fileManager;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ReferenceManager referenceManager;

    protected ApplicationContext ctx() {
        return context;
    }

    protected File getTemplate(final String name) throws IOException {
        if (trimToNull(name) == null) {
            return null;
        }
        // gets a reference on an original template file
        final File source = ctx().getResource(TEMPLATES_CLASSPATH + name).getFile();
        // copies it to a temporary directory, because managers can delete assuming that
        // it's a temporary resource created during file upload operation

        if (!fileManager.getTempDir().exists()) {
            fileManager.getTempDir().mkdirs();
        }

        final File destination = new File(String.format(FMT_TEMP_TEMPLATE_PATH,
                                                        fileManager.getTempDir().getAbsolutePath(), source.getName()));

        Files.copy(source, destination);
        return destination;
    }

    protected Reference createGenome() throws IOException {
        // prepares data to register a test genome in the system
        final File content = getTemplate(FileTemplates.HP_GENOME.getPath());
        getTemplate(FileTemplates.HP_GENOME.getPath() + ".fai");

        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setName(HP_GENOME);
        request.setPath(content.getPath());
        request.setNoGCContent(false);

        // register a test genome in the system
        final Reference reference = referenceManager.registerGenome(request);
        // cleans up 'path' parameters, because they never should be sent to the client, and it
        // breaks ReflectionAssert.assertReflectionEquals() when you compare a chromosome from this
        // reference and another one received from a response
        reference.getChromosomes().stream().forEach(e -> e.setPath(null));
        return reference;
    }

    //returns a copy a test resource file for unregister testing
    protected File getResourceFileCopy(String resourcePath) throws IOException {
        Resource resource = context.getResource(resourcePath);
        final File tmp = new File(fileManager.getTempDir(), resource.getFilename());
        FileUtils.copyFile(resource.getFile(), tmp);
        FileUtils.forceDeleteOnExit(tmp);
        return tmp;
    }

}
