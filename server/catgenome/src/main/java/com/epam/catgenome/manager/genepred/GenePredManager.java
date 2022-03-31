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

import com.epam.catgenome.component.MessageCode;
import com.epam.catgenome.manager.genbank.GeneType;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import com.epam.catgenome.manager.gene.writer.Gff3FeatureImpl;
import com.epam.catgenome.manager.gene.writer.Gff3Writer;
import com.epam.catgenome.util.feature.reader.TribbleIndexedFeatureReader;
import lombok.SneakyThrows;
import org.broad.igv.feature.BasicFeature;
import org.broad.igv.feature.tribble.UCSCGeneTableCodec;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.manager.gene.GeneUtils.ID_ATTR;
import static com.epam.catgenome.manager.gene.GeneUtils.NAME_ATTR;
import static com.epam.catgenome.manager.genepred.GenePredUtils.SOURCE;
import static com.epam.catgenome.util.Utils.DOT;

@Service
public class GenePredManager {

    @SneakyThrows
    public void genePredToGTF(final String genePredFile, final Path gffFilePath) {
        Assert.notNull(genePredFile, getMessage(MessageCode.RESOURCE_NOT_FOUND));
        final UCSCGeneTableCodec codec = new UCSCGeneTableCodec(UCSCGeneTableCodec.Type.GENEPRED, null);
        final TribbleIndexedFeatureReader<BasicFeature, ?> reader = new TribbleIndexedFeatureReader<>(genePredFile,
                null,
                codec,
                false,
                null);
        final Iterable<BasicFeature> iterator = reader.iterator();
        Assert.isTrue(iterator.iterator().hasNext(), getMessage(MessageCode.ERROR_GENBANK_FILE_READING));
        try (Gff3Writer gff3Writer = new Gff3Writer(gffFilePath)) {
            for (BasicFeature f : iterator) {
                Gff3FeatureImpl feature = convertFeature(f);
                gff3Writer.addFeature(feature);
            }
        }
    }

    private Gff3FeatureImpl convertFeature(final BasicFeature f) {
        final Map<String, List<String>> attributes = new LinkedHashMap<>();
        attributes.put(ID_ATTR, Collections.singletonList(f.getIdentifier()));
        attributes.put(NAME_ATTR, Collections.singletonList(f.getName()));
        return new Gff3FeatureImpl(
                f.getChr(),
                SOURCE,
                GeneType.GENE.getGffName(),
                f.getStart(),
                f.getEnd(),
                Float.isNaN(f.getScore()) ? DOT : String.valueOf(f.getScore()),
                StrandSerializable.valueOf(f.getStrand().name()),
                DOT,
                attributes
        );
    }
}
