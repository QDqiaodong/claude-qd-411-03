package com.orchard.grove.controller;

import com.orchard.grove.model.Inventory;
import com.orchard.grove.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    @Autowired
    InventoryService service;

    @GetMapping
    public List<Inventory> list() {
        return service.list();
    }

    @PostMapping("/outbound")
    public Inventory outbound(@RequestBody Map<String, Object> body) {
        String variety = (String) body.get("variety");
        Double kg = ((Number) body.get("kg")).doubleValue();
        return service.outbound(variety, kg);
    }

    @PostMapping("/warn-line")
    public Inventory warnLine(@RequestBody Map<String, Object> body) {
        String variety = (String) body.get("variety");
        Double warnLine = body.get("warnLine") == null ? null : ((Number) body.get("warnLine")).doubleValue();
        return service.setWarnLine(variety, warnLine);
    }
}
