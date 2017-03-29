package com.epam.catgenome;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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

    @Test
    public void testGetDefaultTrackSettings() throws IOException {
        Map<String, Map<String, Object>> tracksSettings = fileManager.getDefaultTrackSettings();

        final String wig = "wig";
        final String area = "area";

        final String id = "id";
        final String name = "name";
        final String ref = "ref";

        final String minimum = "minimum";
        final String color = "color";
        final String height = "height";

        final int expectedFileConfigs = 2;
        Assert.assertTrue(tracksSettings.size() == expectedFileConfigs);

        Map<String, Object> wigConfig = tracksSettings.get("wig");
        Assert.assertNotNull(wigConfig);
        Map areaConfigMap = (Map) wigConfig.get(area);
        Assert.assertNotNull(areaConfigMap);
        Map wigConfigMap = (Map) wigConfig.get(wig);
        Assert.assertNotNull(wigConfigMap);

        final int expectedMinimum = 0;
        Assert.assertTrue(areaConfigMap.containsKey(minimum) && areaConfigMap.get(minimum).equals(expectedMinimum));
        final String expectedColor = "#92AEE7";
        Assert.assertTrue(wigConfigMap.containsKey(color) && wigConfigMap.get(color).equals(expectedColor));
        final int expectedHeight = 50;
        Assert.assertTrue(wigConfig.containsKey(height) && wigConfig.get(height).equals(expectedHeight));

        Map<String, Object> bamConfig = tracksSettings.get("bam");
        Assert.assertNotNull(bamConfig);
        final int expectedId = 1;
        Assert.assertTrue(bamConfig.containsKey(id) && bamConfig.get(id).equals(expectedId));
        final String expectedName = "bam-track";
        Assert.assertTrue(bamConfig.containsKey(name) && bamConfig.get(name).equals(expectedName));
        final String expectedRef = "hg19";
        Assert.assertTrue(bamConfig.containsKey(ref) && bamConfig.get(ref).equals(expectedRef));

        // check that the invalid json config file is not in the config map
        Map<String, Object> invalidConfig = tracksSettings.get("invalid");
        Assert.assertNull(invalidConfig);
    }
}
