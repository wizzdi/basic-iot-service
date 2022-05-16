
package com.wizzdi.basic.iot.service.data;


import com.flexicore.model.Baseclass;
import com.flexicore.model.Basic;
import com.flexicore.security.SecurityContextBase;
import com.wizzdi.basic.iot.model.*;
import com.wizzdi.basic.iot.service.request.DeviceFilter;
import com.wizzdi.flexicore.boot.base.interfaces.Plugin;
import com.wizzdi.flexicore.security.data.BasicRepository;
import org.pf4j.Extension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import javax.persistence.metamodel.SingularAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Extension
@Component
public class DeviceRepository implements Plugin {
    @PersistenceContext
    private EntityManager em;
    @Autowired
    private RemoteRepository remoteRepository;
    @Autowired
    private DeviceTypeRepository deviceTypeRepository;

    public List<Device> getAllDevices(SecurityContextBase securityContext,
                                           DeviceFilter filtering) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Device> q = cb.createQuery(Device.class);
        Root<Device> r = q.from(Device.class);
        List<Predicate> preds = new ArrayList<>();
        addDevicePredicates(filtering, cb, q, r, preds, securityContext);
        q.select(r).where(preds.toArray(Predicate[]::new)).orderBy(cb.desc(r.get(Device_.name)));
        TypedQuery<Device> query = em.createQuery(q);
        BasicRepository.addPagination(filtering, query);
        return query.getResultList();
    }

    public long countAllDevices(SecurityContextBase securityContext,
                                   DeviceFilter filtering) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<Device> r = q.from(Device.class);
        List<Predicate> preds = new ArrayList<>();
        addDevicePredicates(filtering, cb, q, r, preds, securityContext);
        q.select(cb.count(r)).where(preds.toArray(Predicate[]::new));
        TypedQuery<Long> query = em.createQuery(q);
        return query.getSingleResult();
    }

    public <T extends Device> void addDevicePredicates(DeviceFilter filtering,
                                                           CriteriaBuilder cb, CommonAbstractCriteria q, From<?, T> r, List<Predicate> preds, SecurityContextBase securityContext) {
        remoteRepository.addRemotePredicates(filtering,cb,q,r,preds,securityContext);
        if(filtering.getGateways()!=null&&!filtering.getGateways().isEmpty()){
            Set<String> ids=filtering.getGateways().stream().map(f->f.getId()).collect(Collectors.toSet());
            Join<T, Gateway> join=r.join(Device_.gateway);
            preds.add(join.get(Gateway_.id).in(ids));
        }

        if(filtering.getDeviceTypes()!=null&&!filtering.getDeviceTypes().isEmpty()){
            Set<String> ids=filtering.getDeviceTypes().stream().map(f->f.getId()).collect(Collectors.toSet());
            Join<T, DeviceType> join=r.join(Device_.deviceType);
            preds.add(join.get(DeviceType_.id).in(ids));
        }
        if(filtering.getDeviceTypeFilter()!=null){
            Join<T, DeviceType> join=r.join(Device_.deviceType);
            deviceTypeRepository.addDeviceTypePredicates(filtering.getDeviceTypeFilter(),cb,q,join,preds,securityContext);
        }



    }

    public <T extends Baseclass> List<T> listByIds(Class<T> c, Set<String> ids, SecurityContextBase securityContext) {
        return remoteRepository.listByIds(c, ids, securityContext);
    }

    public <T extends Baseclass> T getByIdOrNull(String id, Class<T> c, SecurityContextBase securityContext) {
        return remoteRepository.getByIdOrNull(id, c, securityContext);
    }

    public <D extends Basic, E extends Baseclass, T extends D> T getByIdOrNull(String id, Class<T> c, SingularAttribute<D, E> baseclassAttribute, SecurityContextBase securityContext) {
        return remoteRepository.getByIdOrNull(id, c, baseclassAttribute, securityContext);
    }

    public <D extends Basic, E extends Baseclass, T extends D> List<T> listByIds(Class<T> c, Set<String> ids, SingularAttribute<D, E> baseclassAttribute, SecurityContextBase securityContext) {
        return remoteRepository.listByIds(c, ids, baseclassAttribute, securityContext);
    }

    public <D extends Basic, T extends D> List<T> findByIds(Class<T> c, Set<String> ids, SingularAttribute<D, String> idAttribute) {
        return remoteRepository.findByIds(c, ids, idAttribute);
    }

    public <T extends Basic> List<T> findByIds(Class<T> c, Set<String> requested) {
        return remoteRepository.findByIds(c, requested);
    }

    public <T> T findByIdOrNull(Class<T> type, String id) {
        return remoteRepository.findByIdOrNull(type, id);
    }

    @Transactional
    public void merge(Object base) {
        remoteRepository.merge(base);
    }

    @Transactional
    public void massMerge(List<?> toMerge) {
        remoteRepository.massMerge(toMerge);
    }

}