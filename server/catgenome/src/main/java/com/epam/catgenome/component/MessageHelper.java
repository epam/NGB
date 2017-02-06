/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
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

package com.epam.catgenome.component;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.util.Assert;

/**
 * Source:      MessageHelper.java
 * Created:     10/2/15, 3:57 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code MessageHelper} represents utility, which allows resolve and format messages.
 * <p>
 * Represents the component managed by Spring to attach {@code MessageSource} instance which was registered
 * in application context by Spring.
 * <p>
 * It can be useful to resolve messages codes in any classes, which are out of scope application context, e.g.
 * any exceptions classes, plain objects etc.
 * <p>
 * Provides methods to resolve messages codes and format messages by passed arguments. To do it, internally
 * uses {@code MessageSource} methods which are suppressed any exceptions when no message with the given code
 * can be found in the source. By default it'll use code which was passed.
 *
 * @see java.text.MessageFormat
 * @see org.springframework.context.support.AbstractMessageSource
 */
public final class MessageHelper {

    /**
     * Represents a reference on the {@code MessageHelper} singleton.
     */
    private static volatile MessageHelper instance;

    /**
     * Represents a reference on {@code MessageSource} instance, which is associated with this helper and should
     * be used to lookup messages by given codes. Injected to this helper component from web application context.
     */
    private MessageSource messageSource;

    /**
     * Creates a new {@code MessageHelper} associated with the provided {@code MessageSource}.
     *
     * @param messageSource {@code MessageSource} represents a reference on resources bundle that should
     *                      be associated with this helper
     */
    public MessageHelper(final MessageSource messageSource) {
        this.messageSource = messageSource;
        instance = this;
    }

    /**
     * Returns a reference on the underlying {@code MessageSource} that is used by this helper to resolve codes
     * of messages.
     *
     * @return {@code MessageSource}
     */
    public MessageSource getMessageSource() {
        return messageSource;
    }

    @Autowired
    public static void setInstance(MessageHelper helper) {
        MessageHelper.instance = helper;
    }

    /**
     * Tries to resolve the message. Returns the resolved and formatted message or the given message code, when
     * no appropriate message was found in available resources.
     *
     * @param code {@code String} represents the code to look up
     * @param args represents a reference on array of {@code Object} or varargs; both provide arguments that will
     *             be filled in for params within message, or <tt>null</tt> if no arguments; args look like "{0}",
     *             "{1, date}", "{2, time}" within message (also see e.g. Spring documentation for more details)
     * @return {@code String} represents the resolved message if the lookup was successful; otherwise the given
     * keycode
     * @see java.text.MessageFormat
     * @see org.springframework.context.support.AbstractMessageSource
     */
    public static String getMessage(final String code, final Object... args) {
        return instance.getMessageSource().getMessage(code, args, code, Locale.getDefault());
    }

    /**
     * Tries to resolve the message. Returns the resolved and formatted message or the given message code, when
     * no appropriate message was found in available resources.
     *
     * @param code {@code MessageCode} represents the code to look up
     * @param args represents a reference on array of {@code Object} or varargs; both provide arguments that will
     *             be filled in for params within message, or <tt>null</tt> if no arguments; args look like "{0}",
     *             "{1, date}", "{2, time}" within message (also see e.g. Spring documentation for more details)
     * @return {@code String} represents the resolved message if the lookup was successful; otherwise the given
     * keycode
     * @see java.text.MessageFormat
     * @see org.springframework.context.support.AbstractMessageSource
     */
    public static String getMessage(final MessageCode code, final Object... args) {
        return getMessage(code.getCode(), args);
    }

    /**
     * Represents a factory method to instantiate {@code MessageHelper} singleton following by
     * "Double Checked Locking & Volatile" pattern.
     *
     * @param messageSource {@code MessageSource} used as the underlying messages bundle
     * @return {@code MessageHelper}
     */
    public static MessageHelper singleton(final MessageSource messageSource) {
        MessageHelper helper = instance;
        if (helper == null) {
            synchronized (MessageHelper.class) {
                helper = instance;
                if (helper == null) {
                    Assert.notNull(messageSource);
                    instance = new MessageHelper(messageSource);
                    helper = instance;
                }
            }
        }
        return helper;
    }
}
