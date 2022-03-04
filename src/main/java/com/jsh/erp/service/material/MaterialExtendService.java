package com.jsh.erp.service.material;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.MaterialExtend;
import com.jsh.erp.datasource.entities.MaterialExtendExample;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.MaterialExtendMapper;
import com.jsh.erp.datasource.mappers.MaterialExtendMapperEx;
import com.jsh.erp.datasource.vo.MaterialExtendVo4List;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.service.system.log.LogService;
import com.jsh.erp.service.system.redis.RedisService;
import com.jsh.erp.service.auth.user.UserService;
import com.jsh.erp.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Service
public class MaterialExtendService {
    private Logger logger = LoggerFactory.getLogger(MaterialExtendService.class);

    @Resource
    private MaterialExtendMapper materialExtendMapper;
    @Resource
    private MaterialExtendMapperEx materialExtendMapperEx;
    @Resource
    private LogService logService;
    @Resource
    private UserService userService;
    @Resource
    private RedisService redisService;

    public MaterialExtend getMaterialExtend(long id) throws Exception {
        MaterialExtend result = null;
        try {
            result = materialExtendMapper.selectByPrimaryKey(id);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return result;
    }

    public List<MaterialExtendVo4List> getDetailList(Long materialId) {
        List<MaterialExtendVo4List> list = null;
        try {
            list = materialExtendMapperEx.getDetailList(materialId);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<MaterialExtend> getListByMIds(List<Long> idList) {
        List<MaterialExtend> meList = null;
        try {
            Long[] idArray = StringUtil.listToLongArray(idList);
            if (idArray != null && idArray.length > 0) {
                meList = materialExtendMapperEx.getListByMId(idArray);
            }
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return meList;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public String saveDetials(JSONObject obj, String sortList, Long materialId, String type) throws Exception {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        JSONArray meArr = obj.getJSONArray("meList");
        JSONArray insertedJson = new JSONArray();
        JSONArray updatedJson = new JSONArray();
        JSONArray deletedJson = new JSONArray();
        List<String> barCodeList = new ArrayList<>();
        if (null != meArr) {
            if ("insert".equals(type)) {
                for (int i = 0; i < meArr.size(); i++) {
                    JSONObject tempJson = meArr.getJSONObject(i);
                    if (StringUtils.isNotEmpty(tempJson.getString("barCode"))) {
                        insertedJson.add(tempJson);
                    }
                }
            } else if ("update".equals(type)) {
                for (int i = 0; i < meArr.size(); i++) {
                    JSONObject tempJson = meArr.getJSONObject(i);
                    String barCode = tempJson.getString("barCode");
                    barCodeList.add(barCode);
                    MaterialExtend materialExtend = getInfoByBarCode(barCode);
                    if (materialExtend.getBarCode() == null) {
                        insertedJson.add(tempJson);
                    } else {
                        updatedJson.add(tempJson);
                    }
                }
                List<MaterialExtend> materialExtendList = getMeListByBarCodeAndMid(barCodeList, materialId);
                for (MaterialExtend meObj : materialExtendList) {
                    JSONObject deleteObj = new JSONObject();
                    deleteObj.put("id", meObj.getId());
                    deletedJson.add(deleteObj);
                }
            }
        }
        JSONArray sortJson = JSONArray.parseArray(sortList);
        if (null != insertedJson) {
            for (int i = 0; i < insertedJson.size(); i++) {
                MaterialExtend materialExtend = new MaterialExtend();
                JSONObject tempInsertedJson = JSONObject.parseObject(insertedJson.getString(i));
                materialExtend.setMaterialId(materialId);
                if (StringUtils.isNotEmpty(tempInsertedJson.getString("barCode"))) {
                    int exist = checkIsBarCodeExist(0L, tempInsertedJson.getString("barCode"));
                    if (exist > 0) {
                        throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_BARCODE_EXISTS_CODE,
                                String.format(ExceptionConstants.MATERIAL_BARCODE_EXISTS_MSG, tempInsertedJson.getString("barCode")));
                    } else {
                        materialExtend.setBarCode(tempInsertedJson.getString("barCode"));
                    }
                }
                if (StringUtils.isNotEmpty(tempInsertedJson.getString("commodityUnit"))) {
                    materialExtend.setCommodityUnit(tempInsertedJson.getString("commodityUnit"));
                }
                if (tempInsertedJson.get("sku") != null) {
                    materialExtend.setSku(tempInsertedJson.getString("sku"));
                }
                if (StringUtils.isNotEmpty(tempInsertedJson.getString("purchaseDecimal"))) {
                    materialExtend.setPurchaseDecimal(tempInsertedJson.getBigDecimal("purchaseDecimal"));
                }
                if (StringUtils.isNotEmpty(tempInsertedJson.getString("dropshippingDecimal"))) {
                    materialExtend.setDropshippingDecimal(tempInsertedJson.getBigDecimal("dropshippingDecimal"));
                }
                if (StringUtils.isNotEmpty(tempInsertedJson.getString("costDecimal"))) {
                    materialExtend.setCostDecimal(tempInsertedJson.getBigDecimal("costDecimal"));
                }
                if (StringUtils.isNotEmpty(tempInsertedJson.getString("commodityDecimal"))) {
                    materialExtend.setCommodityDecimal(tempInsertedJson.getBigDecimal("commodityDecimal"));
                }
                if (StringUtils.isNotEmpty(tempInsertedJson.getString("wholesaleDecimal"))) {
                    materialExtend.setWholesaleDecimal(tempInsertedJson.getBigDecimal("wholesaleDecimal"));
                }
                if (StringUtils.isNotEmpty(tempInsertedJson.getString("lowDecimal"))) {
                    materialExtend.setLowDecimal(tempInsertedJson.getBigDecimal("lowDecimal"));
                }
                if (StringUtils.isNotEmpty(tempInsertedJson.getString("supplierId"))) {
                    materialExtend.setSupplierId(tempInsertedJson.getLong("supplierId"));
                }
                //设置 税率相关
                if (StringUtils.isNotEmpty(tempInsertedJson.getString("normalTaxRate"))){
                    materialExtend.setNormalTaxRate(tempInsertedJson.getString("normalTaxRate"));
                }
                if (StringUtils.isNotEmpty(tempInsertedJson.getString("noTaxRate"))){
                    materialExtend.setNoTaxRate(tempInsertedJson.getString("noTaxRate"));
                }
                if (StringUtils.isNotEmpty(tempInsertedJson.getString("specialTaxRate"))){
                    materialExtend.setSpecialTaxRate(tempInsertedJson.getString("specialTaxRate"));
                }
                this.insertMaterialExtend(materialExtend);
            }
        }
        if (null != deletedJson) {
            StringBuffer bf = new StringBuffer();
            for (int i = 0; i < deletedJson.size(); i++) {
                JSONObject tempDeletedJson = JSONObject.parseObject(deletedJson.getString(i));
                bf.append(tempDeletedJson.getLong("id"));
                if (i < (deletedJson.size() - 1)) {
                    bf.append(",");
                }
            }
            this.batchDeleteMaterialExtendByIds(bf.toString(), request);
        }
        if (null != updatedJson) {
            for (int i = 0; i < updatedJson.size(); i++) {
                JSONObject tempUpdatedJson = JSONObject.parseObject(updatedJson.getString(i));
                MaterialExtend materialExtend = new MaterialExtend();
                materialExtend.setId(tempUpdatedJson.getLong("id"));
                if (StringUtils.isNotEmpty(tempUpdatedJson.getString("barCode"))) {
                    int exist = checkIsBarCodeExist(tempUpdatedJson.getLong("id"), tempUpdatedJson.getString("barCode"));
                    if (exist > 0) {
                        throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_BARCODE_EXISTS_CODE,
                                String.format(ExceptionConstants.MATERIAL_BARCODE_EXISTS_MSG, tempUpdatedJson.getString("barCode")));
                    } else {
                        materialExtend.setBarCode(tempUpdatedJson.getString("barCode"));
                    }
                }
                if (StringUtils.isNotEmpty(tempUpdatedJson.getString("commodityUnit"))) {
                    materialExtend.setCommodityUnit(tempUpdatedJson.getString("commodityUnit"));
                }

                if (StringUtils.isNotEmpty(tempUpdatedJson.getString("purchaseDecimal"))) {
                    materialExtend.setPurchaseDecimal(tempUpdatedJson.getBigDecimal("purchaseDecimal"));
                }else {
                    materialExtend.setPurchaseDecimal(new BigDecimal(0));
                }

                if (StringUtils.isNotEmpty(tempUpdatedJson.getString("dropshippingDecimal"))) {
                    materialExtend.setDropshippingDecimal(tempUpdatedJson.getBigDecimal("dropshippingDecimal"));
                }else {
                    materialExtend.setDropshippingDecimal(new BigDecimal(0));
                }

                if (StringUtils.isNotEmpty(tempUpdatedJson.getString("commodityDecimal"))) {
                    materialExtend.setCommodityDecimal(tempUpdatedJson.getBigDecimal("commodityDecimal"));
                } else {
                    materialExtend.setCommodityDecimal(new BigDecimal(0));
                }

                if (StringUtils.isNotEmpty(tempUpdatedJson.getString("wholesaleDecimal"))) {
                    materialExtend.setWholesaleDecimal(tempUpdatedJson.getBigDecimal("wholesaleDecimal"));
                }
                else {
                    materialExtend.setWholesaleDecimal(new BigDecimal(0));
                }

                if (StringUtils.isNotEmpty(tempUpdatedJson.getString("lowDecimal"))) {
                    materialExtend.setLowDecimal(tempUpdatedJson.getBigDecimal("lowDecimal"));
                }
                else {
                    materialExtend.setLowDecimal(new BigDecimal(0));
                }

                if (null != tempUpdatedJson.getLong("supplierId")) {
                    materialExtend.setSupplierId(tempUpdatedJson.getLong("supplierId"));
                }

                //设置 税率相关
                if (StringUtils.isNotEmpty(tempUpdatedJson.getString("normalTaxRate"))){
                    materialExtend.setNormalTaxRate(tempUpdatedJson.getString("normalTaxRate"));
                }
                if (StringUtils.isNotEmpty(tempUpdatedJson.getString("noTaxRate"))){
                    materialExtend.setNoTaxRate(tempUpdatedJson.getString("noTaxRate"));
                }
                if (StringUtils.isNotEmpty(tempUpdatedJson.getString("specialTaxRate"))){
                    materialExtend.setSpecialTaxRate(tempUpdatedJson.getString("specialTaxRate"));
                }
                this.updateMaterialExtend(materialExtend);
            }
        }
        //处理条码的排序，基本单位排第一个
        if (null != sortJson && sortJson.size() > 0) {
            //此处为更新的逻辑
            for (int i = 0; i < sortJson.size(); i++) {
                JSONObject tempSortJson = JSONObject.parseObject(sortJson.getString(i));
                MaterialExtend materialExtend = new MaterialExtend();
                if (StringUtil.isExist(tempSortJson.get("id"))) {
                    materialExtend.setId(tempSortJson.getLong("id"));
                }
                if (StringUtil.isExist(tempSortJson.get("defaultFlag"))) {
                    materialExtend.setDefaultFlag(tempSortJson.getString("defaultFlag"));
                }
                this.updateMaterialExtend(materialExtend);
            }
        } else {
            //新增的时候将价格低的设置为默认  一次比较集采价 代发价 市场零售价
            MaterialExtendExample example = new MaterialExtendExample();
            example.createCriteria().andMaterialIdEqualTo(materialId).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            List<MaterialExtend> meList = materialExtendMapper.selectByExample(example);
            if (meList != null) {
                BigDecimal minPurchaseDecimal = null;
                BigDecimal minDropShippingDecimal = null;
                BigDecimal minCommodityDecimal = null;
                int minIndex=0;

                for (int i = 0; i < meList.size(); i++) {
                    MaterialExtend materialExtend = new MaterialExtend();
                    MaterialExtend curMaterialExtend = meList.get(i);
                    materialExtend.setId(curMaterialExtend.getId());
                    materialExtend.setDefaultFlag("0"); //先全部设置为非默认

                    if (i==0){
                        minPurchaseDecimal=curMaterialExtend.getPurchaseDecimal();
                        minCommodityDecimal=curMaterialExtend.getCommodityDecimal();
                        minDropShippingDecimal=curMaterialExtend.getDropshippingDecimal();
                    }
                    //比较集采价
                    if (curMaterialExtend.getPurchaseDecimal()!=null&&minPurchaseDecimal!=null&&curMaterialExtend.getPurchaseDecimal().compareTo(minPurchaseDecimal) == -1) {
                            minPurchaseDecimal = curMaterialExtend.getPurchaseDecimal();
                            minIndex=i;
                    }else {
                        //如果集采价为空 比较代发价
                        if (curMaterialExtend.getDropshippingDecimal()!=null&&minDropShippingDecimal!=null&&curMaterialExtend.getDropshippingDecimal().compareTo(minDropShippingDecimal)==-1){
                            minDropShippingDecimal=curMaterialExtend.getDropshippingDecimal();
                            minIndex=i;
                        }else {
                            //如果代发价也为空 比较市场零售价
                            if (curMaterialExtend.getCommodityDecimal()!=null&&minCommodityDecimal!=null&&curMaterialExtend.getCommodityDecimal().compareTo(minCommodityDecimal)==-1){
                                minCommodityDecimal=curMaterialExtend.getCommodityDecimal();
                                minIndex=i;
                            }
                        }
                    }
                    this.updateMaterialExtend(materialExtend);
                }
                //根据minIndex重新设置默认
                MaterialExtend materialExtend = new MaterialExtend();
                MaterialExtend curMaterialExtend = meList.get(minIndex);
                materialExtend.setId(curMaterialExtend.getId());
                materialExtend.setDefaultFlag("1");
                this.updateMaterialExtend(materialExtend);
            }
        }
        return null;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertMaterialExtend(MaterialExtend materialExtend) throws Exception {
        User user = userService.getCurrentUser();
        materialExtend.setDeleteFlag(BusinessConstants.DELETE_FLAG_EXISTS);
        materialExtend.setCreateTime(new Date());
        materialExtend.setUpdateTime(new Date().getTime());
        materialExtend.setCreateSerial(user.getLoginName());
        materialExtend.setUpdateSerial(user.getLoginName());
        int result = 0;
        try {
            result = materialExtendMapper.insertSelective(materialExtend);
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateMaterialExtend(MaterialExtend MaterialExtend) throws Exception {
        User user = userService.getCurrentUser();
        MaterialExtend.setUpdateTime(System.currentTimeMillis());
        MaterialExtend.setUpdateSerial(user.getLoginName());
        int res = 0;
        try {
            res = materialExtendMapper.updateByPrimaryKeySelective(MaterialExtend);
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return res;
    }

    public int checkIsBarCodeExist(Long id, String barCode) throws Exception {
        MaterialExtendExample example = new MaterialExtendExample();
        MaterialExtendExample.Criteria criteria = example.createCriteria();
        criteria.andBarCodeEqualTo(barCode);
        if (id > 0) {
            criteria.andIdNotEqualTo(id).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        } else {
            criteria.andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        }
        List<MaterialExtend> list = null;
        try {
            list = materialExtendMapper.selectByExample(example);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list == null ? 0 : list.size();
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteMaterialExtend(Long id, HttpServletRequest request) throws Exception {
        int result = 0;
        MaterialExtend materialExtend = new MaterialExtend();
        materialExtend.setId(id);
        materialExtend.setDeleteFlag(BusinessConstants.DELETE_FLAG_DELETED);
        Long userId = Long.parseLong(redisService.getObjectFromSessionByKey(request, "userId").toString());
        User user = userService.getUser(userId);
        materialExtend.setUpdateTime(new Date().getTime());
        materialExtend.setUpdateSerial(user.getLoginName());
        try {
            result = materialExtendMapper.updateByPrimaryKeySelective(materialExtend);
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteMaterialExtendByIds(String ids, HttpServletRequest request) throws Exception {
        String[] idArray = ids.split(",");
        int result = 0;
        try {
            result = materialExtendMapperEx.batchDeleteMaterialExtendByIds(idArray);
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public int insertMaterialExtend(JSONObject obj, HttpServletRequest request) throws Exception {
        MaterialExtend materialExtend = JSONObject.parseObject(obj.toJSONString(), MaterialExtend.class);
        int result = 0;
        try {
            result = materialExtendMapper.insertSelective(materialExtend);
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public int updateMaterialExtend(JSONObject obj, HttpServletRequest request) throws Exception {
        MaterialExtend materialExtend = JSONObject.parseObject(obj.toJSONString(), MaterialExtend.class);
        int result = 0;
        try {
            result = materialExtendMapper.insertSelective(materialExtend);
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public List<MaterialExtend> getMaterialExtendByTenantAndTime(Long tenantId, Long lastTime, Long syncNum) throws Exception {
        List<MaterialExtend> list = new ArrayList<MaterialExtend>();
        try {
            //先获取最大的时间戳，再查两个时间戳之间的数据，这样同步能够防止丢失数据（应为时间戳有重复）
            Long maxTime = materialExtendMapperEx.getMaxTimeByTenantAndTime(tenantId, lastTime, syncNum);
            if (tenantId != null && lastTime != null && maxTime != null) {
                MaterialExtendExample example = new MaterialExtendExample();
                example.createCriteria().andTenantIdEqualTo(tenantId)
                        .andUpdateTimeGreaterThan(lastTime)
                        .andUpdateTimeLessThanOrEqualTo(maxTime);
                list = materialExtendMapper.selectByExample(example);
            }
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public Long selectIdByMaterialIdAndDefaultFlag(Long materialId, String defaultFlag) throws Exception {
        Long id = 0L;
        MaterialExtendExample example = new MaterialExtendExample();
        example.createCriteria().andMaterialIdEqualTo(materialId).andDefaultFlagEqualTo(defaultFlag)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<MaterialExtend> list = materialExtendMapper.selectByExample(example);
        if (list != null && list.size() > 0) {
            id = list.get(0).getId();
        }
        return id;
    }

    public MaterialExtend getInfoByBarCode(String barCode) throws Exception {
        MaterialExtend materialExtend = new MaterialExtend();
        MaterialExtendExample example = new MaterialExtendExample();
        example.createCriteria().andBarCodeEqualTo(barCode)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<MaterialExtend> list = materialExtendMapper.selectByExample(example);
        if (list != null && list.size() > 0) {
            materialExtend = list.get(0);
        }
        return materialExtend;
    }

    /**
     * 查询某个商品里面被清除的条码信息
     *
     * @param barCodeList
     * @param mId
     * @return
     * @throws Exception
     */
    public List<MaterialExtend> getMeListByBarCodeAndMid(List<String> barCodeList, Long mId) throws Exception {
        MaterialExtend materialExtend = new MaterialExtend();
        MaterialExtendExample example = new MaterialExtendExample();
        example.createCriteria().andBarCodeNotIn(barCodeList).andMaterialIdEqualTo(mId)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<MaterialExtend> list = materialExtendMapper.selectByExample(example);
        return list;
    }
}
