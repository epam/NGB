package com.epam.catgenome.manager.bam;

import com.epam.catgenome.entity.bam.PSLRecord;
import com.epam.catgenome.entity.reference.Species;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.HttpDataManager;
import com.epam.catgenome.manager.externaldb.ParameterNameValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class BlatSearchManager {

    @Value("#{catgenome['blat.search.url']}")
    private String blatURL;
    @Value("#{catgenome['blat.search.type']}")
    private String searchType;
    @Value("#{catgenome['blat.search.sort.order']}")
    private String sortOrder;
    @Value("#{catgenome['blat.search.output.type']}")
    private String outputType;

    @Autowired
    private PSLRecordParser pslRecordParser;

    @Autowired
    private HttpDataManager httpDataManager;

    public List<PSLRecord> find(String readSequence, Species species)
            throws ExternalDbUnavailableException, IOException {
        String response = httpDataManager.fetchData(blatURL + "?",
                createURLParameters(readSequence, species));
        return pslRecordParser.parse(response);
    }

    private ParameterNameValue[] createURLParameters(String sequense, Species species) {
        return new ParameterNameValue[] {
                new ParameterNameValue("org", species.getName()),
                new ParameterNameValue("db", species.getVersion()),
                new ParameterNameValue("type", searchType),
                new ParameterNameValue("sort", sortOrder),
                new ParameterNameValue("output", outputType),
                new ParameterNameValue("userSeq", sequense)
        };
    }
}
