/*
 * MIT License
 *
 * Copyright (c) 2025 EPAM Systems
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

package com.epam.catgenome.manager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.controller.vo.registration.ReferenceRegistrationRequest;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.util.NgbFileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
@Slf4j
public class UrlValidatorService {

    private final List<String> allowedHosts;

    public UrlValidatorService(@Value("#{'${item.path.register.allowed.hosts}'.split(',')}")
                               final List<String> allowedHosts) {
        this.allowedHosts = allowedHosts;
    }

    public void validate(final IndexedFileRegistrationRequest request) {
        validatePath(request.getPath(), request.getType());
        final BiologicalDataItemResourceType indexType = Optional.ofNullable(request.getIndexType())
                .orElse(request.getType());
        validatePath(request.getIndexPath(), indexType);
    }

    public void validateReference(final ReferenceRegistrationRequest request) {
        validatePath(request.getPath(), request.getType());
    }

    private void validatePath(final String inputPath, final BiologicalDataItemResourceType type) {
        if (StringUtils.isBlank(inputPath)) {
            log.debug("Input path is empty. Skipping validation.");
            return;
        }
        if (!validationRequired(inputPath, type)) {
            log.debug("Validation not required.");
            return;
        }
        validateUrl(inputPath);
    }

    private void validateUrl(final String inputUrl) {
        try {
            final String host = new URI(inputUrl).getHost();
            log.debug("Validating host '{}'", host);
            ListUtils.emptyIfNull(allowedHosts).stream()
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .filter(domain -> matchesAllowedDomain(domain, host))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            MessageHelper.getMessage(MessagesConstants.ERROR_HOST_NOT_ALLOWED)));
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to parse URL.", e);
        }
    }

    private boolean matchesAllowedDomain(final String allowedDomain, final String targetHost) {
        return FilenameUtils.wildcardMatch(targetHost, allowedDomain);
    }

    private boolean validationRequired(final String inputPath, final BiologicalDataItemResourceType type) {
        return BiologicalDataItemResourceType.URL.equals(type)
                || BiologicalDataItemResourceType.S3.equals(type)
                || BiologicalDataItemResourceType.AZ.equals(type)
                || NgbFileUtils.isRemotePath(inputPath)
                || inputPath.startsWith("s3://") || inputPath.startsWith("sws://")
                || inputPath.startsWith("az://");
    }
}
