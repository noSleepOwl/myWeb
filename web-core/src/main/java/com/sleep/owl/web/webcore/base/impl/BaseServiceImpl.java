package com.sleep.owl.web.webcore.base.impl;

import com.fasterxml.jackson.databind.ser.Serializers;
import com.sleep.owl.web.webcore.base.BaseRepository;
import com.sleep.owl.web.webcore.base.BaseService;
import com.sleep.owl.web.webcore.base.model.AbstractBaseModel;
import com.sleep.owl.web.webcore.base.model.BaseModel;
import com.sleep.owl.web.webcore.base.model.MarkDeleteableModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;

@SuppressWarnings("unchecked")
public abstract class BaseServiceImpl<T extends BaseModel<ID>, ID extends Serializable> implements BaseService<T, ID> {

    public abstract BaseRepository<T, ID> getDAO();

    @Override
    public T save(T t) {
        return getDAO().save(t);
    }

    @Override
    public Iterable<T> save(Iterable<T> entities) {
        return getDAO().saveAll(entities);
    }

    @Override
    public void del(ID id) {
        boolean isPresent = findById(id)
                .filter(t -> t instanceof MarkDeleteableModel)
                .map(t -> {
                    MarkDeleteableModel<T> baseModel = (MarkDeleteableModel<T>) t;
                    baseModel.setDel(1);
                    getDAO().save(t);
                    return t;
                }).isPresent();
        if (!isPresent) {
            getDAO().deleteById(id);
        }
    }

    @Override
    public void del(T t) {
        if (t instanceof MarkDeleteableModel) {
            MarkDeleteableModel<T> baseModel = (MarkDeleteableModel<T>) t;
            baseModel.setDel(1);
            getDAO().save(t);
        } else {
            getDAO().delete(t);
        }
    }

    @Override
    public Optional<T> findById(ID id) {
        return getDAO().findById(id);
    }

    @Override
    public List<T> findAll() {
        return getDAO().findAll();
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        return getDAO().findAll(pageable);
    }

