package com.cxy1ng.mapper;

import com.cxy1ng.entity.SiteConfig;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface SiteConfigMapper {

    @Select("SELECT id, config_key AS configKey, config_value AS configValue, description, updated_time AS updatedTime, updated_by AS updatedBy FROM site_config ORDER BY id ASC")
    List<SiteConfig> selectAll();

    @Select("SELECT id, config_key AS configKey, config_value AS configValue, description, updated_time AS updatedTime, updated_by AS updatedBy FROM site_config WHERE config_key = #{configKey} LIMIT 1")
    SiteConfig selectByKey(String configKey);

    @Insert("INSERT INTO site_config (config_key, config_value, description, updated_time, updated_by) VALUES (#{configKey}, #{configValue}, #{description}, #{updatedTime}, #{updatedBy})")
    int insert(SiteConfig siteConfig);

    @Update("UPDATE site_config SET config_value = #{configValue}, description = COALESCE(#{description}, description), updated_time = #{updatedTime}, updated_by = #{updatedBy} WHERE config_key = #{configKey}")
    int updateByKey(SiteConfig siteConfig);
}
