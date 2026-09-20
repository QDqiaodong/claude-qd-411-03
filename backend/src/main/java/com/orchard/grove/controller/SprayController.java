package com.orchard.grove.controller;

import com.orchard.grove.model.SprayRecord;
import com.orchard.grove.service.SprayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sprays")
public class SprayController {
    @Autowired
    SprayService service;

    @GetMapping
    public List<SprayRecord> list(@RequestParam(required = false) Long plotId) {
        return service.list(plotId);
    }

    @PostMapping
    public SprayRecord create(@RequestBody SprayRecord f) {
        return service.create(f);
    }

    @PutMapping("/{id}/void")
    public SprayRecord voidRecord(@PathVariable Long id) {
        return service.voidRecord(id);
    }
}
