package com.epam.catgenome.security.saml;

import org.apache.commons.lang3.StringUtils;
import org.opensaml.common.binding.decoding.BasicURLComparator;
import org.opensaml.common.binding.decoding.URIComparator;

import java.net.MalformedURLException;
import java.net.URL;

public class SchemeInsensitiveUrlComparator implements URIComparator {

    private final URIComparator uriComparator = new BasicURLComparator();

    @Override
    public boolean compare(final String uri1, final String uri2) {
        if (StringUtils.isBlank(uri1) || StringUtils.isBlank(uri2)) {
            return false;
        }
        String url2WithMatchingProtocol = switchProtocolIfRequired(uri1, uri2);
        return uriComparator.compare(uri1, url2WithMatchingProtocol);
    }

    private String switchProtocolIfRequired(final String url1, final String url2) {
        try {
            URL first = new URL(url1);
            URL second = new URL(url2);
            String targetProtocol = first.getProtocol();
            if (targetProtocol.equals(second.getProtocol())) {
                return url2;
            }
            return url2.replace(second.getProtocol(), targetProtocol);
        } catch (MalformedURLException e) {
            return url2;
        }
    }
}
