package com.epam.catgenome.common;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.springframework.beans.factory.annotation.Autowired;

import com.epam.catgenome.manager.FileManager;

/**
 * Source:      AbstractManagerTest
 * Created:     24.01.17, 12:15
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 15.0.3, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
public abstract class AbstractManagerTest {
    @Autowired
    private FileManager fileManager;

    @After
    public void tearDown() throws IOException {
        System.gc();
        FileUtils.deleteDirectory(new File(fileManager.getBaseDirPath()));
    }
}
