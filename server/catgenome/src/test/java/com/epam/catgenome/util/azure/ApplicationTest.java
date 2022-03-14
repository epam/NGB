package com.epam.catgenome.util.azure;

import com.epam.catgenome.app.Application;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

public class ApplicationTest {

    static Field CREDENTIAL_CONFIG_FIELD;

    @BeforeClass
    public static void prepareClass() {
        CREDENTIAL_CONFIG_FIELD = ReflectionUtils.findField(AzureBlobClient.class,"credentialConfig", AzureCredentialConfiguration.class);
        Assert.assertNotNull(CREDENTIAL_CONFIG_FIELD);
        ReflectionUtils.makeAccessible(CREDENTIAL_CONFIG_FIELD);
    }

    @Test
    public void testAzureBlobClientDummyClient() {
        Application app = new Application();

        AzureBlobClient client = app.azureBlobClient("", "", "", "", "", "");
        Assert.assertNotNull(client);
        Assert.assertNull(ReflectionUtils.getField(CREDENTIAL_CONFIG_FIELD, client));
    }

    @Test
    public void testAzureBlobClientAccessKeyClient() {
        Application app = new Application();

        AzureBlobClient client = app.azureBlobClient("storageAccount", "storageKey", "managedIdentity", "tenantId", "clientId", "clientSecret");
        Assert.assertNotNull(client);
        Object credentialConfig = ReflectionUtils.getField(CREDENTIAL_CONFIG_FIELD, client);
        Assert.assertNotNull(credentialConfig);
        Assert.assertEquals(AccessKeyCredentialConfigurer.class, credentialConfig.getClass());
    }

    @Test
    public void testAzureBlobClientServicePrincipalClient() {
        Application app = new Application();

        AzureBlobClient client = app.azureBlobClient("storageAccount", "", "", "tenantId", "clientId", "clientSecret");
        Assert.assertNotNull(client);
        Object credentialConfig = ReflectionUtils.getField(CREDENTIAL_CONFIG_FIELD, client);
        Assert.assertNotNull(credentialConfig);
        Assert.assertEquals(ServicePrincipalCredentialConfigurer.class, credentialConfig.getClass());
    }

    @Test
    public void testAzureBlobDefaultCredentialClient() {
        Application app = new Application();

        AzureBlobClient client = app.azureBlobClient("storageAccount", "", "managedIdentity", "tenantId", "", "");
        Assert.assertNotNull(client);
        Object credentialConfig = ReflectionUtils.getField(CREDENTIAL_CONFIG_FIELD, client);
        Assert.assertNotNull(credentialConfig);
        Assert.assertEquals(DefaultCredentialConfigurer.class, credentialConfig.getClass());
    }
}
