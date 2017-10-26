package com.epam.catgenome.manager.bam;

import com.epam.catgenome.entity.bam.PSLRecord;
import com.epam.catgenome.entity.reference.Species;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import org.springframework.stereotype.Service;

/**
 *
 */
@Service
public class BlatSearchManager {

    public PSLRecord find(String readSequence, Species species) throws ExternalDbUnavailableException {
        return null;
    }

}
