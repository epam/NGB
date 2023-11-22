/*
 * MIT License
 *
 * Copyright (c) 2023 EPAM Systems
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
package com.epam.catgenome.manager.externaldb.ncbi;

import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.join;

@Service
@Slf4j
@RequiredArgsConstructor
public class NCBISequencesManager {
    public static final String EMPTY_LINE = "\n\n";
    private static final Integer FETCH_SEQUENCES_BATCH_SIZE = 50;
    private final NCBIDataManager ncbiDataManager;

    public String getProteinsFasta(final List<String> proteinIds) throws ExternalDbUnavailableException {
        final StringBuilder result = new StringBuilder();
        final List<List<String>> subSets = Lists.partition(proteinIds, FETCH_SEQUENCES_BATCH_SIZE);
        String proteins;
        for (List<String> subIds : subSets) {
            proteins = ncbiDataManager.fetchTextById("protein", join(subIds, ","), "fasta");
            result.append(proteins);
        }
        return result.toString();
    }

    public static String extractSequenceId(final String fasta) {
        return fasta.split(" ")[0].replace(">", "").toUpperCase();
    }

    public static Map<String, String> getFastaMap(final String fasta) {
        final Map<String, String> res = new HashMap<>();
        for (String line : fasta.split(EMPTY_LINE)) {
            res.put(extractSequenceId(line), line);
        }
        return res;
    }
}
