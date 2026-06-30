package com.company.dgp.common.web;

import com.company.dgp.common.result.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> ping() {
        return ApiResponse.success(Map.of(
                "status", "ok",
                "time", OffsetDateTime.now().toString()
        ));
    }
}
