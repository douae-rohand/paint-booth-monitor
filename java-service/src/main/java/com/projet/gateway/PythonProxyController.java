package com.projet.gateway;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/python-proxy")
public class PythonProxyController {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final String pythonServiceUrl = "http://localhost:8000";

    @PostMapping("/**")
    public ResponseEntity<Object> proxyPost(@RequestBody Object body) {
        // Relais REST vers Python (IA/RAG)
        return ResponseEntity.ok().build();
    }

    @GetMapping("/**")
    public ResponseEntity<Object> proxyGet() {
        return ResponseEntity.ok().build();
    }
}
