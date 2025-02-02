package leonardo.labutilities.qualitylabpro.services;

import leonardo.labutilities.qualitylabpro.dtos.analytics.AnalyticsDTO;
import leonardo.labutilities.qualitylabpro.dtos.analytics.GroupedValuesByLevelDTO;
import leonardo.labutilities.qualitylabpro.dtos.analytics.UpdateAnalyticsMeanDTO;
import leonardo.labutilities.qualitylabpro.entities.Analytic;
import leonardo.labutilities.qualitylabpro.repositories.AnalyticsRepository;
import leonardo.labutilities.qualitylabpro.services.analytics.AnalyticHelperService;
import leonardo.labutilities.qualitylabpro.services.email.EmailService;
import leonardo.labutilities.qualitylabpro.utils.components.ControlRulesValidators;
import leonardo.labutilities.qualitylabpro.utils.components.RulesValidatorComponent;
import leonardo.labutilities.qualitylabpro.utils.exception.CustomGlobalErrorHandling;
import leonardo.labutilities.qualitylabpro.utils.mappers.AnalyticMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static leonardo.labutilities.qualitylabpro.utils.AnalyticsHelperMocks.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticHelperServiceTests {

	@Mock
	private AnalyticsRepository analyticsRepository;
	@Mock
	private AnalyticHelperService analyticHelperService;
	@Mock
	private EmailService emailService;
	@Mock
	private ControlRulesValidators controlRulesValidators;

	@BeforeEach
	void setUp() {
		try (AutoCloseable closeable = MockitoAnnotations.openMocks(this)) {
			analyticHelperService = new AnalyticHelperService(analyticsRepository, emailService,
					controlRulesValidators) {

				@Override
				public List<AnalyticsDTO> findAnalyticsByNameAndLevel(Pageable pageable,
						String name, String level) {
					return analyticsRepository.findByNameAndLevel(pageable, name, level).stream()
							.map(AnalyticMapper::toRecord).toList();
				}

				@Override
				public List<AnalyticsDTO> findAnalyticsByNameAndLevelAndDate(String name,
						String level, LocalDateTime dateStart, LocalDateTime dateEnd,
						Pageable pageable) {
					return analyticsRepository
							.findByNameAndLevelAndDateBetween(name, level, dateStart, dateEnd,
									PageRequest.of(0, 200))
							.stream().map(AnalyticMapper::toRecord).toList();
				}
			};
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize mocks", e);
		}
	}


	@Test
	void updateAnalyticsMean() {
		var mockDto = new UpdateAnalyticsMeanDTO("Glucose", "PCCC1", "076587", 1.0);
		analyticHelperService.updateAnalyticsMeanByNameAndLevelAndLevelLot(mockDto.name(),
				mockDto.level(), mockDto.levelLot(), mockDto.mean());
		verify(analyticsRepository).updateMeanByNameAndLevelAndLevelLot(mockDto.name(),
				mockDto.level(), mockDto.levelLot(), mockDto.mean());
	}

	@Test
	void shouldValidateRulesProcessedByRulesValidatorComponent() {
		// Arrange: create sample input records
		List<AnalyticsDTO> records = createSampleRecordList();
		RulesValidatorComponent rulesValidatorComponent = new RulesValidatorComponent();

		// Act: convert the records to AnalyticsDTO using the validation component
		List<Analytic> analytics = records.stream()
				.map(values -> new Analytic(values, rulesValidatorComponent)).toList();

		// Assert: validate the rules generated by the component
		assertEquals(records.stream().map(AnalyticsDTO::rules).toList(),
				analytics.stream().map(Analytic::getRules).toList(), """
						The rules processed by the \
						RulesValidatorComponent should match the input\
						 rules""");
	}

	@Test
	void saveNewAnalyticsRecords_WithValidRecords_ShouldSaveSuccessfully() {
		List<AnalyticsDTO> records = createSampleRecordList();
		when(analyticsRepository.existsByDateAndLevelAndName(any(), any(), any()))
				.thenReturn(false);
		when(analyticsRepository.saveAll(any()))
				.thenAnswer(invocation -> invocation.getArgument(0));

		assertDoesNotThrow(() -> analyticHelperService.saveNewAnalyticsRecords(records));
		verify(analyticsRepository, times(1)).saveAll(any());
	}

	@Test
	void saveNewAnalyticsRecords_WithDuplicateRecords_ShouldThrowException() {
		List<AnalyticsDTO> records = createSampleRecordList();
		when(analyticsRepository.existsByDateAndLevelAndName(any(), any(), any())).thenReturn(true);

		assertThrows(CustomGlobalErrorHandling.DataIntegrityViolationException.class,
				() -> analyticHelperService.saveNewAnalyticsRecords(records));
		verify(analyticsRepository, never()).saveAll(any());
	}

	@Test
	void findById_WithValidId_ShouldReturnRecord() {
		Long id = 1L;
		Analytic analytic = new Analytic();
		when(analyticsRepository.findById(id)).thenReturn(Optional.of(analytic));

		Analytic result = AnalyticMapper.toEntity(analyticHelperService.findOneById(id));

		assertNotNull(result);
		assertEquals(analytic, result);
	}

	@Test
	void findById_WithInvalidId_ShouldThrowException() {
		Long id = 999L;
		when(analyticsRepository.findById(id)).thenReturn(Optional.empty());

		assertThrows(CustomGlobalErrorHandling.ResourceNotFoundException.class,
				() -> analyticHelperService.findOneById(id));
	}

	@Test
	void findAnalyticsByNameAndLevel_WithFilters_ShouldReturnFilteredRecords() {
		String name = "Glucose";
		String level = "Normal";
		Pageable pageable = PageRequest.of(0, 10);
		List<Analytic> expectedRecords = createSampleRecordList().stream()
				.filter(r -> r.name().equals(name) && r.level().equals(level)).toList().stream()
				.map(AnalyticMapper::toEntity).toList();

		when(analyticsRepository.findByNameAndLevel(pageable, name, level))
				.thenReturn(expectedRecords);

		List<AnalyticsDTO> result =
				analyticHelperService.findAnalyticsByNameAndLevel(pageable, name, level);

		assertEquals(expectedRecords.size(), result.size());
		verify(analyticsRepository).findByNameAndLevel(pageable, name, level);
	}

	@Test
	void findAllAnalyticsByNameAndLevelAndDate_WithDateRange_ShouldReturnFilteredRecords() {
		String name = "Glucose";
		String level = "Normal";
		LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
		LocalDateTime endDate = LocalDateTime.of(2024, 1, 2, 0, 0);
		List<Analytic> expectedRecords =
				createDateRangeRecords().stream().map(AnalyticMapper::toEntity).toList();

		when(analyticsRepository.findByNameAndLevelAndDateBetween(eq(name), eq(level),
				eq(startDate), eq(endDate), any(Pageable.class))).thenReturn(expectedRecords);

		List<AnalyticsDTO> result = analyticHelperService.findAnalyticsByNameAndLevelAndDate(name,
				level, startDate, endDate, null);

		assertNotNull(result);
		assertEquals(expectedRecords.size(), result.size());
	}

	@Test
	void deleteAnalyticsById_WithValidId_ShouldDelete() {
		Long id = 1L;
		when(analyticsRepository.existsById(id)).thenReturn(true);
		doNothing().when(analyticsRepository).deleteById(id);

		assertDoesNotThrow(() -> analyticHelperService.deleteAnalyticsById(id));

		verify(analyticsRepository).deleteById(id);
	}

	@Test
	void deleteAnalyticsById_WithInvalidId_ShouldThrowException() {
		Long id = 999L;
		when(analyticsRepository.existsById(id)).thenReturn(false);

		assertThrows(CustomGlobalErrorHandling.ResourceNotFoundException.class,
				() -> analyticHelperService.deleteAnalyticsById(id));
		verify(analyticsRepository, never()).deleteById(id);
	}

	@Test
	void ensureNameExists_WithValidName_ShouldNotThrowException() {
		String name = "Glucose";
		when(analyticsRepository.existsByName(name.toUpperCase())).thenReturn(true);

		assertDoesNotThrow(() -> analyticHelperService.ensureNameExists(name));
	}

	@Test
	void ensureNameExists_WithInvalidName_ShouldThrowException() {
		String name = "NonExistentTest";
		when(analyticsRepository.existsByName(name.toUpperCase())).thenReturn(false);

		assertThrows(CustomGlobalErrorHandling.ResourceNotFoundException.class,
				() -> analyticHelperService.ensureNameExists(name));
	}

	@Test
	void ensureNameNotExists_WithInvalidName_ShouldThrowException() {
		String name = "Glucose";
		when(analyticsRepository.existsByName(name.toUpperCase())).thenReturn(false);

		CustomGlobalErrorHandling.ResourceNotFoundException exception =
				assertThrows(CustomGlobalErrorHandling.ResourceNotFoundException.class,
						() -> analyticHelperService.ensureNameExists(name));

		assertEquals("AnalyticsDTO by name not found", exception.getMessage());
	}

	@Test
	void isAnalyticsNonExistent_WithNonExistentRecord_ShouldReturnTrue() {
		AnalyticsDTO record = createSampleRecord();
		when(analyticsRepository.existsByDateAndLevelAndName(record.date(), record.level(),
				record.name())).thenReturn(false);

		boolean result = analyticHelperService.isAnalyticsNonExistent(record);

		assertTrue(result);
	}

	@Test
	void isAnalyticsNonExistent_WithExistentRecord_ShouldReturnFalse() {
		AnalyticsDTO record = createSampleRecord();
		when(analyticsRepository.existsByDateAndLevelAndName(record.date(), record.level(),
				record.name())).thenReturn(true);

		boolean result = analyticHelperService.isAnalyticsNonExistent(record);

		assertFalse(result);
	}

	@Test
	void findGroupedAnalyticsByLevel_WithValidInputs_ShouldReturnGroupedRecords() {
		String name = "Glucose";
		LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
		LocalDateTime endDate = LocalDateTime.of(2024, 1, 2, 0, 0);
		List<Analytic> records =
				createSampleRecordList().stream().map(AnalyticMapper::toEntity).toList();

		when(analyticsRepository.findByNameAndDateBetweenGroupByLevel(eq(name), eq(startDate),
				eq(endDate), any(Pageable.class))).thenReturn(records);

		List<GroupedValuesByLevelDTO> result = analyticHelperService
				.findGroupedAnalyticsByLevel(name, startDate, endDate, Pageable.unpaged());

		assertNotNull(result);
		assertFalse(result.isEmpty());
		verify(analyticsRepository).findByNameAndDateBetweenGroupByLevel(eq(name), eq(startDate),
				eq(endDate), any(Pageable.class));
	}
}
