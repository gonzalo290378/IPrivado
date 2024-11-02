package com.iprivado.free_area.services;

import com.iprivado.free_area.clients.UserClientRest;
import com.iprivado.free_area.dto.FreeAreaDTO;
import com.iprivado.free_area.exceptions.FreeAreaNotFoundException;
import com.iprivado.free_area.mapper.FreeAreaMapper;
import com.iprivado.free_area.models.entity.FreeArea;
import com.iprivado.free_area.repositories.FreeAreaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FreeAreaServiceImpl implements FreeAreaService {

    private FreeAreaRepository freeAreaRepository;

    private UserClientRest userClientRest;

    private FreeAreaMapper freeAreaMapper;

    public FreeAreaServiceImpl(FreeAreaRepository freeAreaRepository, UserClientRest userClientRest, FreeAreaMapper freeAreaMapper) {
        this.freeAreaRepository = freeAreaRepository;
        this.userClientRest = userClientRest;
        this.freeAreaMapper = freeAreaMapper;
    }

    @Override
    public List<FreeAreaDTO> findAll() {
        List<FreeArea> freeArea = freeAreaRepository.findAll();
        return freeArea.stream()
                .map(freeAreaMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<FreeAreaDTO> findById(Long id) {
        return Optional.ofNullable(freeAreaRepository.findAll()
                .stream()
                .filter(e -> Objects.equals(e.getId(), id))
                .map(freeAreaMapper::toDTO)
                .findFirst().orElseThrow(() ->
                        new FreeAreaNotFoundException("id: " + id + " does not exist")));
    }


    @Transactional
    public FreeArea save(Boolean isEnabled) {
        FreeArea freeArea = FreeArea.builder()
                .isEnabled(isEnabled)
                .principalPhoto(new ArrayList<>())
                .publicContent(new ArrayList<>())
                .build();
        freeAreaRepository.save(freeArea);
        return freeArea;
    }


}
