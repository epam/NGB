package com.epam.catgenome.manager.blast;

import com.epam.catgenome.dao.blast.BlastDataBaseDao;
import com.epam.catgenome.entity.blast.BlastDataBase;
import com.epam.catgenome.entity.blast.BlastDataBaseType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BlastDataBaseManager {

    private final BlastDataBaseDao dataBaseDao;

    @Transactional(propagation = Propagation.REQUIRED)
    public void save(final BlastDataBase blastDataBase) {
        dataBaseDao.saveDataBase(blastDataBase);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void delete(final long id) {
        dataBaseDao.deleteDataBase(id);
    }

    public BlastDataBase loadById(long id) {
        return dataBaseDao.loadDataBase(id);
    }

    public List<BlastDataBase> load(BlastDataBaseType type) {
        return dataBaseDao.loadDataBases(type);
    }
}
