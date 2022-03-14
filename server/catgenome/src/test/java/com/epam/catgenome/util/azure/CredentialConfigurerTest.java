package com.epam.catgenome.util.azure;

import org.junit.Assert;
import org.junit.Test;

public class CredentialConfigurerTest {

    @Test
    public void testAccessKeyConfiguration() {

        String storageKey = "storageKey";
        String storageAccount = "storageAccount";
        AccessKeyCredentialConfigurer c = AzureCredentialConfiguration.byAccessKey().storageKey(storageKey)
                .storageAccount(storageAccount).build();
        Assert.assertNotNull(c);
        Assert.assertEquals(storageKey, c.getStorageKey());
        Assert.assertEquals(storageAccount, c.getStorageAccount());
    }

    @Test
    public void testServicePrincipalAccessKeyConfiguration() {
        String storageAccount = "storageAccount";
        String tenantId = "tenantId";
        String clientId = "clientId";
        String clientSecret = "clientSecret";
        ServicePrincipalCredentialConfigurer c = AzureCredentialConfiguration.byServicePrincipal()
                .storageAccount(storageAccount).tenantId(tenantId).clientId(clientId).clientSecret(clientSecret)
                .build();
        Assert.assertNotNull(c);
        Assert.assertEquals(storageAccount, c.getStorageAccount());
        Assert.assertEquals(tenantId, c.getTenantId());
        Assert.assertEquals(clientId, c.getClientId());
        Assert.assertEquals(clientSecret, c.getClientSecret());
    }

    @Test
    public void testtBuilderAzureCredentialConfiguration() {
        String storageAccount = "storageAccount";
        String tenantId = "tenantId";
        String managedIdentityId = "managedIdentityId";
        DefaultCredentialConfigurer c = AzureCredentialConfiguration.byDefault()
                .storageAccount(storageAccount).tenantId(tenantId).managedIdentityId(managedIdentityId)
                .build();
        Assert.assertNotNull(c);
        Assert.assertEquals(storageAccount, c.getStorageAccount());
        Assert.assertEquals(tenantId, c.getTenantId());
        Assert.assertEquals(managedIdentityId, c.getManagedIdentityId());
    }
}
