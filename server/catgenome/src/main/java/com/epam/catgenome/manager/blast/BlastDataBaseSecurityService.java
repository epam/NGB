package com.epam.catgenome.manager.blast;

import com.epam.catgenome.entity.blast.BlastDataBase;
import com.epam.catgenome.entity.blast.BlastDataBaseType;
import com.epam.catgenome.security.acl.aspect.AclTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.epam.catgenome.security.acl.SecurityExpressions.ROLE_ADMIN;
import static com.epam.catgenome.security.acl.SecurityExpressions.ROLE_USER;

@Service
public class BlastDataBaseSecurityService {

    @Autowired
    private BlastDataBaseManager blastDataBaseManager;

    @PreAuthorize(ROLE_ADMIN)
    public void save(final BlastDataBase blastDataBase) {
        blastDataBaseManager.save(blastDataBase);
    }

    @AclTree
    @PreAuthorize(ROLE_ADMIN)
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
