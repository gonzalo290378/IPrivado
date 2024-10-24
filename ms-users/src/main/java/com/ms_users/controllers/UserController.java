package com.ms_users.controllers;

import com.ms_users.dto.FilterDTO;
import com.ms_users.dto.UserDTO;
import com.ms_users.models.entity.User;
import com.ms_users.services.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@Slf4j
@RequestMapping("api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<UserDTO>> findAll() {
        log.info("Calling findAll");
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        log.info("Calling findById with {}", id);
        return ResponseEntity.ok(userService.findById(id));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<?> findByEmail(@PathVariable String email) {
        log.info("Calling findByEmail with {email}");
        Optional<UserDTO> userOptional = userService.findByEmail(email);
        if (userOptional.isPresent()) {
            return ResponseEntity.ok(userOptional.get());
        }
        return ResponseEntity.notFound().build();
    }

    //TODO HACER LOGICA PARA QUE HAGAN MATCH LAS EDADES ENTRE USUARIOS
    @GetMapping("/filter")
    public ResponseEntity<Page<FilterDTO>> filter(
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @RequestParam(name = "ageFrom", required = true) Long ageFrom,
            @RequestParam(name = "ageTo", required = true) Long ageTo,
            @RequestParam(name = "sex", required = false) String sex,
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "country", required = false) String country,
            @RequestParam(name = "isEnabled", required = false) Boolean isEnabled) {

        FilterDTO filterDTO = FilterDTO.builder()
                .ageFrom(ageFrom)
                .ageTo(ageTo)
                .sex(sex)
                .city(city)
                .country(country)
                .isEnabled(isEnabled)
                .build();
        log.info("Calling filter with {}", filterDTO);
        return ResponseEntity.ok(userService.filter(filterDTO, page, size));
    }


    @PostMapping
    public ResponseEntity<?> save(@Valid @RequestBody User user) {
        log.info("Calling save with {}", user);
        return ResponseEntity.ok(userService.save(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> edit(@Valid @RequestBody User user, BindingResult result, @PathVariable Long id) {
        log.info("Calling edit with {user}");

        if (result.hasErrors()) {
            return userService.validate(result);
        }

        Optional<UserDTO> o = userService.findById(id);
        if (o.isPresent()) {
            UserDTO usuarioDb = o.get();

            if (userService.hasInvalidFields(user, usuarioDb)) {
                return ResponseEntity.badRequest()
                        .body(Collections.singletonMap("Message Application", "Some data cannot be empty"));
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(userService.save(user));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Optional<UserDTO> o = userService.findById(id);
        if (o.isPresent()) {
            userService.delete(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

}
