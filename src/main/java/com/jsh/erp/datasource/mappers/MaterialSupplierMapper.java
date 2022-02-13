package com.jsh.erp.datasource.mappers;

import com.jsh.erp.datasource.entities.MaterialLink;
import com.jsh.erp.datasource.entities.MaterialSupplier;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface MaterialSupplierMapper {
    List<MaterialSupplier> queryByMaterialId(@Param("materialId") Long materialId);

    List<MaterialSupplier> queryByMaterialIds(@Param("materialIds") Collection<Long> materialIds);

    int deleteByMaterialId(@Param("materialId") Long materialId);

    int batchInsert(@Param("models") List<MaterialSupplier> models);
}