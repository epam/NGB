package com.epam.catgenome.util.azure;

import org.junit.Assert;
import org.junit.Test;

public class AzureCredentialConfigurationTest {

    @Test
    public void testByAccessKey() {
        Assert.assertNotNull(AzureCredentialConfiguration.byAccessKey());
    }

    @Test
    public void testByServicePrincipal() {
        Assert.assertNotNull(AzureCredentialConfiguration.byServicePrincipal());
    }

    @Test
    public void testByDefault() {
        Assert.assertNotNull(AzureCredentialConfiguration.byDefault());
    }
}
