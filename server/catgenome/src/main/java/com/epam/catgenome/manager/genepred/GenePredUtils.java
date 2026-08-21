/*
 * MIT License
 *
 * Copyright (c) 2022 EPAM Systems
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

package com.epam.catgenome.manager.genepred;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class GenePredUtils {

    public static final String SOURCE = "GenePred";

    private static final Set<String> GENE_PRED_EXTENSION = new HashSet<>();

    static {
        GENE_PRED_EXTENSION.add(".genepred");
        GENE_PRED_EXTENSION.add(".gp");
    }

    private GenePredUtils() {
        //utility class
    }

    public static Set<String> getGenePredExtensions() {
        return GENE_PRED_EXTENSION;
    }

    // This used to call org.bouncycastle.util.Strings.toLowerCase, which was on the classpath only
    // because spring-security-saml2-core dragged Bouncy Castle in transitively; Phase 3 of the Java
    // 21 migration took that dependency out. Locale.ROOT is what the Bouncy Castle helper did - a
    // locale-independent lowercase - so a file called ".GP" still matches under a Turkish locale.
    public static boolean isGenePred(String path) {
        return GENE_PRED_EXTENSION.stream().anyMatch(path.toLowerCase(Locale.ROOT)::endsWith);
    }
}
