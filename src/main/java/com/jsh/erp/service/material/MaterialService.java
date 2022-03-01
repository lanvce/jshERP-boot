package com.jsh.erp.service.material;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.controller.system.SystemConfigController;
import com.jsh.erp.datasource.entities.*;
import com.jsh.erp.datasource.mappers.*;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.service.depot.DepotService;
import com.jsh.erp.service.depot.DepotItemService;
import com.jsh.erp.service.system.log.LogService;
import com.jsh.erp.service.system.redis.RedisService;
import com.jsh.erp.service.auth.supplier.SupplierService;
import com.jsh.erp.service.system.unit.UnitService;
import com.jsh.erp.service.auth.user.UserService;
import com.jsh.erp.utils.*;
import jxl.Sheet;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MaterialService {
    private Logger logger = LoggerFactory.getLogger(MaterialService.class);

    @Resource
    private MaterialMapper materialMapper;

    @Resource
    private MaterialLinkMapper materialLinkMapper;

    @Resource
    private MaterialExtendMapper materialExtendMapper;
    @Resource
    private MaterialMapperEx materialMapperEx;
    @Resource
    private MaterialCategoryMapperEx materialCategoryMapperEx;
    @Resource
    private MaterialExtendMapperEx materialExtendMapperEx;
    @Resource
    private LogService logService;
    @Resource
    private UserService userService;
    @Resource
    private DepotItemMapperEx depotItemMapperEx;
    @Resource
    private DepotItemService depotItemService;
    @Resource
    private MaterialCategoryService materialCategoryService;
    @Resource
    private UnitService unitService;
    @Resource
    private MaterialInitialStockMapper materialInitialStockMapper;
    @Resource
    private MaterialCurrentStockMapper materialCurrentStockMapper;
    @Resource
    private DepotService depotService;
    @Resource
    private MaterialExtendService materialExtendService;
    @Resource
    private RedisService redisService;

    @Resource
    private SupplierService supplierService;

    @Resource
    private SupplierMapper supplierMapper;

    @Value(value = "${file.path}")
    private String filePath;

    public Material getMaterial(long id) throws Exception {
        Material result = null;
        try {
            result = materialMapper.selectByPrimaryKey(id);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return result;
    }

    public List<Material> getMaterialListByIds(String ids) throws Exception {
        List<Long> idList = StringUtil.strToLongList(ids);
        List<Material> list = new ArrayList<>();
        try {
            MaterialExample example = new MaterialExample();
            example.createCriteria().andIdIn(idList);
            list = materialMapper.selectByExample(example);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<Material> getMaterial() throws Exception {
        MaterialExample example = new MaterialExample();
        example.createCriteria().andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Material> list = null;
        try {
            list = materialMapper.selectByExample(example);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<MaterialVo4Unit> select(String barCode, String name, String standard, String model, String categoryId, String mpList, int offset, int rows)
            throws Exception {
        String[] mpArr = new String[]{};
        if (StringUtil.isNotEmpty(mpList)) {
            mpArr = mpList.split(",");
        }
        List<MaterialVo4Unit> resList = new ArrayList<>();
        List<MaterialVo4Unit> list = null;
        try {
            List<Long> idList = new ArrayList<>();
            if (StringUtil.isNotEmpty(categoryId)) {
                idList = getListByParentId(Long.parseLong(categoryId));
            }
            list = materialMapperEx.selectByConditionMaterial(barCode, name, standard, model, idList, mpList, offset, rows);

            if (!CollectionUtils.isEmpty(list)) {
                //获取商品ids
                Set<Long> materialIds = list.stream().map(MaterialVo4Unit::getId).collect(Collectors.toSet());

//                Map<Long, List<Supplier>> supplierInfoListMap = getMaterialSupplierMap(materialIds);

                //链接
                Map<Long, List<String>> linkMap = getMaterialLinkMap(materialIds);

                for (MaterialVo4Unit m : list) {
                    //扩展信息
                    String materialOther = "";
                    for (int i = 0; i < mpArr.length; i++) {
                        if (mpArr[i].equals("制造商")) {
                            materialOther = materialOther + ((m.getMfrs() == null || m.getMfrs().equals("")) ? "" : "(" + m.getMfrs() + ")");
                        }
                        if (mpArr[i].equals("自定义1")) {
                            materialOther = materialOther + ((m.getOtherField1() == null || m.getOtherField1().equals("")) ? "" : "(" + m.getOtherField1() + ")");
                        }
                        if (mpArr[i].equals("自定义2")) {
                            materialOther = materialOther + ((m.getOtherField2() == null || m.getOtherField2().equals("")) ? "" : "(" + m.getOtherField2() + ")");
                        }
                        if (mpArr[i].equals("自定义3")) {
                            materialOther = materialOther + ((m.getOtherField3() == null || m.getOtherField3().equals("")) ? "" : "(" + m.getOtherField3() + ")");
                        }
                    }
                    m.setMaterialOther(materialOther);
                    m.setStock(depotItemService.getStockByParam(null, m.getId(), null, null));

                    //设置电商链接
                    List<String> links = linkMap.get(m.getId());
                    if (!CollectionUtils.isEmpty(links)) {
                        StringBuilder linkStr = new StringBuilder("");
                        links.stream().forEach(x -> {
                            if (StringUtil.isNotEmpty(linkStr.toString())) {
                                linkStr.append(",");
                            }
                            linkStr.append(x);

                        });
                        m.setLinks(linkStr.toString());
                    }

                    resList.add(m);
                }
            }

        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return resList;
    }


    //通过商品ids获取到对应供应商信息
//    private Map<Long, List<Supplier>> getMaterialSupplierMap(Collection<Long> materialIds) {
//        //商品id-供应商列表信息
//        Map<Long, List<Supplier>> supplierInfoListMap = new HashMap<>();
//        if (CollectionUtils.isEmpty(materialIds)) {
//            return supplierInfoListMap;
//        }
//
//        List<MaterialSupplier> materialSupplierList = materialSupplierMapper.queryByMaterialIds(materialIds);
//        //获取供应商map
//        Map<Long, Supplier> supplierInfoMap = new HashMap<>();
//
//        if (!CollectionUtils.isEmpty(materialSupplierList)) {
//            Set<Long> supplierIds = materialSupplierList.stream().map(MaterialSupplier::getSupplierId).collect(Collectors.toSet());
//            List<Supplier> suppliersInfos = supplierService.selectByIds(supplierIds);
//            supplierInfoMap = suppliersInfos.stream().filter(x -> "供应商".equals(x.getType())).collect(Collectors.toMap(Supplier::getId, x -> x));
//
//            for (MaterialSupplier materialSupplier : materialSupplierList) {
//                Long materialId = materialSupplier.getMaterialId();
//                Long supplierId = materialSupplier.getSupplierId();
//                //如果为空
//                if (CollectionUtils.isEmpty(supplierInfoListMap.get(materialId))) {
//                    List<Supplier> suppliers = new ArrayList<>();
//                    Supplier supplier = supplierInfoMap.get(supplierId);
//                    if (supplier!=null){
//                        suppliers.add(supplier);
//                    }
//                    if (!CollectionUtils.isEmpty(suppliers)){
//                        supplierInfoListMap.put(materialId, suppliers);
//                    }
//                } else {
//                    List<Supplier> suppliers = supplierInfoListMap.get(materialId);
//                    Supplier supplier = supplierInfoMap.get(supplierId);
//                    if (supplier!=null&&!suppliers.contains(supplier)) {
//                        suppliers.add(supplier);
//                    }
//                }
//            }
//        }
//        return supplierInfoListMap;
//    }

    //通过商品ids获取到对应电商链接map
    private Map<Long, List<String>> getMaterialLinkMap(Collection<Long> materialIds) {
        Map<Long, List<String>> linkMap = new HashMap<>();
        if (CollectionUtils.isEmpty(materialIds)) {
            return linkMap;
        }
        List<MaterialLink> materialLinks = materialLinkMapper.queryByMaterialIds(materialIds);

        if (CollectionUtils.isEmpty(materialLinks)) {
            return linkMap;
        }
        return materialLinks.stream().collect(Collectors.groupingBy(MaterialLink::getMaterialId, Collectors.mapping(MaterialLink::getLink, Collectors.toList())));
    }

    public Long countMaterial(String barCode, String name, String standard, String model, String categoryId, String mpList) throws Exception {
        Long result = null;
        try {
            List<Long> idList = new ArrayList<>();
            if (StringUtil.isNotEmpty(categoryId)) {
                idList = getListByParentId(Long.parseLong(categoryId));
            }
            result = materialMapperEx.countsByMaterial(barCode, name, standard, model, idList, mpList);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertMaterial(JSONObject obj, HttpServletRequest request) throws Exception {
        Material m = JSONObject.parseObject(obj.toJSONString(), Material.class);
        m.setEnabled(true);
        try {
            Long mId = null;
            materialMapper.insertSelective(m);
            List<Material> materials = getMaterialListByParam(m.getName(), m.getModel(), m.getColor(),
                    m.getStandard(), m.getMfrs(), m.getUnit(), m.getUnitId());
            if (materials != null && materials.size() > 0) {
                mId = materials.get(0).getId();
            }
            //商品-电商链接
            String linksStr = obj.getString("links");
            if (StringUtil.isNotEmpty(linksStr)) {
                List<String> linkList = JsonUtils.spiltToStringList(linksStr, ",");

                List<MaterialLink> mateialLinkList = new ArrayList<>();
                for (String link : linkList) {
                    MaterialLink materialLink = new MaterialLink();
                    materialLink.setMaterialId(mId);
                    materialLink.setLink(link);
                    materialLink.setCreateTime(new Date());
                    mateialLinkList.add(materialLink);
                }
                materialLinkMapper.batchInsert(mateialLinkList);
            }

            //价格扩展相关
            materialExtendService.saveDetials(obj, obj.getString("sortList"), mId, "insert");
//            if (obj.get("stock") != null) {
//                JSONArray stockArr = obj.getJSONArray("stock");
//                for (int i = 0; i < stockArr.size(); i++) {
//                    JSONObject jsonObj = stockArr.getJSONObject(i);
//                    if (jsonObj.get("id") != null && jsonObj.get("initStock") != null) {
//                        String number = jsonObj.getString("initStock");
//                        BigDecimal lowSafeStock = null;
//                        BigDecimal highSafeStock = null;
//                        if (jsonObj.get("lowSafeStock") != null) {
//                            lowSafeStock = jsonObj.getBigDecimal("lowSafeStock");
//                        }
//                        if (jsonObj.get("highSafeStock") != null) {
//                            highSafeStock = jsonObj.getBigDecimal("highSafeStock");
//                        }
//                        Long depotId = jsonObj.getLong("id");
//                        if (StringUtil.isNotEmpty(number) && Double.valueOf(number) > 0 || lowSafeStock != null || highSafeStock != null) {
//                            insertInitialStockByMaterialAndDepot(depotId, mId, parseBigDecimalEx(number), lowSafeStock, highSafeStock);
//                            insertCurrentStockByMaterialAndDepot(depotId, mId, parseBigDecimalEx(number));
//                        }
//                    }
//                }
//            }
            logService.insertLog("商品",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(m.getName()).toString(), request);
            return 1;
        } catch (BusinessRunTimeException ex) {
            throw new BusinessRunTimeException(ex.getCode(), ex.getMessage());
        } catch (Exception e) {
            JshException.writeFail(logger, e);
            return 0;
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateMaterial(JSONObject obj, HttpServletRequest request) throws Exception {
        Material material = JSONObject.parseObject(obj.toJSONString(), Material.class);
        try {
            materialMapper.updateByPrimaryKeySelective(material);
            if (material.getUnitId() == null) {
                materialMapperEx.setUnitIdToNull(material.getId());
            }
            if (material.getExpiryNum() == null) {
                materialMapperEx.setExpiryNumToNull(material.getId());
            }

            //设置供应商
//            String supplierStr = obj.getString("supplierList");
//            if (StringUtil.isNotEmpty(supplierStr)) {
//                List<Long> supplierIds = JsonUtils.StrToLongList(supplierStr);
//
//                List<MaterialSupplier> materialSupplierList = new ArrayList<>();
//                for (Long supplierId : supplierIds) {
//                    MaterialSupplier materialSupplier = new MaterialSupplier();
//                    materialSupplier.setMaterialId(material.getId());
//                    materialSupplier.setSupplierId(supplierId);
//                    materialSupplier.setCreateTime(new Date());
//                    materialSupplierList.add(materialSupplier);
//                }
//                materialSupplierMapper.batchInsert(materialSupplierList);
//            }

            //设置电商链接
            String linksStr = obj.getString("links");
            if (StringUtil.isNotEmpty(linksStr)) {
                List<String> linkList = JsonUtils.spiltToStringList(linksStr, ",");

                List<MaterialLink> mateialLinkList = new ArrayList<>();
                for (String link : linkList) {
                    MaterialLink materialLink = new MaterialLink();
                    materialLink.setMaterialId(material.getId());
                    materialLink.setLink(link);
                    materialLink.setCreateTime(new Date());
                    mateialLinkList.add(materialLink);
                }
                materialLinkMapper.batchInsert(mateialLinkList);
            }

            materialExtendService.saveDetials(obj, obj.getString("sortList"), material.getId(), "update");
            if (obj.get("stock") != null) {
                JSONArray stockArr = obj.getJSONArray("stock");
                for (int i = 0; i < stockArr.size(); i++) {
                    JSONObject jsonObj = stockArr.getJSONObject(i);
                    if (jsonObj.get("id") != null && jsonObj.get("initStock") != null) {
                        String number = jsonObj.getString("initStock");
                        BigDecimal lowSafeStock = null;
                        BigDecimal highSafeStock = null;
                        if (jsonObj.get("lowSafeStock") != null) {
                            lowSafeStock = jsonObj.getBigDecimal("lowSafeStock");
                        }
                        if (jsonObj.get("highSafeStock") != null) {
                            highSafeStock = jsonObj.getBigDecimal("highSafeStock");
                        }
                        Long depotId = jsonObj.getLong("id");
                        //初始库存-先清除再插入
                        MaterialInitialStockExample example = new MaterialInitialStockExample();
                        example.createCriteria().andMaterialIdEqualTo(material.getId()).andDepotIdEqualTo(depotId);
                        materialInitialStockMapper.deleteByExample(example);
                        if (StringUtil.isNotEmpty(number) && Double.parseDouble(number) != 0 || lowSafeStock != null || highSafeStock != null) {
                            insertInitialStockByMaterialAndDepot(depotId, material.getId(), parseBigDecimalEx(number), lowSafeStock, highSafeStock);
                        }
                        //更新当前库存
                        depotItemService.updateCurrentStockFun(material.getId(), depotId);
                    }
                }
            }
            logService.insertLog("商品",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(material.getName()).toString(), request);
            return 1;
        } catch (Exception e) {
            JshException.writeFail(logger, e);
            return 0;
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteMaterial(Long id, HttpServletRequest request) throws Exception {
        return batchDeleteMaterialByIds(id.toString());
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteMaterial(String ids, HttpServletRequest request) throws Exception {
        return batchDeleteMaterialByIds(ids);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteMaterialByIds(String ids) throws Exception {
        String[] idArray = ids.split(",");
        //校验单据子表	jsh_depot_item
        List<DepotItem> depotItemList = null;
        try {
            depotItemList = depotItemMapperEx.getDepotItemListListByMaterialIds(idArray);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        if (depotItemList != null && depotItemList.size() > 0) {
            logger.error("异常码[{}],异常提示[{}],参数,MaterialIds[{}]",
                    ExceptionConstants.DELETE_FORCE_CONFIRM_CODE, ExceptionConstants.DELETE_FORCE_CONFIRM_MSG, ids);
            throw new BusinessRunTimeException(ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,
                    ExceptionConstants.DELETE_FORCE_CONFIRM_MSG);
        }
        //记录日志
        StringBuffer sb = new StringBuffer();
        sb.append(BusinessConstants.LOG_OPERATION_TYPE_DELETE);
        List<Material> list = getMaterialListByIds(ids);
        for (Material material : list) {
            sb.append("[").append(material.getName()).append("]");
        }
        logService.insertLog("商品", sb.toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        User userInfo = userService.getCurrentUser();
        //校验通过执行删除操作
        try {
            //逻辑删除商品
            materialMapperEx.batchDeleteMaterialByIds(new Date(), userInfo == null ? null : userInfo.getId(), idArray);
            //逻辑删除商品价格扩展
            materialExtendMapperEx.batchDeleteMaterialExtendByMIds(idArray);
            return 1;
        } catch (Exception e) {
            JshException.writeFail(logger, e);
            return 0;
        }
    }

    public int checkIsNameExist(Long id, String name) throws Exception {
        MaterialExample example = new MaterialExample();
        example.createCriteria().andIdNotEqualTo(id).andNameEqualTo(name).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Material> list = null;
        try {
            list = materialMapper.selectByExample(example);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list == null ? 0 : list.size();
    }

    public int checkIsExist(Long id, String name, String model, String color, String standard, String mfrs,
                            String otherField1, String otherField2, String otherField3, String unit, Long unitId) throws Exception {
        return materialMapperEx.checkIsExist(id, name, model, color, standard, mfrs, otherField1,
                otherField2, otherField3, unit, unitId);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchSetStatus(Boolean status, String ids) throws Exception {
        logService.insertLog("商品",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(ids).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        List<Long> materialIds = StringUtil.strToLongList(ids);
        Material material = new Material();
        material.setEnabled(status);
        MaterialExample example = new MaterialExample();
        example.createCriteria().andIdIn(materialIds);
        int result = 0;
        try {
            result = materialMapper.updateByExampleSelective(material, example);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return result;
    }

    public Unit findUnit(Long mId) throws Exception {
        Unit unit = new Unit();
        try {
            List<Unit> list = materialMapperEx.findUnitList(mId);
            if (list != null && list.size() > 0) {
                unit = list.get(0);
            }
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return unit;
    }

    public List<MaterialVo4Unit> findById(Long id) throws Exception {
        List<MaterialVo4Unit> list = null;
        try {
            list = materialMapperEx.findById(id);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<MaterialVo4Unit> findByIdWithBarCode(Long meId) throws Exception {
        List<MaterialVo4Unit> list = null;
        try {
            list = materialMapperEx.findByIdWithBarCode(meId);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<Long> getListByParentId(Long parentId) {
        List<Long> idList = new ArrayList<Long>();
        List<MaterialCategory> list = materialCategoryMapperEx.getListByParentId(parentId);
        idList.add(parentId);
        if (list != null && list.size() > 0) {
            getIdListByParentId(idList, parentId);
        }
        return idList;
    }

    public List<Long> getIdListByParentId(List<Long> idList, Long parentId) {
        List<MaterialCategory> list = materialCategoryMapperEx.getListByParentId(parentId);
        if (list != null && list.size() > 0) {
            for (MaterialCategory mc : list) {
                idList.add(mc.getId());
                getIdListByParentId(idList, mc.getId());
            }
        }
        return idList;
    }

    public List<MaterialVo4Unit> findBySelectWithBarCode(Long categoryId, String q, Integer offset, Integer rows) throws Exception {
        List<MaterialVo4Unit> list = null;
        try {
            List<Long> idList = new ArrayList<>();
            if (categoryId != null) {
                Long parentId = categoryId;
                idList = getListByParentId(parentId);
            }
            if (StringUtil.isNotEmpty(q)) {
                q = q.replace("'", "");
            }
            list = materialMapperEx.findBySelectWithBarCode(idList, q, offset, rows);
            Set<Long> ids = list.stream().map(MaterialVo4Unit::getId).collect(Collectors.toSet());
            //电商链接
            Map<Long, List<String>> linkMap = getMaterialLinkMap(ids);

            list.stream().forEach(x -> {
                //获取到链接的数组
                List<String> links = linkMap.get(x.getId());
                if (!CollectionUtils.isEmpty(links)) {
                    //链接拼接为字符串
                    StringBuilder linkStr = new StringBuilder("");
                    links.stream().forEach(y -> {
                        if (StringUtil.isNotEmpty(linkStr.toString())) {
                            linkStr.append(",");
                        }
                        linkStr.append(y);

                    });
                    x.setLinks(linkStr.toString());
                }
            });
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    public int findBySelectWithBarCodeCount(Long categoryId, String q) throws Exception {
        int result = 0;
        try {
            List<Long> idList = new ArrayList<>();
            if (categoryId != null) {
                Long parentId = categoryId;
                idList = getListByParentId(parentId);
            }
            if (StringUtil.isNotEmpty(q)) {
                q = q.replace("'", "");
            }
            result = materialMapperEx.findBySelectWithBarCodeCount(idList, q);
        } catch (Exception e) {
            logger.error("异常码[{}],异常提示[{}],异常[{}]",
                    ExceptionConstants.DATA_READ_FAIL_CODE, ExceptionConstants.DATA_READ_FAIL_MSG, e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE,
                    ExceptionConstants.DATA_READ_FAIL_MSG);
        }
        return result;
    }

    public List<MaterialVo4Unit> findByAll(String barCode, String name, String standard, String model, String categoryId) throws Exception {
        List<MaterialVo4Unit> resList = new ArrayList<>();
        List<MaterialVo4Unit> list = null;
        try {
            List<Long> idList = new ArrayList<>();
            if (StringUtil.isNotEmpty(categoryId)) {
                idList = getListByParentId(Long.parseLong(categoryId));
            }
            list = materialMapperEx.findByAll(barCode, name, standard, model, idList);

            Set<Long> ids = list.stream().map(MaterialVo4Unit::getId).collect(Collectors.toSet());

            Map<Long, List<String>> linkMap = getMaterialLinkMap(ids);
            list.stream().forEach(x -> {
                //获取到链接的数组
                List<String> links = linkMap.get(x.getId());
                if (!CollectionUtils.isEmpty(links)) {
                    //链接拼接为字符串
                    StringBuilder linkStr = new StringBuilder("");
                    links.stream().forEach(y -> {
                        if (StringUtil.isNotEmpty(linkStr.toString())) {
                            linkStr.append(",");
                        }
                        linkStr.append(y);
                    });
                    x.setLinks(linkStr.toString());
                }
            });

        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        if (null != list) {
            for (MaterialVo4Unit m : list) {
                resList.add(m);
            }
        }
        return resList;
    }

    //todo 未修改代发价字段
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public BaseResponseInfo importExcel(Sheet src, HttpServletRequest request) throws Exception {
        BaseResponseInfo info = new BaseResponseInfo();
        try {
//            List<Depot> depotList = depotService.getDepot();
            List<MaterialWithInitStock> mList = new ArrayList<>();

            //获取当前租户Id
            String token = request.getHeader("X-Access-Token");
            Long tenantId = Tools.getTenantIdByToken(token);

            String links = "";
            byte[] imageBytes = null;
            String imageName = "_" + System.currentTimeMillis() + ".png";
            Map<Integer, byte[]> imgDataMap = new HashMap<>();
            for (int i = 3; i < src.getRows(); i++) {
                String name = ExcelUtils.getContent(src, i, 1); //名称
                String brand = ExcelUtils.getContent(src, i, 0); //名称

                String standard = ExcelUtils.getContent(src, i, 2); //规格
                String model = ExcelUtils.getContent(src, i, 3); //型号
                String color = ExcelUtils.getContent(src, i, 11); //颜色
                String categoryName = ExcelUtils.getContent(src, i, 5); //类别
                String unit = ExcelUtils.getContent(src, i, 6); //基本单位
                links = ExcelUtils.getContent(src, i, 10); //电商链接

                //校验名称、单位是否为空
                if (StringUtil.isNotEmpty(name) && StringUtil.isNotEmpty(unit)) {
                    MaterialWithInitStock m = new MaterialWithInitStock();
                    m.setName(name);
                    m.setStandard(standard);
                    m.setModel(model);
                    m.setColor(color);
                    m.setBrand(brand);
                    m.setEnabled(true);

                    //校验单位是否存在
                    m.setUnit(unit);

                    //设置图片路径
                    m.setImgName("material/" + tenantId + "/" + imageName);

                    Long categoryId = materialCategoryService.getCategoryIdByName(categoryName);
                    if (null != categoryId) {
                        m.setCategoryId(categoryId);
                    }

//                    String mutiRecord = ExcelUtils.getContent(src, i, 7); //是否多条记录（不同供应商有不同的价格）
                    String supplierName = ExcelUtils.getContent(src, i, 12); //供应商
                    String dropShippingDecimal = ExcelUtils.getContent(src, i, 7); //代发价
                    String purchaseDecimal = ExcelUtils.getContent(src, i, 8); //集采价
                    String commodityDecimal = ExcelUtils.getContent(src, i, 9); //市场零售价

                    imgDataMap = ExcelUtils.readPictureData(src); //图片map数据
                    imageBytes=imgDataMap.get(i);

                    //处理供应商
                    Long supplierId = null;
                    List<Supplier> supList = supplierService.findBySelectSupName(supplierName);
                    if (CollectionUtils.isEmpty(supList)) {
                        //新增此供应商 并获取到他的Id
                        Supplier newSupplier = new Supplier();
                        newSupplier.setSupplier(supplierName);
                        supplierMapper.insert(newSupplier);

                        List<Supplier> newSup = supplierService.findBySelectSupName(supplierName);
                        supplierId = newSup.get(0).getId();

                    } else {
                        //获取到供应商Id
                        supplierId = supList.get(0).getId();
                    }

                    //条码现在根据类别自动生成
                    String maxBarCode = this.getMaxBarCode(String.valueOf(categoryId));

                    JSONObject materialExObj = new JSONObject();
                    JSONObject priceObj = new JSONObject();
                    priceObj.put("commodityUnit", unit);
                    priceObj.put("purchaseDecimal", purchaseDecimal);
                    priceObj.put("dropShippingDecimal", dropShippingDecimal);
                    priceObj.put("commodityDecimal", commodityDecimal);
                    priceObj.put("barCode", Long.valueOf(maxBarCode) + 1);
                    priceObj.put("supplierId", supplierId);

                    materialExObj.put("price", priceObj);

                    m.setMaterialExObj(materialExObj);
                    mList.add(m);
                }
            }

            logService.insertLog("商品",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_IMPORT).append(mList.size()).append(BusinessConstants.LOG_DATA_UNIT).toString(),
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());

            Long mId = 0L;
            for (MaterialWithInitStock m : mList) {
                //判断该商品是否存在，如果不存在就新增，如果存在就更新
                List<Material> materials = getMaterialListByParam(m.getName(), m.getModel(), m.getColor(), m.getStandard(),
                        m.getMfrs(), m.getUnit(), m.getUnitId());

                //不存在
                if (materials.size() <= 0) {
                    materialMapper.insertSelective(m);
                    List<Material> newList = getMaterialListByParam(m.getName(), m.getModel(), null, m.getStandard(),
                            m.getMfrs(), m.getUnit(), m.getUnitId());
                    if (newList != null && newList.size() > 0) {
                        mId = newList.get(0).getId();
                    }
                } else {
                    //存在 更新商品基本信息
                    mId = materials.get(0).getId();
                    String materialJson = JSON.toJSONString(m);
                    Material material = JSONObject.parseObject(materialJson, Material.class);
                    material.setId(mId);
                    materialMapper.updateByPrimaryKeySelective(material);
                }

                //更新链接信息
                if (StringUtil.isNotEmpty(links)) {
                    List<String> linkList = JsonUtils.spiltToStringList(links, ",");
                    List<MaterialLink> mateialLinkList = new ArrayList<>();
                    for (String link : linkList) {
                        MaterialLink materialLink = new MaterialLink();
                        materialLink.setMaterialId(mId);
                        materialLink.setLink(link);
                        materialLink.setCreateTime(new Date());
                        mateialLinkList.add(materialLink);
                    }
                    materialLinkMapper.batchInsert(mateialLinkList);
                }

                //给商品新增条码与价格、供应商 相关信息
                User user = userService.getCurrentUser();
                JSONObject materialExObj = m.getMaterialExObj();
                if (StringUtil.isExist(materialExObj.get("price"))) {
                    String basicStr = materialExObj.getString("price");
                    MaterialExtend basicMaterialExtend = JSONObject.parseObject(basicStr, MaterialExtend.class);
                    basicMaterialExtend.setMaterialId(mId);
                    basicMaterialExtend.setDefaultFlag("1");
                    basicMaterialExtend.setCreateTime(new Date());
                    basicMaterialExtend.setUpdateTime(System.currentTimeMillis());
                    basicMaterialExtend.setCreateSerial(user.getLoginName());
                    basicMaterialExtend.setUpdateSerial(user.getLoginName());
                    //直接插入价格  如果有重复 可以在网页端删除
                    materialExtendMapper.insertSelective(basicMaterialExtend);
                }

                //商品图片
                if (imageBytes != null && imageBytes.length != 0) {
                    BufferedOutputStream bos = null;
                    FileOutputStream fos = null;
                    File file = null;
                    String path = filePath + File.separator + "material" + File.separator + tenantId + File.separator;

                    try {
                        File dir = new File(path);
                        if (!dir.exists() && dir.isDirectory()) {//判断文件目录是否存在
                            dir.mkdirs();
                        }
                        file = new File(path + File.separator + imageName);
                        fos = new FileOutputStream(file);
                        bos = new BufferedOutputStream(fos);
                        bos.write(imageBytes);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (bos != null) {
                            try {
                                bos.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }

                }

            }
            info.code = 200;
            info.data = "导入成功";
        } catch (BusinessRunTimeException e) {
            info.code = e.getCode();
            info.data = e.getData().get("message");
        } catch (Exception e) {
            e.printStackTrace();
            info.code = 500;
            info.data = "导入失败";
        }
        return info;
    }

    /**
     * 根据条件返回产品列表
     *
     * @param name
     * @param model
     * @param color
     * @param standard
     * @param mfrs
     * @param unit
     * @param unitId
     * @return
     */
    private List<Material> getMaterialListByParam(String name, String model, String color,
                                                  String standard, String mfrs, String unit, Long unitId) {
        MaterialExample example = new MaterialExample();
        MaterialExample.Criteria criteria = example.createCriteria();
        criteria.andNameEqualTo(name);
        if (StringUtil.isNotEmpty(model)) {
            criteria.andModelEqualTo(model);
        }
        if (StringUtil.isNotEmpty(color)) {
            criteria.andColorEqualTo(color);
        }
        if (StringUtil.isNotEmpty(standard)) {
            criteria.andStandardEqualTo(standard);
        }
        if (StringUtil.isNotEmpty(mfrs)) {
            criteria.andMfrsEqualTo(mfrs);
        }
        if (StringUtil.isNotEmpty(unit)) {
            criteria.andUnitEqualTo(unit);
        }
        if (unitId != null) {
            criteria.andUnitIdEqualTo(unitId);
        }
        criteria.andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Material> list = materialMapper.selectByExample(example);
        return list;
    }

    /**
     * 写入初始库存
     *
     * @param depotId
     * @param mId
     * @param stock
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void insertInitialStockByMaterialAndDepot(Long depotId, Long mId, BigDecimal stock, BigDecimal lowSafeStock, BigDecimal highSafeStock) {
        MaterialInitialStock materialInitialStock = new MaterialInitialStock();
        materialInitialStock.setDepotId(depotId);
        materialInitialStock.setMaterialId(mId);
        stock = stock == null ? BigDecimal.ZERO : stock;
        materialInitialStock.setNumber(stock);
        if (lowSafeStock != null) {
            materialInitialStock.setLowSafeStock(lowSafeStock);
        }
        if (highSafeStock != null) {
            materialInitialStock.setHighSafeStock(highSafeStock);
        }
        materialInitialStockMapper.insertSelective(materialInitialStock); //存入初始库存
    }

    /**
     * 写入当前库存
     *
     * @param depotId
     * @param mId
     * @param stock
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void insertCurrentStockByMaterialAndDepot(Long depotId, Long mId, BigDecimal stock) {
        MaterialCurrentStock materialCurrentStock = new MaterialCurrentStock();
        materialCurrentStock.setDepotId(depotId);
        materialCurrentStock.setMaterialId(mId);
        materialCurrentStock.setCurrentNumber(stock);
        materialCurrentStockMapper.insertSelective(materialCurrentStock); //存入初始库存
    }

    public List<MaterialVo4Unit> getMaterialEnableSerialNumberList(String q, Integer offset, Integer rows) throws Exception {
        List<MaterialVo4Unit> list = null;
        try {
            list = materialMapperEx.getMaterialEnableSerialNumberList(q, offset, rows);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    public Long getMaterialEnableSerialNumberCount(String q) throws Exception {
        Long count = null;
        try {
            count = materialMapperEx.getMaterialEnableSerialNumberCount(q);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return count;
    }

    public BigDecimal parseBigDecimalEx(String str) throws Exception {
        if (!StringUtil.isEmpty(str)) {
            return new BigDecimal(str);
        } else {
            return null;
        }
    }

    public BigDecimal parsePrice(String price, String ratio) throws Exception {
        if (StringUtil.isEmpty(price) || StringUtil.isEmpty(ratio)) {
            return BigDecimal.ZERO;
        } else {
            BigDecimal pr = new BigDecimal(price);
            BigDecimal r = new BigDecimal(ratio);
            return pr.multiply(r);
        }
    }

    /**
     * 根据商品获取初始库存-多仓库
     *
     * @param depotList
     * @param materialId
     * @return
     */
    public BigDecimal getInitStockByMidAndDepotList(List<Long> depotList, Long materialId) {
        BigDecimal stock = BigDecimal.ZERO;
        MaterialInitialStockExample example = new MaterialInitialStockExample();
        if (depotList != null && depotList.size() > 0) {
            example.createCriteria().andMaterialIdEqualTo(materialId).andDepotIdIn(depotList)
                    .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        } else {
            example.createCriteria().andMaterialIdEqualTo(materialId)
                    .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        }
        List<MaterialInitialStock> list = materialInitialStockMapper.selectByExample(example);
        if (list != null && list.size() > 0) {
            for (MaterialInitialStock ms : list) {
                if (ms != null) {
                    stock = stock.add(ms.getNumber());
                }
            }
        }
        return stock;
    }

    /**
     * 根据商品和仓库获取初始库存
     *
     * @param materialId
     * @param depotId
     * @return
     */
    public BigDecimal getInitStock(Long materialId, Long depotId) {
        BigDecimal stock = BigDecimal.ZERO;
        MaterialInitialStockExample example = new MaterialInitialStockExample();
        example.createCriteria().andMaterialIdEqualTo(materialId).andDepotIdEqualTo(depotId)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<MaterialInitialStock> list = materialInitialStockMapper.selectByExample(example);
        if (list != null && list.size() > 0) {
            stock = list.get(0).getNumber();
        }
        return stock;
    }

    /**
     * 根据商品和仓库获取当前库存
     *
     * @param materialId
     * @param depotId
     * @return
     */
    public BigDecimal getCurrentStock(Long materialId, Long depotId) {
        BigDecimal stock = BigDecimal.ZERO;
        MaterialCurrentStockExample example = new MaterialCurrentStockExample();
        example.createCriteria().andMaterialIdEqualTo(materialId).andDepotIdEqualTo(depotId)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<MaterialCurrentStock> list = materialCurrentStockMapper.selectByExample(example);
        if (list != null && list.size() > 0) {
            stock = list.get(0).getCurrentNumber();
        } else {
            stock = getInitStock(materialId, depotId);
        }
        return stock;
    }

    /**
     * 根据商品和仓库获取安全库存信息
     *
     * @param materialId
     * @param depotId
     * @return
     */
    public MaterialInitialStock getSafeStock(Long materialId, Long depotId) {
        MaterialInitialStock materialInitialStock = new MaterialInitialStock();
        MaterialInitialStockExample example = new MaterialInitialStockExample();
        example.createCriteria().andMaterialIdEqualTo(materialId).andDepotIdEqualTo(depotId)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<MaterialInitialStock> list = materialInitialStockMapper.selectByExample(example);
        if (list != null && list.size() > 0) {
            materialInitialStock = list.get(0);
        }
        return materialInitialStock;
    }

    public List<MaterialVo4Unit> getMaterialByMeId(Long meId) {
        List<MaterialVo4Unit> result = new ArrayList<MaterialVo4Unit>();
        try {
            if (meId != null) {
                result = materialMapperEx.getMaterialByMeId(meId);
            }
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return result;
    }

    public String getMaxBarCode(String categoryId) throws Exception {
        if (StringUtil.isEmpty(categoryId)) {
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_CATEGORY_EMPTY_CODE, ExceptionConstants.MATERIAL_CATEGORY_EMPTY_MSG);
        }
        //获取到该分类对应的编码
        MaterialCategory materialCategory = materialCategoryService.getMaterialCategory(Long.valueOf(categoryId));
        String maxBarCodeOld = materialMapperEx.getMaxBarCodeByCategoryNum(materialCategory.getSerialNo());
        int length = materialCategory.getSerialNo().length() + 6;
        if (StringUtil.isNotEmpty(maxBarCodeOld) && maxBarCodeOld.length() == length) {
            return Long.parseLong(maxBarCodeOld) + "";
        } else {
            return materialCategory.getSerialNo() + "000000";
        }
    }

    public List<String> getMaterialNameList() {
        return materialMapperEx.getMaterialNameList();
    }

    public List<MaterialVo4Unit> getMaterialByBarCode(String barCode) {
        String[] barCodeArray = barCode.split(",");
        List<MaterialVo4Unit> list = materialMapperEx.getMaterialByBarCode(barCodeArray);
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        Set<Long> ids = list.stream().map(MaterialVo4Unit::getId).collect(Collectors.toSet());

        Map<Long, List<String>> linkMap = getMaterialLinkMap(ids);
        list.stream().forEach(x -> {
            //获取到链接的数组
            List<String> links = linkMap.get(x.getId());
            if (!CollectionUtils.isEmpty(links)) {
                //链接拼接为字符串
                StringBuilder linkStr = new StringBuilder("");
                links.stream().forEach(y -> {
                    if (StringUtil.isNotEmpty(linkStr.toString())) {
                        linkStr.append(",");
                    }
                    linkStr.append(y);
                });
                x.setLinks(linkStr.toString());
            }
        });
        return list;
    }

    public List<MaterialVo4Unit> getListWithStock(List<Long> depotList, List<Long> idList, String materialParam, Integer zeroStock,
                                                  String column, String order, Integer offset, Integer rows) {
        return materialMapperEx.getListWithStock(depotList, idList, materialParam, zeroStock, column, order, offset, rows);
    }

    public int getListWithStockCount(List<Long> depotList, List<Long> idList, String materialParam, Integer zeroStock) {
        return materialMapperEx.getListWithStockCount(depotList, idList, materialParam, zeroStock);
    }

    public MaterialVo4Unit getTotalStockAndPrice(List<Long> depotList, List<Long> idList, String materialParam) {
        return materialMapperEx.getTotalStockAndPrice(depotList, idList, materialParam);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchSetMaterialCurrentStock(String ids) throws Exception {
        int res = 0;
        List<Long> idList = StringUtil.strToLongList(ids);
        List<Depot> depotList = depotService.getAllList();
        for (Long mId : idList) {
            for (Depot depot : depotList) {
                depotItemService.updateCurrentStockFun(mId, depot.getId());
                res = 1;
            }
        }
        return res;
    }
}