    /**
     * {"字段名称:条件":"属性值"}
     *
     * @param params
     * @return
     */
    public Specification<T> getSpec(Map<String, Object> params) {
        return (Specification<T>) (root, query, cb) -> {
            List<Predicate> list = new ArrayList<>();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                Object value = entry.getValue();
                if (value == null || StringUtils.isEmpty(value.toString())) {
                    continue;
                }
                String key = entry.getKey();
                String[] arr = key.split(":");
                Predicate predicate = getPredicate(arr, value, root, cb);
                list.add(predicate);
            }
            Predicate[] p = new Predicate[list.size()];
            return cb.and(list.toArray(p));
        };
    }


    @Override
    public List<T> list(final Map<String, Object> params) {
        List<T> list = getDAO().findAll(getSpec(params));
        return list;
    }

    @Override
    public Page<T> list(final Map<String, Object> params, Pageable pageable) {
        Page<T> page = getDAO().findAll(getSpec(params), pageable);
        return page;
    }

    /**
     * @param arr 命令数组[name]  获取 [name,条件(eq , ne , lt ....)]
     */
    private Predicate getPredicate(String[] arr, Object value,
                                   Root<T> root, CriteriaBuilder cb) {
        if (arr.length == 1) {
            return cb.equal(root.get(arr[0]).as(value.getClass()), value);
        }
        if (QueryTypeEnum.like.name().equals(arr[1])) {
            return cb.like(root.get(arr[0]).as(String.class), String.format("%%%s%%", value));
        }
        if (QueryTypeEnum.ne.name().equals(arr[1])) {
            return cb.notEqual(root.get(arr[0]).as(value.getClass()), value);
        }
        if (QueryTypeEnum.lt.name().equals(arr[1])) {
            return getLessThanPredicate(arr, value, root, cb);
        }
        if (QueryTypeEnum.lte.name().equals(arr[1])) {
            return getLessThanOrEqualToPredicate(arr, value, root, cb);
        }
        if (QueryTypeEnum.gt.name().equals(arr[1])) {
            return getGreaterThanPredicate(arr, value, root, cb);
        }
        if (QueryTypeEnum.gte.name().equals(arr[1])) {
            return getGreaterThanOrEqualToPredicate(arr, value, root, cb);
        }
        throw new UnsupportedOperationException(String.format("不支持的查询类型[%s]", arr[1]));
    }

    private Predicate getLessThanPredicate(String[] arr, Object value,
                                           Root<T> root, CriteriaBuilder cb) {
        if (Integer.class == value.getClass()) {
            return cb.lessThan(root.get(arr[0]).as(Integer.class), (int) value);
        }
        if (Long.class == value.getClass()) {
            return cb.lessThan(root.get(arr[0]).as(Long.class), (long) value);
        }
        if (Double.class == value.getClass()) {
            return cb.lessThan(root.get(arr[0]).as(Double.class), (double) value);
        }
        if (Float.class == value.getClass()) {
            return cb.lessThan(root.get(arr[0]).as(Float.class), (float) value);
        }
        if (Timestamp.class == value.getClass()) {
            return cb.lessThan(root.get(arr[0]).as(Timestamp.class), (Timestamp) value);
        }
        if (Date.class == value.getClass()) {
            return cb.lessThan(root.get(arr[0]).as(Date.class), (Date) value);
        }
        return cb.lessThan(root.get(arr[0]).as(String.class), String.valueOf(value));
    }

    private Predicate getLessThanOrEqualToPredicate(String[] arr,
                                                    Object value, Root<T> root, CriteriaBuilder cb) {
        if (Integer.class == value.getClass()) {
            return cb.lessThanOrEqualTo(root.get(arr[0]).as(Integer.class), (int) value);
        }
        if (Long.class == value.getClass()) {
            return cb.lessThanOrEqualTo(root.get(arr[0]).as(Long.class), (long) value);
        }
        if (Double.class == value.getClass()) {
            return cb.lessThanOrEqualTo(root.get(arr[0]).as(Double.class), (double) value);
        }
        if (Float.class == value.getClass()) {
            return cb.lessThanOrEqualTo(root.get(arr[0]).as(Float.class), (float) value);
        }
        if (Timestamp.class == value.getClass()) {
            return cb.lessThanOrEqualTo(root.get(arr[0]).as(Timestamp.class), (Timestamp) value);
        }
        if (Date.class == value.getClass()) {
            return cb.lessThanOrEqualTo(root.get(arr[0]).as(Date.class), (Date) value);
        }
        return cb.lessThanOrEqualTo(root.get(arr[0]).as(String.class), String.valueOf(value));
    }

    private Predicate getGreaterThanPredicate(String[] arr,
                                              Object value, Root<T> root, CriteriaBuilder cb) {
        if (Integer.class == value.getClass()) {
            return cb.greaterThan(root.get(arr[0]).as(Integer.class), (int) value);
        }
        if (Long.class == value.getClass()) {
            return cb.greaterThan(root.get(arr[0]).as(Long.class), (long) value);
        }
        if (Double.class == value.getClass()) {
            return cb.greaterThan(root.get(arr[0]).as(Double.class), (double) value);
        }
        if (Float.class == value.getClass()) {
            return cb.greaterThan(root.get(arr[0]).as(Float.class), (float) value);
        }
        if (Timestamp.class == value.getClass()) {
            return cb.greaterThan(root.get(arr[0]).as(Timestamp.class), (Timestamp) value);
        }
        if (Date.class == value.getClass()) {
            return cb.greaterThan(root.get(arr[0]).as(Date.class), (Date) value);
        }
        return cb.greaterThan(root.get(arr[0]).as(String.class), String.valueOf(value));
    }

    private Predicate getGreaterThanOrEqualToPredicate(String[] arr, Object value,
                                                       Root<T> root, CriteriaBuilder cb) {
        if (Integer.class == value.getClass()) {
            return cb.greaterThanOrEqualTo(root.get(arr[0]).as(Integer.class), (int) value);
        }
        if (Long.class == value.getClass()) {
            return cb.greaterThanOrEqualTo(root.get(arr[0]).as(Long.class), (long) value);
        }
        if (Double.class == value.getClass()) {
            return cb.greaterThanOrEqualTo(root.get(arr[0]).as(Double.class), (double) value);
        }
        if (Float.class == value.getClass()) {
            return cb.greaterThanOrEqualTo(root.get(arr[0]).as(Float.class), (float) value);
        }
        if (Timestamp.class == value.getClass()) {
            return cb.greaterThanOrEqualTo(root.get(arr[0]).as(Timestamp.class), (Timestamp) value);
        }
        if (Date.class == value.getClass()) {
            return cb.greaterThanOrEqualTo(root.get(arr[0]).as(Date.class), (Date) value);
        }
        return cb.lessThanOrEqualTo(root.get(arr[0]).as(String.class), String.valueOf(value));
    }

}

