package com.codigo.msexamenexp.controller;


import com.codigo.libreriaCodigo.config.RedisService;
import com.codigo.msexamenexp.aggregates.request.RequestEnterprises;
import com.codigo.msexamenexp.aggregates.response.ResponseBase;
import com.codigo.msexamenexp.service.EnterprisesService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/enterprises")
public class EnterprisesController {

    private final RedisService redisService;
    private final EnterprisesService enterprisesService;

    public EnterprisesController(RedisService redisService, EnterprisesService enterprisesService) {
        this.redisService = redisService;
        this.enterprisesService = enterprisesService;
    }

    @PostMapping
    public ResponseBase createEnterprise(@RequestBody RequestEnterprises requestEnterprises){
        ResponseBase responseBase = enterprisesService.createEnterprise(requestEnterprises);
        return responseBase;
    }
    @GetMapping("/{doc}")
    public ResponseBase findOne(@PathVariable String doc){
        ResponseBase responseBase = enterprisesService.findOneEnterprise(doc);
        return responseBase;
    }
    @GetMapping()
    public ResponseBase findAll(){
        ResponseBase responseBase = enterprisesService.findAllEnterprises();
        return responseBase;
    }
    @PatchMapping("/{id}")
    public ResponseBase updateEnterprises(@PathVariable int id, @RequestBody RequestEnterprises requestEnterprises){
        ResponseBase responseBase = enterprisesService.updateEnterprise(id,requestEnterprises);
        return responseBase;
    }
    @DeleteMapping("/{id}")
    public ResponseBase deleteEnterprises(@PathVariable int id){
        ResponseBase responseBase = enterprisesService.delete(id);
        return responseBase;
    }
}
