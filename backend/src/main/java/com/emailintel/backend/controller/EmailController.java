package com.emailintel.backend.controller;

import com.emailintel.backend.model.Email;
import com.emailintel.backend.service.EmailService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController
@RequestMapping("/api/emails")
@CrossOrigin
public class EmailController {
    private final EmailService service;
    public EmailController(EmailService service){this.service=service;}
    @GetMapping public List<Email> all(){return service.all();}
    @GetMapping("/{id}") public Email one(@PathVariable String id){return service.one(id).orElseThrow();}
    @GetMapping("/search") public List<Email> search(@RequestParam String q){return service.search(q);}
    @GetMapping("/{id}/similar") public List<Email> similar(@PathVariable String id){return service.similar(id);}
    @GetMapping("/stats") public Map<String,Object> stats(){return service.stats();}
    @GetMapping("/network") public Map<String,Object> network(@RequestParam String email){return service.network(email);}
    @PostMapping("/demo") public Email demo(@RequestBody Map<String,String> body){return service.addEmail(body.getOrDefault("sender","demo@example.com"),body.getOrDefault("recipient","me@example.com"),body.getOrDefault("subject","Demo Email"),body.getOrDefault("body","This is a demo email."));}
    @PostMapping(value="/upload",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Email upload(@RequestParam MultipartFile file)throws Exception{return service.upload(file);}
}
