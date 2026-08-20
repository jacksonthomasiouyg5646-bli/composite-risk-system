package com.example.usermanagement.system;

import com.example.usermanagement.common.api.ApiResponse;
import com.example.usermanagement.common.security.RequirePermission;
import com.example.usermanagement.system.service.CustomerRiskChatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequirePermission("risk:manage")
@RestController
@RequestMapping("/risks/ai-chat")
public class CustomerRiskChatController {
    private final CustomerRiskChatService customerRiskChatService;

    public CustomerRiskChatController(CustomerRiskChatService customerRiskChatService) {
        this.customerRiskChatService = customerRiskChatService;
    }

    @PostMapping("/customer")
    public ApiResponse<Map<String, Object>> ask(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(customerRiskChatService.ask(body));
    }
}
