package com.tt.tripbook.repository;

import static com.tt.tripbook.TestData.destination;
import static org.assertj.core.api.Assertions.assertThat;

import com.tt.tripbook.model.Destination;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

// Runs against the in-memory H2 database; every test is rolled back.
@SpringBootTest
@Transactional
class DestinationRepositoryTest {

    @Autowired
    private DestinationRepository repository;

    @Test
    void findByCountryContainingIgnoreCase_matchesPartialTextIgnoringCase() {
        repository.save(destination("Lisbon", "Portugal"));
        repository.save(destination("Porto", "Portugal"));
        repository.save(destination("Tokyo", "Japan"));

        Page<Destination> result = repository.findByCountryContainingIgnoreCase(
                "PORTU", PageRequest.of(0, 10, Sort.by("city")));

        assertThat(result.getContent()).extracting(Destination::getCity).containsExactly("Lisbon", "Porto");
    }

    @Test
    void findByCountryContainingIgnoreCase_returnsOnlyTheRequestedPage() {
        repository.save(destination("Lisbon", "Portugal"));
        repository.save(destination("Porto", "Portugal"));
        repository.save(destination("Faro", "Portugal"));
        repository.save(destination("Tokyo", "Japan"));

        Page<Destination> secondPage = repository.findByCountryContainingIgnoreCase(
                "Portugal", PageRequest.of(1, 2, Sort.by("city")));

        assertThat(secondPage.getContent()).extracting(Destination::getCity).containsExactly("Porto");
        assertThat(secondPage.getTotalElements()).isEqualTo(3);
        assertThat(secondPage.getTotalPages()).isEqualTo(2);
        assertThat(secondPage.isLast()).isTrue();
    }

    @Test
    void existsByCityAndCountry_ignoresLetterCase() {
        repository.save(destination("Rio de Janeiro", "Brazil"));

        assertThat(repository.existsByCityIgnoreCaseAndCountryIgnoreCase("RIO DE JANEIRO", "brazil")).isTrue();
        assertThat(repository.existsByCityIgnoreCaseAndCountryIgnoreCase("Rio de Janeiro", "Portugal")).isFalse();
    }

    @Test
    void existsByCityAndCountryAndIdNot_excludesTheDestinationItself() {
        Destination rio = repository.save(destination("Rio de Janeiro", "Brazil"));

        assertThat(repository.existsByCityIgnoreCaseAndCountryIgnoreCaseAndIdNot(
                "Rio de Janeiro", "Brazil", rio.getId())).isFalse();
        assertThat(repository.existsByCityIgnoreCaseAndCountryIgnoreCaseAndIdNot(
                "Rio de Janeiro", "Brazil", rio.getId() + 1)).isTrue();
    }
}
