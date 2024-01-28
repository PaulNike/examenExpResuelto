package com.codigo.msexamenexp.service.impl;

import com.codigo.libreriacodigo.config.RedisService;
import com.codigo.msexamenexp.aggregates.request.RequestEnterprises;
import com.codigo.msexamenexp.aggregates.response.ResponseBase;
import com.codigo.msexamenexp.aggregates.constants.Constants;
import com.codigo.msexamenexp.aggregates.response.ResponseSunat;
import com.codigo.msexamenexp.entity.DocumentsTypeEntity;
import com.codigo.msexamenexp.entity.EnterprisesEntity;
import com.codigo.msexamenexp.entity.EnterprisesTypeEntity;
import com.codigo.msexamenexp.feignclient.SunatClient;
import com.codigo.msexamenexp.repository.DocumentsTypeRepository;
import com.codigo.msexamenexp.repository.EnterprisesRepository;
import com.codigo.msexamenexp.service.EnterprisesService;
import com.codigo.msexamenexp.util.EnterprisesValidations;
import com.codigo.msexamenexp.util.Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;
import java.util.Optional;


@Service
public class EnterprisesServiceImpl implements EnterprisesService {

    private final EnterprisesRepository enterprisesRepository;
    private final EnterprisesValidations enterprisesValidations;
    private final DocumentsTypeRepository typeRepository;
    private final SunatClient sunatClient;
    private final RedisService redisService;


    @Value("${token.api.sunat}")
    private String tokenSunat;

    @Value("${time.expiration.sunat.info}")
    private String timeExpirationSunatInfo;

    public EnterprisesServiceImpl(EnterprisesRepository enterprisesRepository, EnterprisesValidations enterprisesValidations, DocumentsTypeRepository typeRepository, SunatClient sunatClient, RedisService redisService) {
        this.enterprisesRepository = enterprisesRepository;
        this.enterprisesValidations = enterprisesValidations;
        this.typeRepository = typeRepository;
        this.sunatClient = sunatClient;
        this.redisService = redisService;
    }

    @Override
    public ResponseBase createEnterprise(RequestEnterprises requestEnterprises) {
        boolean validate = enterprisesValidations.validateInput(requestEnterprises);
        if(validate){
            EnterprisesEntity enterprises = getEntity(requestEnterprises);
            enterprisesRepository.save(enterprises);
            return new ResponseBase(Constants.CODE_SUCCESS,Constants.MESS_SUCCESS, Optional.of(enterprises));
        }else{
            return new ResponseBase(Constants.CODE_ERROR_DATA_INPUT,Constants.MESS_ERROR_DATA_NOT_VALID,null);
        }
    }

    @Override
    public ResponseBase findOneEnterprise(String doc) {
        String redisCache = redisService.getValueFromCache(Constants.REDIS_KEY_INFO_SUNAT+doc);
        if(redisCache!= null){
            EnterprisesEntity entity = Util.convertFromJson(redisCache,EnterprisesEntity.class);
            return new ResponseBase(Constants.CODE_SUCCESS,Constants.MESS_SUCCESS, Optional.of(entity));
        }else{
            EnterprisesEntity enterprisesEntity = enterprisesRepository.findByNumDocument(doc);
            if(enterprisesEntity != null){
                String redisData = Util.convertToJsonEntity(enterprisesEntity);
                redisService.saveInCache(Constants.REDIS_KEY_INFO_SUNAT+doc,redisData,Integer.valueOf(timeExpirationSunatInfo));
                return new ResponseBase(Constants.CODE_SUCCESS,Constants.MESS_SUCCESS, Optional.of(enterprisesEntity));

            }else{
                return new ResponseBase(Constants.CODE_ERROR_DATA_NOT,Constants.MESS_NON_DATA,Optional.empty());
            }
        }
    }

    @Override
    public ResponseBase findAllEnterprises() {
        Optional allEnterprises = Optional.of(enterprisesRepository.findAll());
        if(allEnterprises.isPresent()){
            return new ResponseBase(Constants.CODE_SUCCESS,Constants.MESS_SUCCESS,allEnterprises);
        }
        return new ResponseBase(Constants.CODE_ERROR_DATA_NOT,Constants.MESS_ZERO_ROWS,Optional.empty());
    }

