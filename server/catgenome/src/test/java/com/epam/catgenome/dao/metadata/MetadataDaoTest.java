/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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

package com.epam.catgenome.dao.metadata;

import com.epam.catgenome.entity.metadata.EntityVO;
import com.epam.catgenome.entity.metadata.MetadataVO;
import com.epam.catgenome.entity.security.AclClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class MetadataDaoTest {

    private static final Long TEST_ENTITY_ID = 1L;
    private static final AclClass TEST_ENTITY_CLASS_1 = AclClass.PROJECT;
    private static final AclClass TEST_ENTITY_CLASS_2 = AclClass.GENE;
    private static final String TEST_KEY_1 = "key1";
    private static final String TEST_VALUE_1 = "value1";
    private static final String TEST_KEY_2 = "key2";
    private static final String TEST_VALUE_2 = "value2";

    @Autowired
    private MetadataDao metadataDao;

    @Test
    @Transactional
    public void shouldCRUDMetadata() {
        final MetadataVO metadata = MetadataVO.builder()
                .id(TEST_ENTITY_ID)
                .aclClass(TEST_ENTITY_CLASS_1)
                .metadata(Collections.singletonMap(TEST_KEY_1, TEST_VALUE_1))
                .build();
        metadataDao.save(metadata);

        final MetadataVO loadedMetadata = metadataDao.get(TEST_ENTITY_ID, TEST_ENTITY_CLASS_1.name());
        assertThat(loadedMetadata.getId()).isEqualTo(TEST_ENTITY_ID);
        assertThat(loadedMetadata.getAclClass()).isEqualTo(TEST_ENTITY_CLASS_1);
        assertThat(loadedMetadata.getMetadata())
                .hasSize(1)
                .containsKey(TEST_KEY_1)
                .containsValue(TEST_VALUE_1);

        final MetadataVO newMetadata = MetadataVO.builder()
                .id(TEST_ENTITY_ID)
                .aclClass(TEST_ENTITY_CLASS_1)
                .metadata(new HashMap<String, String>() {
                    {
                        put(TEST_KEY_1, TEST_VALUE_1);
                        put(TEST_KEY_2, TEST_VALUE_2);
                    }
                })
                .build();
        metadataDao.update(newMetadata);

        final MetadataVO loadedUpdatedMetadata = metadataDao.get(TEST_ENTITY_ID, TEST_ENTITY_CLASS_1.name());
        assertThat(loadedUpdatedMetadata.getId()).isEqualTo(TEST_ENTITY_ID);
        assertThat(loadedUpdatedMetadata.getAclClass()).isEqualTo(TEST_ENTITY_CLASS_1);
        assertThat(loadedUpdatedMetadata.getMetadata())
                .hasSize(2)
                .containsKeys(TEST_KEY_1, TEST_KEY_2)
                .containsValues(TEST_VALUE_1, TEST_VALUE_2);

        metadataDao.delete(TEST_ENTITY_ID, TEST_ENTITY_CLASS_1.name());
        assertThat(metadataDao.get(TEST_ENTITY_ID, TEST_ENTITY_CLASS_1.name())).isNull();
    }

    @Test
    @Transactional
    public void shouldGetSeveralItems() {
        final MetadataVO projectMetadata = MetadataVO.builder()
                .id(TEST_ENTITY_ID)
                .aclClass(TEST_ENTITY_CLASS_1)
                .metadata(Collections.singletonMap(TEST_KEY_1, TEST_VALUE_1))
                .build();
        metadataDao.save(projectMetadata);
        final MetadataVO itemMetadata = MetadataVO.builder()
                .id(TEST_ENTITY_ID)
                .aclClass(TEST_ENTITY_CLASS_2)
                .metadata(Collections.singletonMap(TEST_KEY_2, TEST_VALUE_2))
                .build();
        metadataDao.save(itemMetadata);

        final List<EntityVO> entities = Arrays.asList(
                EntityVO.builder().entityId(TEST_ENTITY_ID).entityClass(TEST_ENTITY_CLASS_1).build(),
                EntityVO.builder().entityId(TEST_ENTITY_ID).entityClass(TEST_ENTITY_CLASS_2).build());

        final List<MetadataVO> result = metadataDao.getItems(entities);
        assertThat(result).hasSize(2);
    }
}
