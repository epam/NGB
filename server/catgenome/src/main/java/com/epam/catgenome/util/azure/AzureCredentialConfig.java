package com.epam.catgenome.util.azure;

public interface AzureCredentialConfig {

    String getStorageAccount();

    static AccessKeyCredentialConfigurer.AccessKeyCredentialConfigurerBuilder byAccessKey() {
        return AccessKeyCredentialConfigurer.builder();
    }

    static ServicePrincipalCredentialConfigurer.ServicePrincipalCredentialConfigurerBuilder byServicePrincipal() {
        return ServicePrincipalCredentialConfigurer.builder();
    }

    static DefaultCredentialConfigurer.DefaultCredentialConfigurerBuilder byDefault() {
        return DefaultCredentialConfigurer.builder();
    }
}
