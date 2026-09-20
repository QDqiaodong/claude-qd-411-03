package com.orchard.grove.controller;

import com.orchard.grove.model.HarvestBatch;
import com.orchard.grove.service.HarvestBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/harvest-batches")
public class HarvestBatchController {
    @Autowired
    HarvestBatchService service;

    @GetMapping
    public List<HarvestBatch> list() {
        return service.list();
    }

    @PostMapping
    public HarvestBatch create(@RequestBody HarvestBatch f) {
        return service.create(f);
    }

    @PutMapping("/{id}")
    public HarvestBatch update(@PathVariable Long id, @RequestBody HarvestBatch f) {
        return service.update(id, f);
    }
}
