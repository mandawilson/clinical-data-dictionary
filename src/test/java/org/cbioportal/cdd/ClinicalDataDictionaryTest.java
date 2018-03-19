/*
 * Copyright (c) 2018 Memorial Sloan-Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan-Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan-Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan-Kettering Cancer
 * Center has been advised of the possibility of such damage.
 */

package org.cbioportal.cdd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

import org.cbioportal.cdd.repository.ClinicalAttributeMetadataRepository;
import org.cbioportal.cdd.service.internal.ClinicalAttributeMetadataCache;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertThat;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.junit.Before;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Manda Wilson, Avery Wang
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(ClinicalDataDictionaryTestConfig.class)
public class ClinicalDataDictionaryTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ClinicalAttributeMetadataRepository clinicalAttributesRepository;

    @Autowired
    private ClinicalAttributeMetadataCache clinicalAttributesCache;

    @Before
    // make sure repository is working version before each test
    public void resetToWorkingRepository() {
        ClinicalDataDictionaryTestConfig config = new ClinicalDataDictionaryTestConfig();
        config.resetWorkingClinicalAttributesRepository(clinicalAttributesRepository);
        ResponseEntity<String> response = restTemplate.getForEntity("/api/refreshCache", String.class);
    }

    @Test
    public void getClinicalAttributeMetadataTest() throws Exception {
        // test all clinical attributes are returned by GET /api/
        ResponseEntity<String> response = restTemplate.getForEntity("/api/", String.class);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode responseJSON = mapper.readTree(response.getBody());

        assertThat(responseJSON.size(), equalTo(5));
        assertThat(response.getBody(), containsString("{\"column_header\":\"LAST_STATUS\",\"display_name\":\"Last Status\",\"description\":\"Last Status.\",\"datatype\":\"STRING\",\"attribute_type\":\"PATIENT\",\"priority\":\"1\"}"));
    }

    @Test
    public void getClinicalAttributeMetadataFilteredTest() throws Exception {
        // test we can get a list of clinical attributes returned by POST /api/
        List<String> columnHeaders = Arrays.asList("AGE", "LAST_status");
        ResponseEntity<String> response = restTemplate.postForEntity("/api/", columnHeaders, String.class);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode responseJSON = mapper.readTree(response.getBody());

        assertThat(responseJSON.size(), equalTo(2));
        assertThat(response.getBody(), containsString("{\"column_header\":\"LAST_STATUS\",\"display_name\":\"Last Status\",\"description\":\"Last Status.\",\"datatype\":\"STRING\",\"attribute_type\":\"PATIENT\",\"priority\":\"1\"}"));
        assertThat(response.getBody(), containsString("{\"column_header\":\"AGE\",\"display_name\":\"Diagnosis Age\",\"description\":\"Age at which a condition or disease was first diagnosed.\",\"datatype\":\"NUMBER\",\"attribute_type\":\"PATIENT\",\"priority\":\"1\"}"));
    }

    @Test
    public void getClinicalAttributeMetadataInvalidClinicalAttributeTest() throws Exception {
        // test an invalid clinical attribute throws an exception in POST /api/
        List<String> columnHeaders = Arrays.asList("AGE", "LAST_status", "INVALID_ATTRIBUTE");
        ResponseEntity<String> response = restTemplate.postForEntity("/api/", columnHeaders, String.class);
        assertThat(response.getBody(), containsString("org.cbioportal.cdd.service.exception.ClinicalAttributeNotFoundException"));
        assertThat(response.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    }

    @Test
    public void getClinicalAttributeTest() throws Exception {
        // test we can get one clinical attribute returned by GET /api/AGE/
        ResponseEntity<String> response = restTemplate.getForEntity("/api/AGE/", String.class);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));

        assertThat(response.getBody(), equalTo("{\"column_header\":\"AGE\",\"display_name\":\"Diagnosis Age\",\"description\":\"Age at which a condition or disease was first diagnosed.\",\"datatype\":\"NUMBER\",\"attribute_type\":\"PATIENT\",\"priority\":\"1\"}"));
    }

    @Test
    public void getClinicalAttributeInvalidClinicalAttributeTest() throws Exception {
        // test invalid clinical attribute throws an exception in GET /api/INVALID_ATTRIBUTE
        ResponseEntity<String> response = restTemplate.getForEntity("/api/INVALID_ATTRIBUTE", String.class);
        assertThat(response.getBody(), containsString("org.cbioportal.cdd.service.exception.ClinicalAttributeNotFoundException"));
        assertThat(response.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    }

    @Test
    public void forceResetCacheTest() throws Exception {
        // test that cache is updated after GET /api/refreshCache
        ClinicalDataDictionaryTestConfig config = new ClinicalDataDictionaryTestConfig();
        // change repository to version with 2 attributes/1 override
        config.resetUpdatedClinicalAttributesRepository(clinicalAttributesRepository);
        ResponseEntity<String> response = restTemplate.getForEntity("/api/refreshCache", String.class);
        assertThat(response.getBody(), equalTo(null));
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(clinicalAttributesRepository.getClinicalAttributeMetadata().size(), equalTo(2));
        assertThat(clinicalAttributesRepository.getClinicalAttributeMetadataOverrides(), hasKey("updated_override_study"));
        assertThat(clinicalAttributesRepository.getClinicalAttributeMetadataOverrides().size(), equalTo(1));
        // change repository to non-working version
        config.resetNotWorkingClinicalAttributesRepository(clinicalAttributesRepository);
        response = restTemplate.getForEntity("/api/refreshCache", String.class);
        assertThat(response.getBody(), containsString("org.cbioportal.cdd.service.exception.ClinicalMetadataSourceUnresponsiveException"));
        assertThat(response.getStatusCode(), equalTo(HttpStatus.SERVICE_UNAVAILABLE));
        // change repository to version with 5 attributes/2 overrides
        config.resetWorkingClinicalAttributesRepository(clinicalAttributesRepository);
        restTemplate.getForEntity("/api/refreshCache", String.class);
        assertThat(clinicalAttributesRepository.getClinicalAttributeMetadata().size(), equalTo(5));
        assertThat(clinicalAttributesRepository.getClinicalAttributeMetadataOverrides().size(), equalTo(2));
        assertThat(clinicalAttributesRepository.getClinicalAttributeMetadataOverrides(), not(hasKey("updated_override_study")));
    }

    @Test
    public void expiredCacheTest() throws Exception {
        // test that if the repository throws an exception, we expire the cache
        ResponseEntity<String> response;

        // now have the repository start thowing exceptions, we should be OK for 2 resetCache calls
        // the third resetCache call invalidates the cache
        ClinicalDataDictionaryTestConfig config = new ClinicalDataDictionaryTestConfig();
        config.resetNotWorkingClinicalAttributesRepository(clinicalAttributesRepository);
        for (int i = 0; i < 3; i++) {
            // we should tolerate two failures
            response = restTemplate.getForEntity("/api/", String.class);
            assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
            clinicalAttributesCache.resetCache();
        }

        // the third reset failure should have expired the cache
        response = restTemplate.getForEntity("/api/", String.class);
        assertThat(response.getBody(), containsString("org.cbioportal.cdd.service.exception.ClinicalMetadataSourceUnresponsiveException"));
        assertThat(response.getStatusCode(), equalTo(HttpStatus.SERVICE_UNAVAILABLE));

        config.resetWorkingClinicalAttributesRepository(clinicalAttributesRepository);
        clinicalAttributesCache.resetCache();
        // repositiory should work again
        response = restTemplate.getForEntity("/api/", String.class);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }

    @Test
    public void getCancerStudiesTest() throws Exception {
        // test all cancer studies are returned by GET /api/cancerStudies
        ResponseEntity<String> response = restTemplate.getForEntity("/api/cancerStudies", String.class);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode responseJSON = mapper.readTree(response.getBody());

        assertThat(responseJSON.size(), equalTo(2));
        assertThat(response.getBody(), containsString("{\"name\":\"test_override_study\"}"));
        assertThat(response.getBody(), containsString("{\"name\":\"mskimpact\"}"));
    }

    @Test
    public void getClinicalAttributeMetadataWithOverrideTest() throws Exception {
        // test cancer study is applied correctly by GET /api/?cancerStudy=test_override_study
        ResponseEntity<String> response = restTemplate.getForEntity("/api/", String.class);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode responseJSON = mapper.readTree(response.getBody());

        assertThat(getPriority(responseJSON, "AGE"), equalTo("1"));
        assertThat(getPriority(responseJSON, "DISEASE_STAGE"), equalTo("1"));

        // now get the same information with overrides
        response = restTemplate.getForEntity("/api/?cancerStudy=test_override_study", String.class);
        responseJSON = mapper.readTree(response.getBody());

        assertThat(getPriority(responseJSON, "AGE"), equalTo("100"));
        assertThat(getPriority(responseJSON, "DISEASE_STAGE"), equalTo("10"));
        assertThat(getPriority(responseJSON, "CLIN_M_STAGE"), equalTo("1")); // this should not have changed
    }

    @Test
    public void getClinicalAttributeMetadataWithOverrideForMskimpactTest() throws Exception {
        // test cancer study is applied correctly by GET /api/?cancerStudy=mskimpact
        ResponseEntity<String> response = restTemplate.getForEntity("/api/", String.class);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode responseJSON = mapper.readTree(response.getBody());

        assertThat(getPriority(responseJSON, "LAST_STATUS"), equalTo("1"));
        assertThat(getPriority(responseJSON, "DISEASE_STAGE"), equalTo("1"));

        // now get the same information with overrides
        response = restTemplate.getForEntity("/api/?cancerStudy=mskimpact", String.class);
        responseJSON = mapper.readTree(response.getBody());

        assertThat(getPriority(responseJSON, "LAST_STATUS"), equalTo("1"));
        // for mskimpact all attributes that are not assigned a priority explicitly by the CancerStudy are set to priority 0
        assertThat(getPriority(responseJSON, "DISEASE_STAGE"), equalTo("0"));
    }

    @Test
    public void getClinicalAttributeMetadataInvalidCancerStudyTest() throws Exception {
        // test an invalid cancer study in GET /api/?cancerStudy=INVALID_POLICY
        ResponseEntity<String> response = restTemplate.getForEntity("/api/?cancerStudy=INVALID_POLICY", String.class);
        assertThat(response.getBody(), containsString("org.cbioportal.cdd.service.exception.CancerStudyNotFoundException"));
        assertThat(response.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    }

    private String getPriority(JsonNode jsonResults, String attributeName) {
        for (JsonNode attributeMedadata : jsonResults) {
            if  (attributeMedadata.get("column_header").textValue().equals(attributeName)) {
                return attributeMedadata.get("priority").textValue();
            }
        }
        return null;
    }
}
