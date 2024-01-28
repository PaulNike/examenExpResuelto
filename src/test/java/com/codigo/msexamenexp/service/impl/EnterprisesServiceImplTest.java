package com.codigo.msexamenexp.service.impl;

import com.codigo.libreriaCodigo.config.RedisService;
import com.codigo.msexamenexp.aggregates.constants.Constants;
import com.codigo.msexamenexp.aggregates.request.RequestEnterprises;
import com.codigo.msexamenexp.aggregates.response.ResponseBase;
import com.codigo.msexamenexp.aggregates.response.ResponseSunat;
import com.codigo.msexamenexp.entity.DocumentsTypeEntity;
import com.codigo.msexamenexp.entity.EnterprisesEntity;
import com.codigo.msexamenexp.entity.EnterprisesTypeEntity;
import com.codigo.msexamenexp.feignclient.SunatClient;
import com.codigo.msexamenexp.repository.DocumentsTypeRepository;
import com.codigo.msexamenexp.repository.EnterprisesRepository;
import com.codigo.msexamenexp.repository.EnterprisesTypeRepository;
import com.codigo.msexamenexp.util.EnterprisesValidations;
import com.codigo.msexamenexp.util.Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

class EnterprisesServiceImplTest {

    @Mock
    EnterprisesRepository enterprisesRepository;
    @Mock
    EnterprisesValidations enterprisesValidations;
    @Mock
    DocumentsTypeRepository documentsTypeRepository;
    @Mock
    RedisService redisService;
    @Mock
    EnterprisesTypeRepository enterprisesTypeRepository;
    @Mock
    Util util;
    @Mock
    SunatClient sunatClient;

    @InjectMocks
    EnterprisesServiceImpl enterprisesService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        enterprisesService = new EnterprisesServiceImpl(enterprisesRepository,enterprisesValidations,documentsTypeRepository,sunatClient, redisService);
    }

    @Test
    void getInfoSunatSucceed() {
        String numero = "20602356459";
        ResponseSunat responseSunat = new ResponseSunat(
                "POLLOS GORDOS S.A.C.",
                "6",
                "20602356459",
                "ACTIVO",
                "HABIDO",
                "AV. PETIT THOUARS NRO 9988 URB. VALLECITO ",
                "123456",
                "AV.",
                "PETIT THOUARS",
                "URB.",
                "VALLECITO",
                "9988",
                "-",
                "-",
                "-",
                "-",
                "-",
                "MIRAFLORES",
                "AREQUIPA",
                "AREQUIPA",
                true
        );
        ResponseBase responseBaseExpected = new ResponseBase(Constants.CODE_SUCCESS, Constants.MESS_SUCCESS, Optional.of(responseSunat));

        Mockito.when(sunatClient.getInfoSunat(anyString(), anyString())).thenReturn(responseSunat);

        ResponseSunat sunat = enterprisesService.getExecutionSunat(numero);
        ResponseBase responseBase = new ResponseBase(Constants.CODE_SUCCESS, Constants.MESS_SUCCESS, Optional.of(sunat));

        assertEquals(responseBase.getCode(), responseBaseExpected.getCode());
        assertEquals(responseBase.getMessage(), responseBaseExpected.getMessage());
        assertEquals(responseBase.getData(), responseBaseExpected.getData());
    }
    @Test
    void getInfoSunatError() {
        String numero = "XDCCCCCC";
        Mockito.when(sunatClient.getInfoSunat(anyString(), anyString())).thenReturn(null);
        ResponseSunat sunat = enterprisesService.getExecutionSunat(numero);
        assertNull(sunat);
    }

    @Test
    void createEnterpriseSucceed() {
        boolean validateEnterprise = true;
        RequestEnterprises requestEnterprises = new RequestEnterprises(
                "20602356459",
                "Los Pollos Gordos",
                "POLLOS GORDOS S.A.C.",
                2,
                3
        );
        ResponseSunat responseSunat = new ResponseSunat(
                "POLLOS GORDOS S.A.C.",
                "6",
                "20602356459",
                "ACTIVO",
                "HABIDO",
                "AV. PETIT THOUARS NRO 9988 URB. VALLECITO ",
                "123456",
                "AV.",
                "PETIT THOUARS",
                "URB.",
                "VALLECITO",
                "9988",
                "-",
                "-",
                "-",
                "-",
                "-",
                "MIRAFLORES",
                "AREQUIPA",
                "AREQUIPA",
                true
        );
        EnterprisesTypeEntity enterprisesTypeEntity = new EnterprisesTypeEntity(
                2,
                "02",
                "SAC",
                1
        );
        DocumentsTypeEntity documentsTypeEntity = new DocumentsTypeEntity(
                3,
                "06",
                "RUC",
                1
        );
        EnterprisesEntity enterprisesEntity = new EnterprisesEntity(
                1,
                "20602356459",
                "Los Pollos Gordos",
                "POLLOS GORDOS S.A.C.",
                1,
                enterprisesTypeEntity,
                documentsTypeEntity
        );
        ResponseBase responseBaseExpected = new ResponseBase(Constants.CODE_SUCCESS, Constants.MESS_SUCCESS, Optional.of(enterprisesEntity));

        EnterprisesServiceImpl spy = Mockito.spy(enterprisesService);

        Mockito.when(enterprisesValidations.validateInput(Mockito.any(RequestEnterprises.class))).thenReturn(validateEnterprise);
        Mockito.doReturn(responseSunat).when(spy).getExecutionSunat(anyString());
        Mockito.when(enterprisesTypeRepository.findById(anyInt())).thenReturn(Optional.of(enterprisesTypeEntity));
        Mockito.when(enterprisesRepository.save(Mockito.any(EnterprisesEntity.class))).thenReturn(enterprisesEntity);
        Mockito.when(documentsTypeRepository.findById(anyInt())).thenReturn(Optional.of(documentsTypeEntity));
        ResponseBase responseBase = spy.createEnterprise(requestEnterprises);
        assertEquals(responseBase.getCode(), responseBaseExpected.getCode());
        assertEquals(responseBase.getMessage(), responseBaseExpected.getMessage());
    }

}