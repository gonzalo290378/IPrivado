package com.ms_users.services;

import com.ms_users.clients.FreeAreaClientRest;
import com.ms_users.clients.PrivateAreaClientRest;
import com.ms_users.dto.FilterDTO;
import com.ms_users.dto.UserDTO;
import com.ms_users.dto.UserFormDTO;
import com.ms_users.enums.AgeConfiguration;
import com.ms_users.enums.AreaConfiguration;
import com.ms_users.enums.DateConfiguration;
import com.ms_users.enums.UserEnabledConfiguration;
import com.ms_users.exceptions.*;
import com.ms_users.mapper.FilterMapper;
import com.ms_users.mapper.UserMapper;
import com.ms_users.models.FreeAreaDTO;
import com.ms_users.models.PrivateAreaDTO;
import com.ms_users.models.entity.City;
import com.ms_users.models.entity.Country;
import com.ms_users.models.entity.Preference;
import com.ms_users.models.entity.User;
import com.ms_users.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final FilterMapper filterMapper;
    private final FreeAreaClientRest freeAreaClientRest;
    private final PrivateAreaClientRest privateAreaClientRest;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, FilterMapper filterMapper, FreeAreaClientRest client, PrivateAreaClientRest privateAreaClientRest) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.filterMapper = filterMapper;
        this.freeAreaClientRest = client;
        this.privateAreaClientRest = privateAreaClientRest;
    }

    @Transactional(readOnly = true)
    public List<UserDTO> findAll() {
        List<User> userList = userRepository.findAll();
        List<FreeAreaDTO> freeAreaList = freeAreaClientRest.findAll();
        List<PrivateAreaDTO> privateAreaList = privateAreaClientRest.findAll();
        List<UserDTO> userDTOList = userList.stream().map(userMapper::toDTO).toList();
        return matchUserWithFreeAreaAndPrivateArea(userDTOList, freeAreaList, privateAreaList);
    }

    private List<UserDTO> matchUserWithFreeAreaAndPrivateArea(List<UserDTO> userDTOList, List<FreeAreaDTO> freeAreaDTOList, List<PrivateAreaDTO> privateAreaDTOList
    ) {
        for (UserDTO user : userDTOList) {
            freeAreaDTOList.stream()
                    .filter(freeArea -> Objects.equals(freeArea.getId(), user.getIdFreeArea()))
                    .peek(user::setFreeAreaDTO)
                    .collect(Collectors.toList());

            privateAreaDTOList.stream()
                    .filter(privateArea -> Objects.equals(privateArea.getId(), user.getIdPrivateArea()))
                    .peek(user::setPrivateAreaDTO)
                    .collect(Collectors.toList());
        }
        return userDTOList;
    }


    @Transactional(readOnly = true)
    public Optional<UserDTO> findById(Long id) {
        User user = userRepository.findAll().stream().filter(e -> Objects.equals(e.getId(), id))
                .findFirst()
                .orElseThrow(() -> new IdNotFoundException("id: " + id + " does not exist"));
        return getUserDTO(user);
    }

    public Optional<UserDTO> findByEmail(String email) {
        User user = userRepository.findAll().stream().filter(e -> Objects.equals(e.getEmail(), email))
                .findFirst()
                .orElseThrow(() -> new EmailNotFoundException("email: " + email + " does not exist"));
        return getUserDTO(user);
    }

    private Optional<UserDTO> getUserDTO(User user) {
        FreeAreaDTO freeAreaDTO = freeAreaClientRest.findById(user.getIdFreeArea());
        PrivateAreaDTO privateAreaDTO = privateAreaClientRest.findById(user.getIdPrivateArea());
        UserDTO userDTO = userMapper.toDTO(user);
        userDTO.setFreeAreaDTO(freeAreaDTO);
        userDTO.setPrivateAreaDTO(privateAreaDTO);
        return Optional.of(userDTO);
    }

    @Transactional(readOnly = true)
    public Page<FilterDTO> filter(FilterDTO filterDTO, Integer page, Integer size) {
        validateUserAgeSelected(filterDTO);
        return getFilteredUsers(filterDTO, page, size);
    }

    private Page<FilterDTO> getFilteredUsers(FilterDTO filterDTO, Integer page, Integer size) {
        Page<User> filterUserList = userRepository.filter(filterDTO, PageRequest.of(page, size));

        return filterUserList.map(user -> {
            FilterDTO filterListDTO = filterMapper.toDTO(user);
            FreeAreaDTO freeAreaDTO = freeAreaClientRest.findById(user.getIdFreeArea());
            PrivateAreaDTO privateAreaDTO = privateAreaClientRest.findById(user.getIdPrivateArea());
            filterListDTO.setFreeAreaDTO(freeAreaDTO);
            filterListDTO.setPrivateAreaDTO(privateAreaDTO);
            filterListDTO.setIdFreeArea(freeAreaDTO.getId());
            return filterListDTO;
        });
    }

    private void validateUserAgeSelected(FilterDTO filterDTO) {
        Long ageFrom = filterDTO.getPreferenceDTO().getAgeFrom();
        Long ageTo = filterDTO.getPreferenceDTO().getAgeTo();

        Boolean isAgeFromTooLow = ageFrom < AgeConfiguration.ADULT.getValue();
        Boolean isAgeRangeInvalid = ageFrom > ageTo;
        Boolean isAgeToTooHigh = ageTo > AgeConfiguration.SENIOR.getValue();

        if (isAgeFromTooLow || isAgeRangeInvalid || isAgeToTooHigh) {
            throw new UserDisabledNotFoundException("User: " + filterDTO.getUsername()
                    + " has selected an invalid age range: ageFrom = " + ageFrom + ", ageTo = " + ageTo);
        }
    }

    public User save(UserFormDTO userFormDTO) {
        validateUserForm(userFormDTO);
        User newUser = buildUser(userFormDTO);
        return userRepository.save(newUser);
    }

    private void validateUserForm(UserFormDTO userFormDTO) {
        validateAgeFromAndAgeTo(userFormDTO.getAgeFrom(), userFormDTO.getAgeTo());
        validateBirthdate(userFormDTO.getBirthdate());
    }

    private void validateAgeFromAndAgeTo(Long ageFrom, Long ageTo) {
        if (ageFrom != null && ageTo != null && ageFrom > ageTo) {
            throw new InvalidAgeRangeException("Age from cannot be greater than age to");
        }
    }

    private void validateBirthdate(LocalDate birthdate) {
        if (!isAdult(birthdate)) {
            throw new InvalidBirthdateException("Age must be at least 18 years old.");
        }
    }

    private User buildUser(UserFormDTO userFormDTO) {
        FreeAreaDTO newFreeAreaDTO = createFreeArea();
        PrivateAreaDTO newPrivateArea = createPrivateArea();
        Preference preference = buildPreference(userFormDTO);
        Country country = buildCountry(userFormDTO);
        City city = buildCity(userFormDTO);
        return buildUser(userFormDTO, newFreeAreaDTO, newPrivateArea, preference, country, city);
    }

    private boolean isAdult(LocalDate birthdate) {
        return birthdate != null && ChronoUnit.YEARS.between(birthdate, LocalDate.now()) >= 18;
    }

    private FreeAreaDTO createFreeArea() {
        return freeAreaClientRest.save(AreaConfiguration.ENABLED.getValue());
    }

    private PrivateAreaDTO createPrivateArea() {
        return privateAreaClientRest.save(AreaConfiguration.DISABLED.getValue());
    }

    private Preference buildPreference(UserFormDTO userFormDTO) {
        return Preference.builder()
                .ageFrom(userFormDTO.getAgeFrom())
                .ageTo(userFormDTO.getAgeTo())
                .sexPreference(userFormDTO.getSexPreference())
                .build();
    }

    private Country buildCountry(UserFormDTO userFormDTO) {
        return Country.builder()
                .country(userFormDTO.getCountry())
                .build();
    }

    private City buildCity(UserFormDTO userFormDTO) {
        return City.builder()
                .city(userFormDTO.getCity())
                .build();
    }

    private User buildUser(UserFormDTO userFormDTO, FreeAreaDTO freeAreaDTO, PrivateAreaDTO privateAreaDTO,
                           Preference preference, Country country, City city) {
        return User.builder()
                .idFreeArea(freeAreaDTO.getId())
                .idPrivateArea(privateAreaDTO.getId())
                .preference(preference)
                .country(country)
                .city(city)
                .username(userFormDTO.getUsername())
                .age(userFormDTO.getAge())
                .sex(userFormDTO.getSex())
                .email(userFormDTO.getEmail())
                .birthdate(userFormDTO.getBirthdate())
                .registerDate(DateConfiguration.TODAY.getValue())
                .description(userFormDTO.getDescription())
                .isEnabled(UserEnabledConfiguration.IS_ENABLED.getValue())
                .password(userFormDTO.getPassword())
                .build();
    }


    @Transactional
    public void delete(Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            userRepository.delete(id);
            freeAreaClientRest.delete(id);
        }
    }

    public ResponseEntity<Map<String, String>> validate(BindingResult result) {
        Map<String, String> errores = new HashMap<>();
        result.getFieldErrors().forEach(err -> {
            errores.put(err.getField(), "The field " + err.getField() + " " + err.getDefaultMessage());
        });
        return ResponseEntity.badRequest().body(errores);
    }

    //TODO FALTA INSERTAR LAS FOTOS (O CONTENIDO)
    public Boolean hasInvalidFields(User user, UserDTO usuarioDb) {
        return !user.getIdFreeArea().equals(usuarioDb.getFreeAreaDTO().getId()) ||
                //!user.getIdContent().equals(usuarioDb.getIdContent()) ||
                isEmpty(user.getUsername()) || isEmpty(user.getEmail()) ||
                //isEmpty(user.getBirthdate()) ||
                isEmpty(user.getCity().getCity()) || isEmpty(user.getCountry().getCountry()) || isEmpty(user.getPassword());
    }

    private Boolean isEmpty(String field) {
        return field == null || field.isEmpty();
    }

}
