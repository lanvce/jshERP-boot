package com.jsh.erp.datasource.mappers;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jsh.erp.datasource.entities.MaterialLink;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface MaterialLinkMapper {

    List<MaterialLink> queryByMaterialId(@Param("materialId") Long materialId);

    List<MaterialLink> queryByMaterialIds(@Param("materialIds") Collection<Long> materialIds);

    int deleteByMaterialId(@Param("materialId") Long materialId);

    int batchInsert(@Param("models") List<MaterialLink> models);

}