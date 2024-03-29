package com.wizzdi.basic.iot.service.service;


import com.flexicore.model.Baseclass;
import com.flexicore.model.Basic;
import com.flexicore.security.SecurityContextBase;
import com.wizzdi.basic.iot.model.*;
import com.wizzdi.basic.iot.service.data.RemoteRepository;
import com.wizzdi.basic.iot.service.request.ConnectivityChangeCreate;
import com.wizzdi.basic.iot.service.request.RemoteCreate;
import com.wizzdi.basic.iot.service.request.RemoteFilter;
import com.wizzdi.basic.iot.service.request.RemoteUpdate;
import com.wizzdi.dynamic.properties.converter.DynamicPropertiesUtils;
import com.wizzdi.flexicore.boot.base.interfaces.Plugin;
import com.wizzdi.flexicore.security.events.BasicCreated;
import com.wizzdi.flexicore.security.interfaces.SecurityContextProvider;
import com.wizzdi.flexicore.security.response.PaginationResponse;
import com.wizzdi.flexicore.security.service.BaseclassService;
import com.wizzdi.flexicore.security.service.BasicService;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.metamodel.SingularAttribute;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Extension
@Component

public class RemoteService implements Plugin {

    private static final Logger logger= LoggerFactory.getLogger(RemoteService.class);

    @Autowired
    private RemoteRepository repository;

    @Autowired
    private BasicService basicService;
    @Autowired
    private ConnectivityChangeService connectivityChangeService;
    @Autowired
    @Lazy
    private SecurityContextProvider securityContextProvider;

    public <T extends Baseclass> List<T> listByIds(Class<T> c, Set<String> ids, SecurityContextBase securityContext) {
        return repository.listByIds(c, ids, securityContext);
    }

    public <T extends Baseclass> T getByIdOrNull(String id, Class<T> c, SecurityContextBase securityContext) {
        return repository.getByIdOrNull(id, c, securityContext);
    }

    public <D extends Basic, E extends Baseclass, T extends D> T getByIdOrNull(String id, Class<T> c, SingularAttribute<D, E> baseclassAttribute, SecurityContextBase securityContext) {
        return repository.getByIdOrNull(id, c, baseclassAttribute, securityContext);
    }

    public <D extends Basic, E extends Baseclass, T extends D> List<T> listByIds(Class<T> c, Set<String> ids, SingularAttribute<D, E> baseclassAttribute, SecurityContextBase securityContext) {
        return repository.listByIds(c, ids, baseclassAttribute, securityContext);
    }

    public <D extends Basic, T extends D> List<T> findByIds(Class<T> c, Set<String> ids, SingularAttribute<D, String> idAttribute) {
        return repository.findByIds(c, ids, idAttribute);
    }

    public <T extends Basic> List<T> findByIds(Class<T> c, Set<String> requested) {
        return repository.findByIds(c, requested);
    }

    public <T> T findByIdOrNull(Class<T> type, String id) {
        return repository.findByIdOrNull(type, id);
    }

    @Transactional
    public void merge(Object base) {
        repository.merge(base);
    }

    @Transactional
    public void massMerge(List<?> toMerge) {
        repository.massMerge(toMerge);
    }

    public void validateFiltering(RemoteFilter remoteFilter,
                                  SecurityContextBase securityContext) {
        basicService.validate(remoteFilter, securityContext);
    }

    public PaginationResponse<Remote> getAllRemotes(
            SecurityContextBase securityContext, RemoteFilter filtering) {
        List<Remote> list = listAllRemotes(securityContext, filtering);
        long count = repository.countAllRemotes(securityContext, filtering);
        return new PaginationResponse<>(list, filtering, count);
    }

    public List<Remote> listAllRemotes(SecurityContextBase securityContext, RemoteFilter remoteFilter) {
        return repository.getAllRemotes(securityContext, remoteFilter);
    }

    public Remote createRemote(RemoteCreate creationContainer,
                                 SecurityContextBase securityContext) {
        Remote remote = createRemoteNoMerge(creationContainer, securityContext);
        repository.merge(remote);
        return remote;
    }

