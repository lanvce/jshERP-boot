package com.jsh.erp.datasource.mappers;

import com.jsh.erp.datasource.entities.MaterialLink;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MaterialSupplierMapper {
    List<MaterialLink> queryByMaterialId(@Param("materialId") Long materialId);

    int deleteByMaterialId(@Param("materialId") Long materialId);

    int batchInsert(@Param("models") MaterialLink models);
}