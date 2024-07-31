/*
 * Copyright (c) 2018 - 2020 Memorial Sloan-Kettering Cancer Center.
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

package org.cbioportal.cdd.service.internal;

import java.util.ArrayList;
import java.util.Map;
import javax.cache.CacheManager;
import javax.cache.spi.CachingProvider;
import org.cbioportal.cdd.model.ClinicalAttributeMetadata;
import org.cbioportal.cdd.repository.graphite.KnowledgeSystemsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * @author Robert Sheridan, Avery Wang, Manda Wilson
 */
@Component
public class ClinicalAttributeMetadataPersistentCache {

    private final static Logger logger = LoggerFactory.getLogger(ClinicalAttributeMetadataPersistentCache.class);

    @Autowired
    private CachingProvider cachingProvider;

    @Autowired
    private KnowledgeSystemsRepository clinicalAttributeRepository;

    private static final String CLINICAL_ATTRIBUTE_METADATA_CACHE = "clinicalAttributeMetadataEHCache";
    private static final String OVERRIDES_CACHE = "clinicalAttributeMetadataOverridesEHCache";
    public static final String CLINICAL_ATTRIBUTES_METADATA_CACHE_KEY = "CLINICAL_ATTRIBUTES_METADATA_CACHE_KEY";
    public static final String OVERRIDES_CACHE_KEY = "OVERRIDES_CACHE_KEY";

    public CacheManager getCacheManager(String ehcacheXMLFilename) throws Exception {
        CacheManager cacheManager = cachingProvider.getCacheManager(getClass().getClassLoader().getResource(ehcacheXMLFilename).toURI(), getClass().getClassLoader());
        return cacheManager;
    }

    // retrieve cached Graphite responses from default EHCache location
    @Cacheable(value = "clinicalAttributeMetadataEHCache", key = "#root.target.CLINICAL_ATTRIBUTES_METADATA_CACHE_KEY", unless = "#result==null")
    public ArrayList<ClinicalAttributeMetadata> getClinicalAttributeMetadataFromPersistentCache() {
        return clinicalAttributeRepository.getClinicalAttributeMetadata();
    }

    @Cacheable(value = "clinicalAttributeMetadataOverridesEHCache", key = "#root.target.OVERRIDES_CACHE_KEY", unless = "#result==null")
    public Map<String, ArrayList<ClinicalAttributeMetadata>> getClinicalAttributeMetadataOverridesFromPersistentCache() {
        return clinicalAttributeRepository.getClinicalAttributeMetadataOverrides();
    }

    // retrieve cache Graphite responses from backup EHCache location (and re-populate default EHCache locatin)
    @Cacheable(value = "clinicalAttributeMetadataEHCache", key = "#root.target.CLINICAL_ATTRIBUTES_METADATA_CACHE_KEY", unless = "#result==null")
    public ArrayList<ClinicalAttributeMetadata> getClinicalAttributeMetadataFromPersistentCacheBackup() throws Exception {
        CacheManager backupCacheManager = getCacheManager("ehcache_backup.xml");
        @SuppressWarnings("unchecked")
        ArrayList<ClinicalAttributeMetadata> clinicalAttributeMetadata = (ArrayList<ClinicalAttributeMetadata>)backupCacheManager.getCache(CLINICAL_ATTRIBUTE_METADATA_CACHE).get(CLINICAL_ATTRIBUTES_METADATA_CACHE_KEY);
        backupCacheManager.close();
        return clinicalAttributeMetadata;
    }

    @Cacheable(value = "clinicalAttributeMetadataOverridesEHCache", key = "#root.target.OVERRIDES_CACHE_KEY", unless = "#result==null")
    public Map<String, ArrayList<ClinicalAttributeMetadata>> getClinicalAttributeMetadataOverridesFromPersistentCacheBackup() throws Exception {
        CacheManager backupCacheManager = getCacheManager("ehcache_backup.xml");
        @SuppressWarnings("unchecked")
        Map<String, ArrayList<ClinicalAttributeMetadata>> clinicalAttributeMetadataOverrides = (Map<String, ArrayList<ClinicalAttributeMetadata>>)backupCacheManager.getCache(OVERRIDES_CACHE).get(OVERRIDES_CACHE_KEY);
        backupCacheManager.close();
        return clinicalAttributeMetadataOverrides;
    }

    // update default EHCache location with Graphite data
    @CachePut(value = "clinicalAttributeMetadataEHCache", key = "#root.target.CLINICAL_ATTRIBUTES_METADATA_CACHE_KEY", unless = "#result==null")
    public ArrayList<ClinicalAttributeMetadata> updateClinicalAttributeMetadataInPersistentCache() {
        logger.info("updating EHCache with updated clinical attribute metadata from Graphite");
        return  clinicalAttributeRepository.getClinicalAttributeMetadata();
    }

    @CachePut(value = "clinicalAttributeMetadataOverridesEHCache", key = "#root.target.OVERRIDES_CACHE_KEY", unless = "#result==null")
    public Map<String, ArrayList<ClinicalAttributeMetadata>> updateClinicalAttributeMetadataOverridesInPersistentCache() {
        logger.info("updating EHCache with updated overrides from Graphite");
        return clinicalAttributeRepository.getClinicalAttributeMetadataOverrides();
    }

    // update backup EHCache location with modeled-object cache values
    public void backupClinicalAttributeMetadataPersistentCache(ArrayList<ClinicalAttributeMetadata> clinicalAttributeMetadata) throws Exception {
        CacheManager cacheManager = getCacheManager("ehcache_backup.xml");
        cacheManager.getCache(CLINICAL_ATTRIBUTE_METADATA_CACHE).put(CLINICAL_ATTRIBUTES_METADATA_CACHE_KEY, clinicalAttributeMetadata);
        cacheManager.close();
    }

    public void backupClinicalAttributeMetadataOverridesPersistentCache(Map<String, ArrayList<ClinicalAttributeMetadata>> clinicalAttributeMetadataOverrides) throws Exception {
        CacheManager cacheManager = getCacheManager("ehcache_backup.xml");
        cacheManager.getCache(OVERRIDES_CACHE).put(OVERRIDES_CACHE_KEY, clinicalAttributeMetadataOverrides);
        cacheManager.close();
    }


}
