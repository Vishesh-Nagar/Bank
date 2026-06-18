package com.example.bank.service;

import com.example.bank.dto.ApplyPaymentRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "account-service", url = "${account-service.url}")
public interface AccountServiceClient {

    @PostMapping("/internal/accounts/apply-payment")
    void applyPayment(@RequestBody ApplyPaymentRequest request,
                      @RequestHeader("X-Internal-Auth") String internalSecret);

    @GetMapping("/internal/accounts/{id}/owner")
    Boolean isAccountOwner(@PathVariable("id") Long id,
                           @RequestParam("username") String username,
                           @RequestHeader("X-Internal-Auth") String internalSecret);
}
