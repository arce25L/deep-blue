package com.deepblue.rescue;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.Expertise;
import com.deepblue.rescue.domain.MedicalRecord;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueCenter;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.ExpertiseRepository;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.repository.RescueCenterRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("deepblue_test")
                    .withUsername("deepblue")
                    .withPassword("deepblue");

    @Autowired
    private RescueCenterRepository rescueCenterRepository;

    @Autowired
    private RescueCaseRepository rescueCaseRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private SpecialistRepository specialistRepository;

    @Autowired
    private ExpertiseRepository expertiseRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ---------------------------------------------------------------
    // PARTE IX — Paso 47: Test de Flyway
    // ---------------------------------------------------------------

    @Test
    void flywayShouldHaveExecutedV1AndV2() {
        List<String> versions = jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success = true order by installed_rank",
                String.class);

        assertThat(versions).contains("1", "2");
    }

    // ---------------------------------------------------------------
    // Paso 48: Test de métodos heredados
    // ---------------------------------------------------------------

    @Test
    void inheritedMethodsShouldWorkOnRescueCenter() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");

        RescueCenter saved = rescueCenterRepository.save(center);
        assertThat(saved.getId()).isNotNull();

        Optional<RescueCenter> found = rescueCenterRepository.findById(saved.getId());
        assertThat(found).isPresent();

        boolean exists = rescueCenterRepository.existsById(saved.getId());
        assertThat(exists).isTrue();

        long count = rescueCenterRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    // ---------------------------------------------------------------
    // Paso 49: Test relación 1:N (RescueCenter -> RescueCase)
    // ---------------------------------------------------------------

    @Test
    void oneCenterShouldHaveManyCases() {
        RescueCenter center = new RescueCenter("DB-ONE", "DeepBlue One", "Santa Marta");

        RescueCase case1 = new RescueCase("RES-ONE-1", LocalDate.of(2026, 1, 10),
                "Playa Uno", RescueStatus.ADMITTED);
        RescueCase case2 = new RescueCase("RES-ONE-2", LocalDate.of(2026, 1, 12),
                "Playa Dos", RescueStatus.ADMITTED);

        center.addCase(case1);
        center.addCase(case2);

        rescueCenterRepository.save(center);

        List<RescueCase> cases = rescueCaseRepository.findByRescueCenterCode("DB-ONE");

        assertThat(cases).hasSize(2);
        assertThat(cases).allMatch(c -> c.getRescueCenter().getCode().equals("DB-ONE"));
    }

    // ---------------------------------------------------------------
    // Paso 50: Test RescueCase 1:1 Animal
    // ---------------------------------------------------------------

    @Test
    void rescueCaseShouldHaveOneAnimal() {
        RescueCenter center = new RescueCenter("DB-TUR", "DeepBlue Turtle Center", "Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase("RES-2026-001", LocalDate.of(2026, 3, 1),
                "Bahia Concha", RescueStatus.ADMITTED);
        center.addCase(rescueCase);

        Animal animal = new Animal("AN-2026-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);

        rescueCaseRepository.save(rescueCase);

        RescueCase reloaded = rescueCaseRepository.findByCaseCode("RES-2026-001").orElseThrow();

        assertThat(reloaded.getAnimal()).isNotNull();
        assertThat(reloaded.getAnimal().getAnimalCode()).isEqualTo("AN-2026-001");
        assertThat(reloaded.getAnimal().getRescueCase().getCaseCode()).isEqualTo("RES-2026-001");
    }

    // ---------------------------------------------------------------
    // Paso 51: Test Animal 1:1 MedicalRecord
    // ---------------------------------------------------------------

    @Test
    void animalShouldHaveOneMedicalRecordViaCascade() {
        RescueCenter center = new RescueCenter("DB-MED", "DeepBlue Medical Center", "Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase("RES-MED-1", LocalDate.of(2026, 3, 5),
                "Taganga", RescueStatus.ADMITTED);
        center.addCase(rescueCase);

        Animal animal = new Animal("AN-2026-002", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);

        MedicalRecord record = new MedicalRecord(
                new BigDecimal("28.40"), "STABLE", "Left front flipper injury", null);
        animal.assignMedicalRecord(record);

        rescueCaseRepository.save(rescueCase);
        rescueCaseRepository.flush();

        assertThat(animal.getId()).isNotNull();
        assertThat(record.getId()).isNotNull();
    }

    // ---------------------------------------------------------------
    // Paso 52: Test N:M (Specialist <-> Expertise)
    // ---------------------------------------------------------------

    @Test
    void specialistShouldHaveMultipleExpertiseAreas() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

        Specialist elena = new Specialist("SPEC-N-1", "Elena", "Vargas", "elena.nm@deepblue.org");
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);

        specialistRepository.save(elena);
        specialistRepository.flush();

        Specialist reloaded = specialistRepository.findById(elena.getId()).orElseThrow();
        assertThat(reloaded.getExpertiseAreas()).hasSize(2);
    }

    // ---------------------------------------------------------------
    // Paso 53: Test Query Method simple (casos por status)
    // ---------------------------------------------------------------

    @Test
    void findByStatusShouldReturnOnlyMatchingCases() {
        RescueCenter center = new RescueCenter("DB-QS", "DeepBlue QS", "Santa Marta");
        rescueCenterRepository.save(center);

        center.addCase(new RescueCase("RES-001-QS", LocalDate.of(2026, 4, 1),
                "Loc 1", RescueStatus.IN_REHABILITATION));
        center.addCase(new RescueCase("RES-002-QS", LocalDate.of(2026, 4, 2),
                "Loc 2", RescueStatus.READY_FOR_RELEASE));
        center.addCase(new RescueCase("RES-003-QS", LocalDate.of(2026, 4, 3),
                "Loc 3", RescueStatus.IN_REHABILITATION));

        rescueCenterRepository.save(center);

        List<RescueCase> inRehab = rescueCaseRepository
                .findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);

        assertThat(inRehab).hasSize(2);
    }

    // ---------------------------------------------------------------
    // Paso 54: Test Query Method navegando relaciones (animales por centro)
    // ---------------------------------------------------------------

    @Test
    void findAnimalsByCenterCodeShouldNotReturnAnimalsFromOtherCenters() {
        RescueCenter car = new RescueCenter("DB-CAR-2", "DeepBlue Caribbean", "Santa Marta");
        RescueCenter pac = new RescueCenter("DB-PAC-2", "DeepBlue Pacific", "Buenaventura");
        rescueCenterRepository.save(car);
        rescueCenterRepository.save(pac);

        RescueCase caseCar = new RescueCase("RES-CAR-1", LocalDate.of(2026, 5, 1),
                "Bahia Concha", RescueStatus.ADMITTED);
        car.addCase(caseCar);
        Animal animalCar = new Animal("AN-CAR-1", "Green Sea Turtle", "Chelonia mydas", AnimalSex.MALE);
        caseCar.assignAnimal(animalCar);

        RescueCase casePac = new RescueCase("RES-PAC-1", LocalDate.of(2026, 5, 2),
                "Bahia Malaga", RescueStatus.ADMITTED);
        pac.addCase(casePac);
        Animal animalPac = new Animal("AN-PAC-1", "Olive Ridley Turtle", "Lepidochelys olivacea", AnimalSex.FEMALE);
        casePac.assignAnimal(animalPac);

        rescueCenterRepository.save(car);
        rescueCenterRepository.save(pac);

        List<Animal> animalsFromCar = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR-2");

        assertThat(animalsFromCar).hasSize(1);
        assertThat(animalsFromCar.get(0).getAnimalCode()).isEqualTo("AN-CAR-1");
    }

    // ---------------------------------------------------------------
    // Paso 55: Test JPQL de especialistas por expertise
    // ---------------------------------------------------------------

    @Test
    void findActiveByExpertiseShouldReturnMatchingSpecialists() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();
        Expertise marineMammals = expertiseRepository.findByNameIgnoreCase("Marine Mammals").orElseThrow();
        Expertise marineBirds = expertiseRepository.findByNameIgnoreCase("Marine Birds").orElseThrow();

        Specialist elena = new Specialist("SPEC-E-1", "Elena", "Vargas", "elena.e1@deepblue.org");
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);

        Specialist mateo = new Specialist("SPEC-E-2", "Mateo", "Rios", "mateo.e2@deepblue.org");
        mateo.addExpertise(marineMammals);
        mateo.addExpertise(rehabilitation);

        Specialist sofia = new Specialist("SPEC-E-3", "Sofia", "Diaz", "sofia.e3@deepblue.org");
        sofia.addExpertise(marineBirds);
        sofia.addExpertise(trauma);

        specialistRepository.save(elena);
        specialistRepository.save(mateo);
        specialistRepository.save(sofia);

        List<Specialist> traumaSpecialists = specialistRepository.findActiveByExpertise("Trauma");

        assertThat(traumaSpecialists)
                .extracting(Specialist::getFirstName)
                .containsExactlyInAnyOrder("Elena", "Sofia");
    }

    // ---------------------------------------------------------------
    // Paso 57: Test Query Method de tratamientos ordenados cronológicamente
    // ---------------------------------------------------------------

    @Test
    void treatmentsShouldBeOrderedChronologically() {
        RescueCenter center = new RescueCenter("DB-TR", "DeepBlue Treatments", "Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase("RES-TR-1", LocalDate.of(2026, 6, 1),
                "Loc TR", RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);

        Animal animal = new Animal("AN-TR-1", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        rescueCenterRepository.save(center);

        Specialist elena = new Specialist("SPEC-TR-1", "Elena", "Vargas", "elena.tr1@deepblue.org");
        Specialist mateo = new Specialist("SPEC-TR-2", "Mateo", "Rios", "mateo.tr2@deepblue.org");
        specialistRepository.save(elena);
        specialistRepository.save(mateo);

        Treatment t1 = new Treatment(animal, elena,
                LocalDateTime.of(2026, 6, 1, 9, 0), TreatmentType.WOUND_CARE, "Treatment 1");
        Treatment t2 = new Treatment(animal, elena,
                LocalDateTime.of(2026, 6, 2, 9, 0), TreatmentType.HYDRATION, "Treatment 2");
        Treatment t3 = new Treatment(animal, mateo,
                LocalDateTime.of(2026, 6, 3, 9, 0), TreatmentType.OBSERVATION, "Treatment 3");

        treatmentRepository.save(t1);
        treatmentRepository.save(t2);
        treatmentRepository.save(t3);

        List<Treatment> treatments = treatmentRepository
                .findByAnimalIdOrderByPerformedAtAsc(animal.getId());

        assertThat(treatments).extracting(Treatment::getDescription)
                .containsExactly("Treatment 1", "Treatment 2", "Treatment 3");
    }

    // ---------------------------------------------------------------
    // Paso 58: Test JPQL por intervalo de fechas
    // ---------------------------------------------------------------

    @Test
    void treatmentsBetweenDatesShouldReturnOnlyThoseWithinRange() {
        RescueCenter center = new RescueCenter("DB-INT", "DeepBlue Interval", "Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase("RES-INT-1", LocalDate.of(2026, 8, 1),
                "Loc INT", RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);

        Animal animal = new Animal("AN-INT-1", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        rescueCenterRepository.save(center);

        Specialist elena = new Specialist("SPEC-INT-1", "Elena", "Vargas", "elena.int1@deepblue.org");
        specialistRepository.save(elena);

        treatmentRepository.save(new Treatment(animal, elena,
                LocalDateTime.of(2026, 8, 1, 10, 0), TreatmentType.WOUND_CARE, "Early"));
        treatmentRepository.save(new Treatment(animal, elena,
                LocalDateTime.of(2026, 8, 10, 10, 0), TreatmentType.HYDRATION, "Middle"));
        treatmentRepository.save(new Treatment(animal, elena,
                LocalDateTime.of(2026, 8, 20, 10, 0), TreatmentType.OBSERVATION, "Late"));

        List<Treatment> inRange = treatmentRepository.findBetweenDates(
                LocalDateTime.of(2026, 8, 5, 0, 0),
                LocalDateTime.of(2026, 8, 15, 0, 0));

        assertThat(inRange).hasSize(1);
        assertThat(inRange.get(0).getDescription()).isEqualTo("Middle");
    }

    // ---------------------------------------------------------------
    // PARTE X — Paso 59: Probar UNIQUE
    // ---------------------------------------------------------------

    @Test
    void savingDuplicateAnimalCodeShouldViolateUniqueConstraint() {
        RescueCenter center = new RescueCenter("DB-UQ", "DeepBlue Unique", "Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase case1 = new RescueCase("RES-UQ-1", LocalDate.of(2026, 7, 1),
                "Loc UQ 1", RescueStatus.ADMITTED);
        RescueCase case2 = new RescueCase("RES-UQ-2", LocalDate.of(2026, 7, 2),
                "Loc UQ 2", RescueStatus.ADMITTED);
        center.addCase(case1);
        center.addCase(case2);
        rescueCenterRepository.save(center);

        Animal animal1 = new Animal("AN-100", "Green Sea Turtle", "Chelonia mydas", AnimalSex.MALE);
        case1.assignAnimal(animal1);
        animalRepository.saveAndFlush(animal1);

        Animal animal2 = new Animal("AN-100", "Loggerhead Turtle", "Caretta caretta", AnimalSex.FEMALE);
        case2.assignAnimal(animal2);

        assertThatThrownBy(() -> animalRepository.saveAndFlush(animal2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------------------------------------------------------------
    // PARTE XII — Reto integrador (Paso 65-67) + PARTE XIII (reto sin guía)
    // ---------------------------------------------------------------

    @Test
    void integratorChallengeScenario() {
        // Centro
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta");
        rescueCenterRepository.save(center);

        // Caso
        RescueCase rescueCase = new RescueCase("RES-2026-100", LocalDate.of(2026, 8, 18),
                "Bahia Concha", RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);

        // Animal
        Animal animal = new Animal("AN-2026-100", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);

        // Expediente medico
        MedicalRecord record = new MedicalRecord(new BigDecimal("27.80"), "STABLE",
                "Injury caused by fishing net", "Possible plastic ingestion");
        animal.assignMedicalRecord(record);

        rescueCenterRepository.save(center);

        // Especialista
        Expertise marineReptiles = expertiseRepository.findByNameIgnoreCase("Marine Reptiles").orElseThrow();
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

        Specialist elena = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org");
        elena.addExpertise(marineReptiles);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);
        specialistRepository.save(elena);

        // Tratamientos
        Treatment t1 = new Treatment(animal, elena, LocalDateTime.of(2026, 8, 19, 9, 0),
                TreatmentType.WOUND_CARE, "Cleaning of left front flipper");
        Treatment t2 = new Treatment(animal, elena, LocalDateTime.of(2026, 8, 20, 9, 0),
                TreatmentType.HYDRATION, "Subcutaneous fluid therapy");
        treatmentRepository.save(t1);
        treatmentRepository.save(t2);

        // Consulta 1: existe el caso?
        assertThat(rescueCaseRepository.existsByCaseCode("RES-2026-100")).isTrue();

        // Consulta 2: casos IN_REHABILITATION
        assertThat(rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION))
                .extracting(RescueCase::getCaseCode)
                .contains("RES-2026-100");

        // Consulta 3: animales de DB-CAR
        assertThat(animalRepository.findByRescueCaseRescueCenterCode("DB-CAR"))
                .extracting(Animal::getAnimalCode)
                .contains("AN-2026-100");

        // Consulta 4: animales cuyo nombre contenga "turtle"
        assertThat(animalRepository.findByCommonNameContainingIgnoreCase("turtle"))
                .extracting(Animal::getAnimalCode)
                .contains("AN-2026-100");

        // Consulta 5: especialistas con experiencia en Trauma
        assertThat(specialistRepository.findActiveByExpertise("Trauma"))
                .extracting(Specialist::getProfessionalCode)
                .contains("SPEC-001");

        // Consulta 6: tratamientos del animal ordenados cronológicamente
        assertThat(treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId()))
                .extracting(Treatment::getDescription)
                .containsExactly("Cleaning of left front flipper", "Subcutaneous fluid therapy");

        // Consulta 7: tratamientos por especialistas con experiencia en Rehabilitation
        assertThat(treatmentRepository.findBySpecialistExpertise("Rehabilitation"))
                .extracting(Treatment::getDescription)
                .contains("Cleaning of left front flipper", "Subcutaneous fluid therapy");

        // Consulta 8: tratamientos realizados entre dos fechas
        assertThat(treatmentRepository.findBetweenDates(
                LocalDateTime.of(2026, 8, 18, 0, 0),
                LocalDateTime.of(2026, 8, 19, 23, 59)))
                .extracting(Treatment::getDescription)
                .containsExactly("Cleaning of left front flipper");

        // PARTE XIII — Reto sin guía:
        // animales en rehabilitación que recibieron al menos un tratamiento
        // realizado por un especialista con experiencia en Trauma
        List<Animal> result = animalRepository.findInRehabilitationTreatedBySpecialistWithExpertise(
                RescueStatus.IN_REHABILITATION, "trauma");

        assertThat(result)
                .extracting(Animal::getAnimalCode)
                .contains("AN-2026-100");
    }
}