    public Remote createRemoteNoMerge(RemoteCreate creationContainer,
                                        SecurityContextBase securityContext) {
        Remote remote = new Remote();
        remote.setId(UUID.randomUUID().toString());

        updateRemoteNoMerge(remote, creationContainer);
        BaseclassService.createSecurityObjectNoMerge(remote, securityContext);
        return remote;
    }

    public boolean updateRemoteNoMerge(Remote remote,
                                        RemoteCreate remoteCreate) {
        boolean update = basicService.updateBasicNoMerge(remoteCreate, remote);
        if (remoteCreate.getRemoteId() != null && !remoteCreate.getRemoteId().equals(remote.getRemoteId())) {
            remote.setRemoteId(remoteCreate.getRemoteId());
            update = true;
        }
        if (remoteCreate.getVersion() != null && !remoteCreate.getVersion().equals(remote.getVersion())) {
            remote.setVersion(remoteCreate.getVersion());
            update = true;
        }
        if (remoteCreate.getLastSeen() != null && !remoteCreate.getLastSeen().equals(remote.getLastSeen())) {
            remote.setLastSeen(remoteCreate.getLastSeen());
            update = true;
        }
        if (remoteCreate.getLockLocation() != null && !remoteCreate.getLockLocation().equals(remote.isLockLocation())) {
            remote.setLockLocation(remoteCreate.getLockLocation());
            update = true;
        }
        if (remoteCreate.getCurrentSchema() != null && (remote.getCurrentSchema()==null||!remoteCreate.getCurrentSchema().getId().equals(remote.getCurrentSchema().getId()))) {
            remote.setCurrentSchema(remoteCreate.getCurrentSchema());
            update = true;
        }
        if (remoteCreate.getMappedPOI() != null && (remote.getMappedPOI()==null||!remoteCreate.getMappedPOI().getId().equals(remote.getMappedPOI().getId()))) {
            remote.setMappedPOI(remoteCreate.getMappedPOI());
            update = true;
        }
        Map<String, Object> mergedDeviceValues = DynamicPropertiesUtils.updateDynamic(remoteCreate.getDeviceProperties(), remote.getDeviceProperties());

        if (mergedDeviceValues != null) {
            remote.setDeviceProperties(mergedDeviceValues);
            update = true;
        }

        Map<String, Object> mergedUserValues = DynamicPropertiesUtils.updateDynamic(remoteCreate.getUserAddedProperties(), remote.getUserAddedProperties());

        if (mergedUserValues != null) {
            remote.setUserAddedProperties(mergedUserValues);
            update = true;
        }

        return update;
    }

    public Remote updateRemote(RemoteUpdate remoteUpdate,
                                 SecurityContextBase securityContext) {
        Remote remote = remoteUpdate.getRemote();
        if (updateRemoteNoMerge(remote, remoteUpdate)) {
            repository.merge(remote);
        }
        return remote;
    }

    public SecurityContextBase getRemoteSecurityContext(Remote remote) {
        if(remote instanceof Device){
            return getRemoteSecurityContext(((Device) remote).getGateway());
        }
        if(remote instanceof Gateway){
            return securityContextProvider.getSecurityContext(((Gateway) remote).getGatewayUser());
        }
        return null;
    }
    @EventListener
    public void onGatewayCreated(BasicCreated<Gateway> remoteCreated){
        onRemoteCreated(remoteCreated.getBaseclass());
    }
    @EventListener
    public void onDeviceCreated(BasicCreated<Device> remoteCreated){
        onRemoteCreated(remoteCreated.getBaseclass());
    }
    public void onRemoteCreated(Remote remote){
        ConnectivityChange connectivityChange = connectivityChangeService.createConnectivityChange(new ConnectivityChangeCreate().setDate(OffsetDateTime.now()).setConnectivity(Connectivity.OFF).setRemote(remote), getRemoteSecurityContext(remote));
        remote.setLastConnectivityChange(connectivityChange);
        merge(remote);
        logger.info("default connectivity created for remote "+remote.getRemoteId() +"("+remote.getId()+")");
    }

    public void validate(RemoteCreate remoteCreate,
                         SecurityContextBase securityContext) {
        basicService.validate(remoteCreate, securityContext);
    }

    public Gateway getGateway(Remote remote){
        if(remote instanceof Gateway ){
            return (Gateway) remote;
        }
        if(remote instanceof Device){
            return ((Device) remote).getGateway();
        }
        return null;
    }
}
