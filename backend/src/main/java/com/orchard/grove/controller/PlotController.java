package com.orchard.grove.controller;

import com.orchard.grove.model.Plot;
import com.orchard.grove.service.PlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plots")
public class PlotController {
    @Autowired
    PlotService service;

    @GetMapping
    public List<Plot> list() {
        return service.list();
    }

    @PostMapping
    public Plot create(@RequestBody Plot f) {
        return service.create(f);
    }

    @PutMapping("/{id}")
    public Plot update(@PathVariable Long id, @RequestBody Plot f) {
        return service.update(id, f);
    }
}