    @Override
    public ResponseBase updateEnterprise(Integer id, RequestEnterprises requestEnterprises) {
        boolean existEnterprise = enterprisesRepository.existsById(id);
        if(existEnterprise){
            Optional<EnterprisesEntity> enterprises = enterprisesRepository.findById(id);
            boolean validationEntity = enterprisesValidations.validateInputUpdate(requestEnterprises);
            if(validationEntity){
                EnterprisesEntity enterprisesUpdate = getEntityUpdate(requestEnterprises,enterprises.isPresent()?enterprises.get():enterprises.orElse(null));
                enterprisesRepository.save(enterprisesUpdate);
                return new ResponseBase(Constants.CODE_SUCCESS,Constants.MESS_SUCCESS,Optional.of(enterprisesUpdate));
            }else {
                return new ResponseBase(Constants.CODE_ERROR_DATA_INPUT,Constants.MESS_ERROR_DATA_NOT_VALID,Optional.empty());
            }
        }else {
            return new ResponseBase(Constants.CODE_ERROR_DATA_NOT,Constants.MESS_ERROR_NOT_UPDATE,Optional.empty());
        }

    }

    @Override
    public ResponseBase delete(Integer id) {
        Optional<EnterprisesEntity> enterprises = enterprisesRepository.findById(id);
        enterprisesRepository.save(getEntityDelete(enterprises.get()));
        return new ResponseBase(Constants.CODE_SUCCESS,Constants.MESS_SUCCESS,Optional.of(enterprisesRepository.findById(id)));

    }

    private EnterprisesEntity getEntity(RequestEnterprises requestEnterprises){
        EnterprisesEntity entity = new EnterprisesEntity();
        ResponseSunat sunat = getExecutionSunat(requestEnterprises.getNumDocument());
        if(sunat != null){
            entity.setNumDocument(sunat.getNumeroDocumento());
            entity.setBusinessName(sunat.getRazonSocial());
            entity.setTradeName(enterprisesValidations.isNullOrEmpty(requestEnterprises.getTradeName()) ? sunat.getRazonSocial() : requestEnterprises.getTradeName());

        }
        entity.setStatus(Constants.STATUS_ACTIVE);
        entity.setEnterprisesTypeEntity(getEnterprisesType(requestEnterprises));
        entity.setDocumentsTypeEntity(getDocumentsType());
        entity.setUserCreate(Constants.AUDIT_ADMIN);
        entity.setDateCreate(getTimestamp());

        return entity;
    }
    private EnterprisesEntity getEntityUpdate(RequestEnterprises requestEnterprises, EnterprisesEntity enterprisesEntity){
        enterprisesEntity.setNumDocument(requestEnterprises.getNumDocument());
        enterprisesEntity.setBusinessName(requestEnterprises.getBusinessName());
        ResponseSunat sunat = getExecutionSunat(requestEnterprises.getNumDocument());
        if(sunat != null){
            enterprisesEntity.setNumDocument(sunat.getNumeroDocumento());
            enterprisesEntity.setBusinessName(sunat.getRazonSocial());
            enterprisesEntity.setTradeName(enterprisesValidations.isNullOrEmpty(requestEnterprises.getTradeName()) ? sunat.getRazonSocial() : requestEnterprises.getTradeName());

        }
        enterprisesEntity.setEnterprisesTypeEntity(getEnterprisesType(requestEnterprises));
        enterprisesEntity.setUserModif(Constants.AUDIT_ADMIN);
        enterprisesEntity.setDateModif(getTimestamp());
        return enterprisesEntity;
    }
    private EnterprisesEntity getEntityDelete(EnterprisesEntity enterprisesEntity){
        enterprisesEntity.setStatus(Constants.STATUS_INACTIVE);
        enterprisesEntity.setUserDelete(Constants.AUDIT_ADMIN);
        enterprisesEntity.setDateDelete(getTimestamp());
        return enterprisesEntity;
    }

    private EnterprisesTypeEntity getEnterprisesType(RequestEnterprises requestEnterprises){
        EnterprisesTypeEntity typeEntity = new EnterprisesTypeEntity();

        typeEntity.setIdEnterprisesType(requestEnterprises.getEnterprisesTypeEntity());
        return typeEntity;
    }

    private DocumentsTypeEntity getDocumentsType(){
        return  typeRepository.findByCodType(Constants.COD_TYPE_RUC);
    }

    private Timestamp getTimestamp(){
        long currentTime = System.currentTimeMillis();
        return new Timestamp(currentTime);
    }
    public ResponseSunat getExecutionSunat(String numero){
        String authorization = "Bearer "+tokenSunat;
        return sunatClient.getInfoSunat(numero,authorization);
    }
}
