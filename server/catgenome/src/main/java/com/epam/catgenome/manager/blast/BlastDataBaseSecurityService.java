package com.epam.catgenome.manager.blast;

import com.epam.catgenome.controller.Result;
import com.epam.catgenome.controller.vo.BlastDataBaseVO;
import com.epam.catgenome.entity.blast.BlastDataBase;
import com.epam.catgenome.entity.blast.BlastDataBaseType;
import com.epam.catgenome.security.acl.aspect.AclMask;
import com.epam.catgenome.security.acl.aspect.AclTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.epam.catgenome.security.acl.SecurityExpressions.ROLE_ADMIN;
import static com.epam.catgenome.security.acl.SecurityExpressions.ROLE_USER;

@Service
public class BlastDataBaseSecurityService {

    @Autowired
    private BlastDataBaseManager blastDataBaseManager;

    @AclTree
    @AclMask
//    @PreAuthorize(ROLE_ADMIN)
    @PreAuthorize(ROLE_USER)
    public void save(final BlastDataBaseVO dataBaseVO) {
        BlastDataBase blastDataBase = new BlastDataBase();
        blastDataBase.setName(dataBaseVO.getName());
        blastDataBase.setType(dataBaseVO.getType());
        blastDataBase.setPath(dataBaseVO.getPath());
        blastDataBaseManager.save(blastDataBase);
    }

    @AclTree

//    @PreAuthorize(ROLE_ADMIN)
    @PreAuthorize(ROLE_USER)
    public void delete(final long id) {
        blastDataBaseManager.delete(id);
    }

    @PreAuthorize(ROLE_USER)
    public BlastDataBase loadById(long id) {
        return blastDataBaseManager.loadById(id);
    }

    @PreAuthorize(ROLE_USER)
    public List<BlastDataBase> load(BlastDataBaseType type) {
        return blastDataBaseManager.load(type);
    }
}
