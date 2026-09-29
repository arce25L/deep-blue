package com.deepblue.rescue.mapper;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.dto.response.AnimalResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AnimalMapper {

    // sex (enum AnimalSex) -> String: MapStruct lo convierte automáticamente
    @Mapping(target = "caseCode", source = "rescueCase.caseCode")
    @Mapping(target = "rescueStatus", source = "rescueCase.status")
    AnimalResponse toResponse(Animal animal);
}
