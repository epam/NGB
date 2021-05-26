package com.epam.catgenome.manager.blast;

import com.epam.catgenome.dao.blast.BlastDataBaseDao;
import com.epam.catgenome.entity.blast.BlastDataBase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BlastDataBaseManager {

    private final BlastDataBaseDao dataBaseDao;

    public void save(final BlastDataBase blastDataBase) {
        dataBaseDao.saveDataBase(blastDataBase);
    }

    public void delete(final long id) {
        dataBaseDao.deleteDataBase(id);
    }
}
