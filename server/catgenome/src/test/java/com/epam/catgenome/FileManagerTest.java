package com.epam.catgenome;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.epam.catgenome.common.AbstractManagerTest;
import com.epam.catgenome.entity.file.FsDirectory;
import com.epam.catgenome.entity.file.AbstractFsItem;
import com.epam.catgenome.manager.FileManager;

/**
 * Test specific features, associated with FileManager only
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class FileManagerTest extends AbstractManagerTest {
    @Autowired
    private FileManager fileManager;

    @Autowired
    private ApplicationContext context;

    @Test
    public void testLoadDirectoryContents() throws IOException {
        Resource resource = context.getResource("classpath:templates");

        List<AbstractFsItem> items = fileManager.loadDirectoryContents(resource.getFile().getAbsolutePath());
        Assert.assertFalse(items.isEmpty());
        for (AbstractFsItem i : items) {
            if (i instanceof FsDirectory) {
                FsDirectory dir = (FsDirectory) i;
                List<AbstractFsItem> children = fileManager.loadDirectoryContents((dir.getPath()));
                Assert.assertEquals(dir.getFileCount().intValue(), children.size());
            }
        }

        Assert.assertTrue(items.stream()
                              .anyMatch(i -> i instanceof FsDirectory && ((FsDirectory) i).getFileCount() > 0));
    }

    @Test
    public void testLoadRoot() throws IOException {
        List<AbstractFsItem> items = fileManager.loadDirectoryContents(null);
        Assert.assertFalse(items.isEmpty());
    }
}
