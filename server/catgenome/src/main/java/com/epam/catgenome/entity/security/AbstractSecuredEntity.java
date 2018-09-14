package com.epam.catgenome.entity.security;

import java.util.Date;
import java.util.Objects;

import com.epam.catgenome.entity.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@link AbstractSecuredEntity} represents an entity access to which may be
 * restricted via ACL (access control list) security layer. {@link AbstractSecuredEntity}
 * implies that permissions may be set for current object or may be inherited from
 * a parent {@link AbstractSecuredEntity}. Common rule is that only users with admin role
 * are allowed to manage entities without a parent ("root" entities), but this behaviour
 * may be overriden in security layer.
 */
@Getter
@Setter
@NoArgsConstructor
public abstract class AbstractSecuredEntity extends BaseEntity {
    public static final int ALL_PERMISSIONS_MASK = 15;
    public static final int ALL_PERMISSIONS_MASK_FULL = 85;

    /**
     * Represents permissions mask for currently authenticated user. Note that
     * some additional postprocessing is required to set the correct mask for
     * an entity. This postprocessing is enabled by annotating a method
     * returning {@link AbstractSecuredEntity} by some of
     * ACL annotations. See the detailed
     * information on supported permissions in {@code AclPermission} class.
     */
    private Integer mask = ALL_PERMISSIONS_MASK;

    /**
     * Username of creator or current owner of an entity
     */
    private String owner;
    private Date createdDate;

    /**
     * Flag indicating, whether item is locked from changes or not
     */
    private boolean locked = false;

    public AbstractSecuredEntity(Long id) {
        super();
        setId(id);
    }

    public AbstractSecuredEntity(Long id, String name, Date createdDate) {
        super(id, name);
        this.createdDate = createdDate;
    }

    public AbstractSecuredEntity(Long id, String name) {
        super(id, name);
    }

    /**
     * @return a parent {@link AbstractSecuredEntity} to inherit permissions,
     * that are not set for current entity.
     */
    public abstract AbstractSecuredEntity getParent();

    public void clearParent() {
        // nothing by default
    }

    /**
     * @return {@link AclClass} to which an entity belongs
     */
    public abstract AclClass getAclClass();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractSecuredEntity that = (AbstractSecuredEntity) o;
        return Objects.equals(getAclClass(), that.getAclClass()) &&
                Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAclClass(), getId());
    }
}
