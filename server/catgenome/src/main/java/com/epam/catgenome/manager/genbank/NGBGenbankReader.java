/*
 *                    BioJava development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the individual
 * authors.  These should be listed in @author doc comments.
 *
 * @author Scooter Willis ;lt;willishf at gmail dot com&gt;
 * @author Karl Nicholas <github:karlnicholas>
 * @author Paolo Pavan
 *
 * For more information on the BioJava project and its aims,
 * or to join the biojava-l mailing list, visit the home page
 * at:
 *
 *      http://www.biojava.org/
 *
 * Created on 01-21-2010
 */

package com.epam.catgenome.manager.genbank;

import org.biojava.nbio.core.exceptions.CompoundNotFoundException;
import org.biojava.nbio.core.sequence.DataSource;
import org.biojava.nbio.core.sequence.TaxonomyID;
import org.biojava.nbio.core.sequence.features.AbstractFeature;
import org.biojava.nbio.core.sequence.features.DBReferenceInfo;
import org.biojava.nbio.core.sequence.io.GenbankSequenceParser;
import org.biojava.nbio.core.sequence.io.template.SequenceCreatorInterface;
import org.biojava.nbio.core.sequence.template.AbstractSequence;
import org.biojava.nbio.core.sequence.template.Compound;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class NGBGenbankReader<S extends AbstractSequence<C>, C extends Compound> {

    private final SequenceCreatorInterface<C> sequenceCreator;
    private final GenbankSequenceParser<S, C> genbankParser;
    private final InputStream inputStream;

    public NGBGenbankReader(InputStream is, SequenceCreatorInterface<C> sequenceCreator) {
        this.sequenceCreator = sequenceCreator;
        this.inputStream = is;
        genbankParser = new GenbankSequenceParser<>();
    }

    /**
     * If you are going to use the FileProxyProteinSequenceCreator then you
     * need to use this constructor because we need details about
     * the location of the file.
     *
     * @param file
     * @param sequenceCreator
     * @throws FileNotFoundException if the file does not exist, is a directory
     *                               rather than a regular file, or for some other reason cannot be opened
     *                               for reading.
     * @throws SecurityException     if a security manager exists and its checkRead
     *                               method denies read access to the file.
     */
    public NGBGenbankReader(File file, SequenceCreatorInterface<C> sequenceCreator) throws FileNotFoundException {
        inputStream = new FileInputStream(file);
        this.sequenceCreator = sequenceCreator;
        genbankParser = new GenbankSequenceParser<>();
    }

    /**
     * The parsing is done in this method.<br>
     * This method tries to process all the available Genbank records
     * in the File or InputStream, closes the underlying resource,
     * and return the results in {@link LinkedHashMap}.<br>
     * You don't need to call {@link #close()} after calling this method.
     *
     * @return {@link HashMap} containing all the parsed Genbank records
     * present, starting current fileIndex onwards.
     * @throws IOException
     * @throws CompoundNotFoundException
     * @see #process(int)
     */
    public LinkedHashMap<String, S> process() throws IOException, CompoundNotFoundException {
        return process(-1);
    }

    /**
     * This method tries to parse maximum <code>max</code> records from
     * the open File or InputStream, and leaves the underlying resource open.<br>
     * <p>
     * Subsequent calls to the same method continue parsing the rest of the file.<br>
     * This is particularly useful when dealing with very big data files,
     * (e.g. NCBI nr database), which can't fit into memory and will take long
     * time before the first result is available.<br>
     * <b>N.B.</b>
     * <ul>
     * <li>This method ca't be called after calling its NO-ARGUMENT twin.</li>
     * <li>remember to close the underlying resource when you are done.</li>
     * </ul>
     *
     * @param max maximum number of records to return, <code>-1</code> for infinity.
     * @return {@link HashMap} containing maximum <code>max</code> parsed Genbank records
     * present, starting current fileIndex onwards.
     * @throws IOException
     * @throws CompoundNotFoundException
     * @author Amr AL-Hossary
     * @see #process()
     * @since 3.0.6
     */
    public LinkedHashMap<String, S> process(int max) throws IOException, CompoundNotFoundException {
        LinkedHashMap<String, S> sequences = new LinkedHashMap<>();
        @SuppressWarnings("unchecked")
        int i = 0;
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        while (true) {
            if (max > 0 && i >= max) {
                break;
            }
            i++;
            String seqString = genbankParser.getSequence(br, 0);
            //reached end of file?
            if (seqString == null) {
                break;
            }
            S sequence = (S) sequenceCreator.getSequence(seqString, 0);
            genbankParser.getSequenceHeaderParser().parseHeader(genbankParser.getHeader(), sequence);

            // add features to new sequence
            for (String k : genbankParser.getFeatures().keySet()) {
                for (AbstractFeature f : genbankParser.getFeatures(k)) {
                    sequence.addFeature(f);
                }
            }

            // add taxonomy ID to new sequence
            ArrayList<DBReferenceInfo> dbQualifier = genbankParser.getDatabaseReferences().get("db_xref");
            if (dbQualifier != null) {
                DBReferenceInfo q = dbQualifier.get(0);
                sequence.setTaxonomy(new TaxonomyID(q.getDatabase() + ":" + q.getId(), DataSource.GENBANK));
            }
            sequences.put(GenbankUtils.getSequenceId(sequence), sequence);
        }
        br.close();
        close();
        return sequences;
    }

    public void close() throws IOException {
        inputStream.close();
    }
}
