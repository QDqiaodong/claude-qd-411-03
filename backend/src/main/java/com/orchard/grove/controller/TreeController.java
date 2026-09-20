package com.orchard.grove.controller;

import com.orchard.grove.model.Tree;
import com.orchard.grove.model.TreeRemoval;
import com.orchard.grove.service.TreeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trees")
public class TreeController {
    @Autowired
    TreeService service;

    @GetMapping
    public List<Tree> list() {
        return service.list();
    }

    @PostMapping
    public Tree create(@RequestBody Tree f) {
        return service.create(f);
    }

    @PutMapping("/{id}")
    public Tree update(@PathVariable Long id, @RequestBody Tree f) {
        return service.update(id, f);
    }

    @PostMapping("/{id}/removals")
    public TreeRemoval clear(@PathVariable Long id, @RequestBody(required = false) TreeRemoval f) {
        return service.clear(id, f);
    }

    @PostMapping("/{id}/removals/withdraw")
    public TreeRemoval withdraw(@PathVariable Long id) {
        return service.withdraw(id);
    }
}
