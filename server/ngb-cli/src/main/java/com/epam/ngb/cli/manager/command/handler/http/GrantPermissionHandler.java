/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2018 EPAM Systems
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.epam.ngb.cli.manager.command.handler.http;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.AclSecuredEntry;
import com.epam.ngb.cli.entity.BiologicalDataItem;
import com.epam.ngb.cli.entity.PermissionGrantRequest;
import com.epam.ngb.cli.entity.Project;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.request.RequestManager;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.epam.ngb.cli.constants.MessageConstants.ERROR_WRONG_PERMISSION;
import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;
import static com.epam.ngb.cli.entity.PermissionGrantRequest.*;

/**
 */
@Command(type = Command.Type.REQUEST, command = {"chmod"})
public class GrantPermissionHandler extends AbstractHTTPCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrantPermissionHandler.class);

    static final Map<String, Integer> PERMISSION_MAP = new HashMap<>();
    private static final String DELETE_PERMISSION_URL = "/restapi/grant?id=%d&aclClass=%s&user=%s&isPrincipal=%b";
    private static final String GET_PERMISSION_URL = "/restapi/grant?id=%d&aclClass=%s";
    private static final String DELETE_TYPE = "DELETE";
    private static final String DELETE_PERMISSION_ACTION = "!";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final int READ_NO_READ_BITS = 0x3;
    private static final int WRITE_NO_WRITE_BITS = 0xc;

    static {
        PERMISSION_MAP.put("r+", 1);
        PERMISSION_MAP.put("r-", 2);
        PERMISSION_MAP.put("w+", 4);
        PERMISSION_MAP.put("w-", 8);
    }

    private static final Pattern PERMISSION_PATTERN = Pattern.compile("^(r[+\\-]?)|(w[+\\-]?)$");
    private static final Pattern ACTION_PATTERN = Pattern.compile("\\+|\\-|!");


    private List<String> users = Collections.emptyList();
    private List<String> groups = Collections.emptyList();
    private List<BiologicalDataItem> files = Collections.emptyList();
    private List<Project> datasets = Collections.emptyList();
    private String action;
    private String permission;

    /**
     * Verifies input arguments
     * @param arguments command line arguments for 'grant_permission' command
     * @param options
     */
    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException(MessageConstants.getMessage(
                    ILLEGAL_COMMAND_ARGUMENTS, getCommand(), 1, arguments.size()));
        }

        String permissionAndAction = arguments.get(0);
        action = String.valueOf(permissionAndAction.charAt(permissionAndAction.length() - 1));
        permission = permissionAndAction.substring(0, permissionAndAction.length() - 1);

        if(!PERMISSION_PATTERN.matcher(permission).find() && !ACTION_PATTERN.matcher(action).find()) {
            throw new IllegalArgumentException(MessageConstants.getMessage(
                    ERROR_WRONG_PERMISSION));
        }

        if (options.getUsers() != null) {
            users = Arrays.asList(options.getUsers().split(","));
        }

        if (options.getGroups() != null) {
            groups = Arrays.stream(options.getGroups().split(","))
                    .map(g -> !g.startsWith(ROLE_PREFIX) ? ROLE_PREFIX + g : g)
                    .collect(Collectors.toList());
        }

        if (options.getFiles() != null) {
            files = Arrays.stream(options.getFiles().split(","))
                    .map(f -> loadSilentlyOrWarn(() -> loadFileByNameOrBioID(f)))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        if (options.getDatasets() != null) {
            datasets = Arrays.stream(options.getDatasets().split(","))
                    .map(d -> loadSilentlyOrWarn(() -> loadProjectByName(d)))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

    }

    @Override public int runCommand() {
        int mask = getPermissionMask();
        files.forEach(item -> {
            AclClass aclClass = AclClass.valueOf(item.getFormat().name());
            users.forEach(user -> grantOrDeletePermission(
                    mask, aclClass, item.getId(), item.getClass(), user, true));
            groups.forEach(group -> grantOrDeletePermission(
                    mask, aclClass, item.getId(), item.getClass(), group, false));
        });
        datasets.forEach(dataset -> {
            AclClass aclClass = AclClass.PROJECT;
            users.forEach(user -> grantOrDeletePermission(
                    mask, aclClass, dataset.getId(), dataset.getClass(), user, true));
            groups.forEach(group -> grantOrDeletePermission(
                    mask, aclClass, dataset.getId(), dataset.getClass(), group, false));
        });
        return 0;
    }

    private void grantOrDeletePermission(int mask, AclClass aclClass, Long id, Class<?> classType,
                                         String identifier, boolean isPrincipal) {
        String result;

        if (action.equals(DELETE_PERMISSION_ACTION)) {
            HttpDelete deletePermission = (HttpDelete) getRequestFromURLByType(
                    DELETE_TYPE, serverParameters.getServerUrl() +
                            String.format(DELETE_PERMISSION_URL, id, aclClass, identifier, isPrincipal));
            result = RequestManager.executeRequest(deletePermission);
        } else {
            String existingPermissions = RequestManager.executeRequest(
                    getRequestFromURLByType(
                            "GET",
                            serverParameters.getServerUrl() + String.format(GET_PERMISSION_URL, id, aclClass)));
            AclSecuredEntry entityPermissions = getResult(existingPermissions, AclSecuredEntry.class);
            for (AclSecuredEntry.AclPermissionEntry entry : entityPermissions.getPermissions()) {
                if (entry.getSid().getName().equalsIgnoreCase(identifier)) {
                    Integer previousMask = entry.getMask();
                    mask = mergeMasks(previousMask, mask);
                }
            }
            PermissionGrantRequest registration = new PermissionGrantRequest(
                    isPrincipal, identifier, mask, aclClass, id);
            HttpRequestBase request = getRequest(getRequestUrl());
            result = getPostResult(registration, (HttpPost) request);
        }

        try {
            checkAndPrintResult(result, false, false, classType);
        } catch (ApplicationException e) {
            LOGGER.info(e.getMessage());
        }
    }

    private int mergeMasks(Integer previousMask, int mask) {
        return mergeMaskForPermission(previousMask, mask, READ_NO_READ_BITS)
                | mergeMaskForPermission(previousMask, mask, WRITE_NO_WRITE_BITS);
    }

    private int mergeMaskForPermission(Integer previousMask, int mask, int permissionTemplate) {
        int result;
        if ((mask & permissionTemplate) != 0) {
            result = mask & permissionTemplate;
        } else {
            // select only permission bits
            result = previousMask & permissionTemplate;
        }
        return result;
    }

    private <T> T loadSilentlyOrWarn(Supplier<T> loadFunction) {
        try {
            return loadFunction.get();
        } catch (ApplicationException e) {
            LOGGER.info(e.getMessage() + " We will skip it!");
            return null;
        }
    }

    private int getPermissionMask() {
        int finalMask = 0;
        Matcher matcher = PERMISSION_PATTERN.matcher(permission);
        while (matcher.find()) {
            Integer partialMask = mapPermissionToMask(matcher.group(), action);
            if (partialMask != null) {
                finalMask = finalMask | partialMask;
            }
        }
        return finalMask;
    }

    private Integer mapPermissionToMask(String perm, String defaultAction) {
        if (perm.length() == 1) {
            perm = perm + defaultAction;
        }
        return PERMISSION_MAP.get(perm);
    }

}
