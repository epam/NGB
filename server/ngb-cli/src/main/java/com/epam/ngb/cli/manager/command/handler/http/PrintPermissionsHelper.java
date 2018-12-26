/*
 * MIT License
 *
 * Copyright (c) 2018 EPAM Systems
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

package com.epam.ngb.cli.manager.command.handler.http;

import com.epam.ngb.cli.entity.AclSecuredEntry;
import com.epam.ngb.cli.entity.PermissionsContainer;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.printer.AbstractResultPrinter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.epam.ngb.cli.manager.command.handler.http.GrantPermissionHandler.PERMISSION_MAP;

/**
 * Prepares entity permissions and prints them.
 */
@Setter
@Getter
@RequiredArgsConstructor
public class PrintPermissionsHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrintPermissionsHelper.class);

    private final AbstractHTTPCommandHandler handler;
    private final boolean printTable;

    /**
     * Loads entity permissions and prints them.
     * @param entityId entity ID
     * @param entityClass entity acl class
     */
    public void print(final Long entityId, final String entityClass) {
        try {
            AclSecuredEntry permissions = handler.loadPermissions(entityId, entityClass);
            List<AclSecuredEntry.AclPermissionEntry> rawPermissions = permissions.getPermissions();

            List<PermissionsContainer> permissionsContainer = preparePermissions(rawPermissions);
            if (CollectionUtils.isEmpty(permissionsContainer)) {
                return;
            }

            AbstractResultPrinter printer = AbstractResultPrinter
                    .getPrinter(printTable, permissionsContainer.get(0).getFormatString(permissionsContainer));
            printer.printHeader(permissionsContainer.get(0));
            permissionsContainer
                    .stream()
                    .sorted(getSidComparator())
                    .forEach(printer::printItem);
        } catch (IOException | ApplicationException e) {
            LOGGER.error("An error occurred during building permissions", e);
        }
    }

    private static List<PermissionsContainer> preparePermissions(List<AclSecuredEntry.AclPermissionEntry> permissions) {
        if (CollectionUtils.isEmpty(permissions)) {
            return Collections.emptyList();
        }
        return permissions.stream()
                .map(entry -> PermissionsContainer
                        .builder()
                        .isPrincipal(entry.getSid().isPrincipal())
                        .userName(entry.getSid().getName())
                        .prettyMask(convertToPrettyMask(entry.getMask()))
                        .build())
                .collect(Collectors.toList());
    }

    private static Comparator<PermissionsContainer> getSidComparator() {
        return (table1, table2) -> table1.isPrincipal() ? -1 : 1;
    }

    private static String convertToPrettyMask(final int mask) {
        StringBuilder permissions = new StringBuilder();
        PERMISSION_MAP.forEach((permissionName, permissionMask) -> {
            if ((mask & permissionMask) == permissionMask) {
                permissions.append(permissionName);
            }
        });
        return permissions.toString();
    }
}
