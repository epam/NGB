package com.epam.catgenome.manager.blast;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.blast.BlastDataBaseDao;
import com.epam.catgenome.entity.blast.BlastDataBase;
import com.epam.catgenome.entity.blast.BlastDataBaseType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;

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
        BlastDataBase dataBase = dataBaseDao.loadDataBase(id);
        Assert.notNull(dataBase, MessageHelper.getMessage(MessagesConstants.ERROR_DATA_BASE_NOT_FOUND, id));
        dataBaseDao.deleteDataBase(id);
    }

    public BlastDataBase loadById(long id) {
        BlastDataBase dataBase = dataBaseDao.loadDataBase(id);
        Assert.notNull(dataBase, MessageHelper.getMessage(MessagesConstants.ERROR_DATA_BASE_NOT_FOUND, id));
        return dataBase;
    }

    public List<BlastDataBase> load(BlastDataBaseType type) {
        return dataBaseDao.loadDataBases(type);
    }
}
