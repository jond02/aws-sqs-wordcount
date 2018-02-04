package com.jdann.aws.sqsconsumer.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/")
public class IndexRestController {

    @GetMapping(value = "consumer", produces = "application/json")
    public ResponseEntity<String> consumer() {
        return new ResponseEntity<>("Running", HttpStatus.OK);
    }
}
