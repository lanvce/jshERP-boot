package com.jsh.erp.service.depot;

import cn.afterturn.easypoi.entity.ImageEntity;
import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.TemplateExportParams;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.*;
import com.jsh.erp.datasource.mappers.*;
import com.jsh.erp.datasource.vo.DepotItemStockWarningCount;
import com.jsh.erp.datasource.vo.DepotItemVo4Stock;
import com.jsh.erp.datasource.vo.DepotItemVoBatchNumberList;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.service.auth.supplier.SupplierService;
import com.jsh.erp.service.material.MaterialExtendService;
import com.jsh.erp.service.system.log.LogService;
import com.jsh.erp.service.material.MaterialService;
import com.jsh.erp.service.system.serialNumber.SerialNumberService;
import com.jsh.erp.service.system.systemConfig.SystemConfigService;
import com.jsh.erp.service.auth.user.UserService;
import com.jsh.erp.utils.EmailUtil;
import com.jsh.erp.utils.QueryUtils;
import com.jsh.erp.utils.StringUtil;
import com.jsh.erp.utils.Tools;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DepotItemService {
    private Logger logger = LoggerFactory.getLogger(DepotItemService.class);

    private final static String TYPE = "入库";
    private final static String SUM_TYPE = "number";
    private final static String IN = "in";
    private final static String OUT = "out";

    @Value(value = "${file.path}")
    private String imgPath;

    @Value(value="${template.path}")
    private String templatePath;

    @Resource
    private DepotItemMapper depotItemMapper;
    @Resource
    private DepotItemMapperEx depotItemMapperEx;
    @Resource
    private MaterialService materialService;
    @Resource
    private MaterialExtendService materialExtendService;
    @Resource
    SerialNumberMapperEx serialNumberMapperEx;
    @Resource
    private DepotHeadMapper depotHeadMapper;
    @Resource
    SerialNumberService serialNumberService;
    @Resource
    private UserService userService;
    @Resource
    private SystemConfigService systemConfigService;
    @Resource
    private MaterialCurrentStockMapper materialCurrentStockMapper;
    @Resource
    private LogService logService;
    @Resource
    private MaterialLinkMapper materialLinkMapper;

    @Resource
    private SupplierService supplierService;

    public DepotItem getDepotItem(long id) throws Exception {
        DepotItem result = null;
        try {
            result = depotItemMapper.selectByPrimaryKey(id);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return result;
    }

    public List<DepotItem> getDepotItem() throws Exception {
        DepotItemExample example = new DepotItemExample();
        example.createCriteria().andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<DepotItem> list = null;
        try {
            list = depotItemMapper.selectByExample(example);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<DepotItem> select(String name, Integer type, String remark, int offset, int rows) throws Exception {
        List<DepotItem> list = null;
        try {
            list = depotItemMapperEx.selectByConditionDepotItem(name, type, remark, offset, rows);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    public Long countDepotItem(String name, Integer type, String remark) throws Exception {
        Long result = null;
        try {
            result = depotItemMapperEx.countsByDepotItem(name, type, remark);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertDepotItem(JSONObject obj, HttpServletRequest request) throws Exception {
        DepotItem depotItem = JSONObject.parseObject(obj.toJSONString(), DepotItem.class);
        int result = 0;
        try {
            result = depotItemMapper.insertSelective(depotItem);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateDepotItem(JSONObject obj, HttpServletRequest request) throws Exception {
        DepotItem depotItem = JSONObject.parseObject(obj.toJSONString(), DepotItem.class);
        int result = 0;
        try {
            result = depotItemMapper.updateByPrimaryKeySelective(depotItem);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteDepotItem(Long id, HttpServletRequest request) throws Exception {
        int result = 0;
        try {
            result = depotItemMapper.deleteByPrimaryKey(id);
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteDepotItem(String ids, HttpServletRequest request) throws Exception {
        List<Long> idList = StringUtil.strToLongList(ids);
        DepotItemExample example = new DepotItemExample();
        example.createCriteria().andIdIn(idList);
        int result = 0;
        try {
            result = depotItemMapper.deleteByExample(example);
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public int checkIsNameExist(Long id, String name) throws Exception {
        DepotItemExample example = new DepotItemExample();
        example.createCriteria().andIdNotEqualTo(id).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<DepotItem> list = null;
        try {
            list = depotItemMapper.selectByExample(example);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list == null ? 0 : list.size();
    }

    public List<DepotItemVo4DetailByTypeAndMId> findDetailByTypeAndMaterialIdList(Map<String, String> map) throws Exception {
        String mIdStr = map.get("mId");
        Long mId = null;
        if (!StringUtil.isEmpty(mIdStr)) {
            mId = Long.parseLong(mIdStr);
        }
        List<DepotItemVo4DetailByTypeAndMId> list = null;
        try {
            list = depotItemMapperEx.findDetailByTypeAndMaterialIdList(mId, QueryUtils.offset(map), QueryUtils.rows(map));
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    public Long findDetailByTypeAndMaterialIdCounts(Map<String, String> map) throws Exception {
        String mIdStr = map.get("mId");
        Long mId = null;
        if (!StringUtil.isEmpty(mIdStr)) {
            mId = Long.parseLong(mIdStr);
        }
        Long result = null;
        try {
            result = depotItemMapperEx.findDetailByTypeAndMaterialIdCounts(mId);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertDepotItemWithObj(DepotItem depotItem) throws Exception {
        int result = 0;
        try {
            result = depotItemMapper.insertSelective(depotItem);
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateDepotItemWithObj(DepotItem depotItem) throws Exception {
        int result = 0;
        try {
            result = depotItemMapper.updateByPrimaryKeySelective(depotItem);
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public List<DepotItem> getListByHeaderId(Long headerId) throws Exception {
        List<DepotItem> list = null;
        try {
            DepotItemExample example = new DepotItemExample();
            example.createCriteria().andHeaderIdEqualTo(headerId);
            list = depotItemMapper.selectByExample(example);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<DepotItemVo4WithInfoEx> getDetailList(Long headerId) throws Exception {
        List<DepotItemVo4WithInfoEx> list = null;
        try {
            list = depotItemMapperEx.getDetailList(headerId);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<DepotItemVo4WithInfoEx> findByAll(String materialParam, String endTime, Integer offset, Integer rows) throws Exception {
        List<DepotItemVo4WithInfoEx> list = null;
        try {
            list = depotItemMapperEx.findByAll(materialParam, endTime, offset, rows);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    public int findByAllCount(String materialParam, String endTime) throws Exception {
        int result = 0;
        try {
            result = depotItemMapperEx.findByAllCount(materialParam, endTime);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return result;
    }

    public BigDecimal buyOrSale(String type, String subType, Long MId, String monthTime, String sumType) throws Exception {
        BigDecimal result = BigDecimal.ZERO;
        try {
            String beginTime = Tools.firstDayOfMonth(monthTime) + BusinessConstants.DAY_FIRST_TIME;
            String endTime = Tools.lastDayOfMonth(monthTime) + BusinessConstants.DAY_LAST_TIME;
            if (SUM_TYPE.equals(sumType)) {
                result = depotItemMapperEx.buyOrSaleNumber(type, subType, MId, beginTime, endTime, sumType);
            } else {
                result = depotItemMapperEx.buyOrSalePrice(type, subType, MId, beginTime, endTime, sumType);
            }
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return result;

    }

    /**
     * 统计采购或销售的总金额
     *
     * @param type
     * @param subType
     * @param month
     * @return
     * @throws Exception
     */
    public BigDecimal inOrOutPrice(String type, String subType, String month) throws Exception {
        BigDecimal result = BigDecimal.ZERO;
        try {
            String beginTime = Tools.firstDayOfMonth(month) + BusinessConstants.DAY_FIRST_TIME;
            String endTime = Tools.lastDayOfMonth(month) + BusinessConstants.DAY_LAST_TIME;
            result = depotItemMapperEx.inOrOutPrice(type, subType, beginTime, endTime);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return result;
    }

    /**
     * 统计零售的总金额
     *
     * @param type
     * @param subType
     * @param month
     * @return
     * @throws Exception
     */
    public BigDecimal inOrOutRetailPrice(String type, String subType, String month) throws Exception {
        BigDecimal result = BigDecimal.ZERO;
        try {
            String beginTime = Tools.firstDayOfMonth(month) + BusinessConstants.DAY_FIRST_TIME;
            String endTime = Tools.lastDayOfMonth(month) + BusinessConstants.DAY_LAST_TIME;
            result = depotItemMapperEx.inOrOutRetailPrice(type, subType, beginTime, endTime);
            result = result.abs();
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void saveDetials(String rows, Long headerId, HttpServletRequest request) throws Exception {
        //查询单据主表信息
        DepotHead depotHead = depotHeadMapper.selectByPrimaryKey(headerId);
        //获得当前操作人
        User userInfo = userService.getCurrentUser();
        //首先回收序列号，如果是调拨，不用处理序列号
        if (BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType())
                && !BusinessConstants.SUB_TYPE_TRANSFER.equals(depotHead.getSubType())) {
            List<DepotItem> depotItemList = getListByHeaderId(headerId);
            for (DepotItem depotItem : depotItemList) {
                Material material = materialService.getMaterial(depotItem.getMaterialId());
                if (material == null) {
                    continue;
                }
                if (BusinessConstants.ENABLE_SERIAL_NUMBER_ENABLED.equals(material.getEnableSerialNumber())) {
                    serialNumberService.cancelSerialNumber(depotItem.getMaterialId(), depotHead.getNumber(),
                            (depotItem.getBasicNumber() == null ? 0 : depotItem.getBasicNumber()).intValue(), userInfo);
                }
            }
        }
        //删除单据的明细
        deleteDepotItemHeadId(headerId);
        //单据状态:是否全部完成 2-全部完成 3-部分完成（针对订单的分批出入库）
        String billStatus = BusinessConstants.BILLS_STATUS_SKIPED;
        JSONArray rowArr = JSONArray.parseArray(rows);
        if (null != rowArr && rowArr.size() > 0) {
            for (int i = 0; i < rowArr.size(); i++) {
                DepotItem depotItem = new DepotItem();
                JSONObject rowObj = JSONObject.parseObject(rowArr.getString(i));
                depotItem.setHeaderId(headerId);
                String barCode = rowObj.getString("barCode");
                MaterialExtend materialExtend = materialExtendService.getInfoByBarCode(barCode);
                depotItem.setMaterialId(materialExtend.getMaterialId());
                depotItem.setMaterialExtendId(materialExtend.getId());
                depotItem.setMaterialUnit(rowObj.getString("unit"));
                if (StringUtil.isExist(rowObj.get("snList"))) {
                    depotItem.setSnList(rowObj.getString("snList"));
                    if (StringUtil.isExist(rowObj.get("depotId"))) {
                        Long depotId = rowObj.getLong("depotId");
                        if (BusinessConstants.SUB_TYPE_PURCHASE.equals(depotHead.getSubType()) ||
                                BusinessConstants.SUB_TYPE_SALES_RETURN.equals(depotHead.getSubType())) {
                            serialNumberService.addSerialNumberByBill(depotHead.getNumber(), materialExtend.getMaterialId(), depotId, depotItem.getSnList());
                        }
                    }
                }
                if (StringUtil.isExist(rowObj.get("batchNumber"))) {
                    depotItem.setBatchNumber(rowObj.getString("batchNumber"));
                }
                if (StringUtil.isExist(rowObj.get("expirationDate"))) {
                    depotItem.setExpirationDate(rowObj.getDate("expirationDate"));
                }
                if (StringUtil.isExist(rowObj.get("sku"))) {
                    depotItem.setSku(rowObj.getString("sku"));
                }
                if (StringUtil.isExist(rowObj.get("operNumber"))) {
                    depotItem.setOperNumber(rowObj.getBigDecimal("operNumber"));
                    String unit = rowObj.get("unit").toString();
                    BigDecimal oNumber = rowObj.getBigDecimal("operNumber");
                    //以下进行单位换算
                    Unit unitInfo = materialService.findUnit(materialExtend.getMaterialId()); //查询计量单位信息
                    if (StringUtil.isNotEmpty(unitInfo.getName())) {
                        String basicUnit = unitInfo.getBasicUnit(); //基本单位
                        if (unit.equals(basicUnit)) { //如果等于基本单位
                            depotItem.setBasicNumber(oNumber); //数量一致
                        } else if (unit.equals(unitInfo.getOtherUnit())) { //如果等于副单位
                            depotItem.setBasicNumber(oNumber.multiply(new BigDecimal(unitInfo.getRatio()))); //数量乘以比例
                        } else if (unit.equals(unitInfo.getOtherUnitTwo())) { //如果等于副单位2
                            depotItem.setBasicNumber(oNumber.multiply(new BigDecimal(unitInfo.getRatioTwo()))); //数量乘以比例
                        } else if (unit.equals(unitInfo.getOtherUnitThree())) { //如果等于副单位3
                            depotItem.setBasicNumber(oNumber.multiply(new BigDecimal(unitInfo.getRatioThree()))); //数量乘以比例
                        }
                    } else {
                        depotItem.setBasicNumber(oNumber); //其他情况
                    }
                }
                //如果数量+已完成数量<原订单数量，代表该单据状态为未全部完成出入库(判断前提是存在关联订单)
                if (StringUtil.isNotEmpty(depotHead.getLinkNumber())
                        && StringUtil.isExist(rowObj.get("preNumber")) && StringUtil.isExist(rowObj.get("finishNumber"))) {
                    BigDecimal preNumber = rowObj.getBigDecimal("preNumber");
                    BigDecimal finishNumber = rowObj.getBigDecimal("finishNumber");
                    if (depotItem.getOperNumber().add(finishNumber).compareTo(preNumber) < 0) {
                        billStatus = BusinessConstants.BILLS_STATUS_SKIPING;
                    } else if (depotItem.getOperNumber().add(finishNumber).compareTo(preNumber) > 0) {
                        throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_NUMBER_NEED_EDIT_FAILED_CODE,
                                String.format(ExceptionConstants.DEPOT_HEAD_NUMBER_NEED_EDIT_FAILED_MSG, barCode));
                    }
                }
                if (StringUtil.isExist(rowObj.get("unitPrice"))) {
                    depotItem.setUnitPrice(rowObj.getBigDecimal("unitPrice"));
                }
                if (StringUtil.isExist(rowObj.get("taxUnitPrice"))) {
                    depotItem.setTaxUnitPrice(rowObj.getBigDecimal("taxUnitPrice"));
                }
                if (StringUtil.isExist(rowObj.get("allPrice"))) {
                    depotItem.setAllPrice(rowObj.getBigDecimal("allPrice"));
                }
                if (StringUtil.isExist(rowObj.get("depotId"))) {
                    depotItem.setDepotId(rowObj.getLong("depotId"));
                } else {
                    if (!BusinessConstants.SUB_TYPE_PURCHASE_ORDER.equals(depotHead.getSubType())
                            && !BusinessConstants.SUB_TYPE_SALES_ORDER.equals(depotHead.getSubType())) {
                        throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DEPOT_FAILED_CODE,
                                String.format(ExceptionConstants.DEPOT_HEAD_DEPOT_FAILED_MSG));
                    }
                }
                if (BusinessConstants.SUB_TYPE_TRANSFER.equals(depotHead.getSubType())) {
                    if (StringUtil.isExist(rowObj.get("anotherDepotId"))) {
                        if (rowObj.getLong("anotherDepotId").equals(rowObj.getLong("depotId"))) {
                            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ANOTHER_DEPOT_EQUAL_FAILED_CODE,
                                    String.format(ExceptionConstants.DEPOT_HEAD_ANOTHER_DEPOT_EQUAL_FAILED_MSG));
                        } else {
                            depotItem.setAnotherDepotId(rowObj.getLong("anotherDepotId"));
                        }
                    } else {
                        throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ANOTHER_DEPOT_FAILED_CODE,
                                String.format(ExceptionConstants.DEPOT_HEAD_ANOTHER_DEPOT_FAILED_MSG));
                    }
                }
                if (StringUtil.isExist(rowObj.get("taxRateType"))) {
                    depotItem.setTaxRateType(rowObj.getIntValue("taxRateType"));
                }
                if (StringUtil.isExist(rowObj.get("taxRate"))) {
                    depotItem.setTaxRate(rowObj.getBigDecimal("taxRate"));
                }
                if (StringUtil.isExist(rowObj.get("taxMoney"))) {
                    depotItem.setTaxMoney(rowObj.getBigDecimal("taxMoney"));
                }
                if (StringUtil.isExist(rowObj.get("taxLastMoney"))) {
                    depotItem.setTaxLastMoney(rowObj.getBigDecimal("taxLastMoney"));
                }
                if (StringUtil.isExist(rowObj.get("mType"))) {
                    depotItem.setMaterialType(rowObj.getString("mType"));
                }
                if (StringUtil.isExist(rowObj.get("remark"))) {
                    depotItem.setRemark(rowObj.getString("remark"));
                }
                if (StringUtil.isExist(rowObj.get("supplierId"))) {
                    depotItem.setSupplier(rowObj.getLong("supplierId"));
                }

                //出库时判断库存是否充足
//                if (BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType())) {
//                    if (depotItem == null) {
//                        continue;
//                    }
//                    Material material = materialService.getMaterial(depotItem.getMaterialId());
//                    if (material == null) {
//                        continue;
//                    }
//                    BigDecimal stock = getStockByParam(depotItem.getDepotId(), depotItem.getMaterialId(), null, null);
//                    BigDecimal thisBasicNumber = depotItem.getBasicNumber() == null ? BigDecimal.ZERO : depotItem.getBasicNumber();
//                    if (systemConfigService.getMinusStockFlag() == false && stock.compareTo(thisBasicNumber) < 0) {
//                        throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_STOCK_NOT_ENOUGH_CODE,
//                                String.format(ExceptionConstants.MATERIAL_STOCK_NOT_ENOUGH_MSG, material == null ? "" : material.getName()));
//                    }
//                    //出库时处理序列号
//                    if (!BusinessConstants.SUB_TYPE_TRANSFER.equals(depotHead.getSubType())) {
//                        //判断商品是否开启序列号，开启的收回序列号，未开启的跳过
//                        if (BusinessConstants.ENABLE_SERIAL_NUMBER_ENABLED.equals(material.getEnableSerialNumber())) {
//                            //查询单据子表中开启序列号的数据列表
//                            serialNumberService.checkAndUpdateSerialNumber(depotItem, depotHead.getNumber(), userInfo, StringUtil.toNull(depotItem.getSnList()));
//                        }
//                    }
//                }
                this.insertDepotItemWithObj(depotItem);
                //更新当前库存
//                updateCurrentStock(depotItem);
            }
            //如果关联单据号非空则更新订单的状态,单据类型：采购入库单或销售出库单
            if (BusinessConstants.SUB_TYPE_PURCHASE.equals(depotHead.getSubType())
                    || BusinessConstants.SUB_TYPE_SALES.equals(depotHead.getSubType())) {
                if (StringUtil.isNotEmpty(depotHead.getLinkNumber())) {
                    changeBillStatus(depotHead, billStatus);
                }
            }
        } else {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_CODE,
                    String.format(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_MSG));
        }
    }

    /**
     * 更新单据状态
     *
     * @param depotHead
     * @param billStatus
     */
    public void changeBillStatus(DepotHead depotHead, String billStatus) {
        DepotHead depotHeadOrders = new DepotHead();
        depotHeadOrders.setStatus(billStatus);
        DepotHeadExample example = new DepotHeadExample();
        List<String> linkNumberList = StringUtil.strToStringList(depotHead.getLinkNumber());
        example.createCriteria().andNumberIn(linkNumberList);
        try {
            depotHeadMapper.updateByExampleSelective(depotHeadOrders, example);
        } catch (Exception e) {
            logger.error("异常码[{}],异常提示[{}],异常[{}]",
                    ExceptionConstants.DATA_WRITE_FAIL_CODE, ExceptionConstants.DATA_WRITE_FAIL_MSG, e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE,
                    ExceptionConstants.DATA_WRITE_FAIL_MSG);
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void deleteDepotItemHeadId(Long headerId) throws Exception {
        try {
            //1、查询删除前的单据明细
            List<DepotItem> depotItemList = getListByHeaderId(headerId);
            //2、删除单据明细
            DepotItemExample example = new DepotItemExample();
            example.createCriteria().andHeaderIdEqualTo(headerId);
            depotItemMapper.deleteByExample(example);
            //3、计算删除之后单据明细中商品的库存
            for (DepotItem depotItem : depotItemList) {
                updateCurrentStock(depotItem);
            }
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public List<DepotItemStockWarningCount> findStockWarningCount(Integer offset, Integer rows, String materialParam, List<Long> depotList) {
        List<DepotItemStockWarningCount> list = null;
        try {
            list = depotItemMapperEx.findStockWarningCount(offset, rows, materialParam, depotList);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int findStockWarningCountTotal(String materialParam, List<Long> depotList) {
        int result = 0;
        try {
            result = depotItemMapperEx.findStockWarningCountTotal(materialParam, depotList);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return result;
    }

    /**
     * 库存统计-sku
     *
     * @param depotId
     * @param meId
     * @param beginTime
     * @param endTime
     * @return
     */
    public BigDecimal getSkuStockByParam(Long depotId, Long meId, String beginTime, String endTime) {
        DepotItemVo4Stock stockObj = depotItemMapperEx.getSkuStockByParam(depotId, meId, beginTime, endTime);
        BigDecimal stockSum = BigDecimal.ZERO;
        if (stockObj != null) {
            BigDecimal inTotal = stockObj.getInTotal();
            BigDecimal transfInTotal = stockObj.getTransfInTotal();
            BigDecimal assemInTotal = stockObj.getAssemInTotal();
            BigDecimal disAssemInTotal = stockObj.getDisAssemInTotal();
            BigDecimal outTotal = stockObj.getOutTotal();
            BigDecimal transfOutTotal = stockObj.getTransfOutTotal();
            BigDecimal assemOutTotal = stockObj.getAssemOutTotal();
            BigDecimal disAssemOutTotal = stockObj.getDisAssemOutTotal();
            stockSum = inTotal.add(transfInTotal).add(assemInTotal).add(disAssemInTotal)
                    .subtract(outTotal).subtract(transfOutTotal).subtract(assemOutTotal).subtract(disAssemOutTotal);
        }
        return stockSum;
    }

    /**
     * 库存统计-单仓库
     *
     * @param depotId
     * @param mId
     * @param beginTime
     * @param endTime
     * @return
     */
    public BigDecimal getStockByParam(Long depotId, Long mId, String beginTime, String endTime) {
        List<Long> depotList = new ArrayList<>();
        if (depotId != null) {
            depotList.add(depotId);
        }
        return getStockByParamWithDepotList(depotList, mId, beginTime, endTime);
    }

    /**
     * 库存统计-多仓库
     *
     * @param depotList
     * @param mId
     * @param beginTime
     * @param endTime
     * @return
     */
    public BigDecimal getStockByParamWithDepotList(List<Long> depotList, Long mId, String beginTime, String endTime) {
        //初始库存
        BigDecimal initStock = materialService.getInitStockByMidAndDepotList(depotList, mId);
        //盘点复盘后数量的变动
        BigDecimal stockCheckSum = depotItemMapperEx.getStockCheckSumByDepotList(depotList, mId, beginTime, endTime);
        DepotItemVo4Stock stockObj = depotItemMapperEx.getStockByParamWithDepotList(depotList, mId, beginTime, endTime);
        BigDecimal stockSum = BigDecimal.ZERO;
        if (stockObj != null) {
            BigDecimal inTotal = stockObj.getInTotal();
            BigDecimal transfInTotal = stockObj.getTransfInTotal();
            BigDecimal assemInTotal = stockObj.getAssemInTotal();
            BigDecimal disAssemInTotal = stockObj.getDisAssemInTotal();
            BigDecimal outTotal = stockObj.getOutTotal();
            BigDecimal transfOutTotal = stockObj.getTransfOutTotal();
            BigDecimal assemOutTotal = stockObj.getAssemOutTotal();
            BigDecimal disAssemOutTotal = stockObj.getDisAssemOutTotal();
            stockSum = inTotal.add(transfInTotal).add(assemInTotal).add(disAssemInTotal)
                    .subtract(outTotal).subtract(transfOutTotal).subtract(assemOutTotal).subtract(disAssemOutTotal);
        }
        return initStock.add(stockCheckSum).add(stockSum);
    }

    /**
     * 统计时间段内的入库和出库数量-多仓库
     *
     * @param depotList
     * @param mId
     * @param beginTime
     * @param endTime
     * @return
     */
    public Map<String, BigDecimal> getIntervalMapByParamWithDepotList(List<Long> depotList, Long mId, String beginTime, String endTime) {
        Map<String, BigDecimal> intervalMap = new HashMap<>();
        BigDecimal inSum = BigDecimal.ZERO;
        BigDecimal outSum = BigDecimal.ZERO;
        //盘点复盘后数量的变动
        BigDecimal stockCheckSum = depotItemMapperEx.getStockCheckSumByDepotList(depotList, mId, beginTime, endTime);
        DepotItemVo4Stock stockObj = depotItemMapperEx.getStockByParamWithDepotList(depotList, mId, beginTime, endTime);
        if (stockObj != null) {
            BigDecimal inTotal = stockObj.getInTotal();
            BigDecimal transfInTotal = stockObj.getTransfInTotal();
            BigDecimal assemInTotal = stockObj.getAssemInTotal();
            BigDecimal disAssemInTotal = stockObj.getDisAssemInTotal();
            inSum = inTotal.add(transfInTotal).add(assemInTotal).add(disAssemInTotal);
            BigDecimal outTotal = stockObj.getOutTotal();
            BigDecimal transfOutTotal = stockObj.getTransfOutTotal();
            BigDecimal assemOutTotal = stockObj.getAssemOutTotal();
            BigDecimal disAssemOutTotal = stockObj.getDisAssemOutTotal();
            outSum = outTotal.add(transfOutTotal).add(assemOutTotal).add(disAssemOutTotal);
        }
        if (stockCheckSum.compareTo(BigDecimal.ZERO) > 0) {
            inSum = inSum.add(stockCheckSum);
        } else {
            //盘点复盘数量为负数代表出库
            outSum = outSum.subtract(stockCheckSum);
        }
        intervalMap.put("inSum", inSum);
        intervalMap.put("outSum", outSum);
        return intervalMap;
    }

    /**
     * 根据单据明细来批量更新当前库存
     *
     * @param depotItem
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void updateCurrentStock(DepotItem depotItem) {
        updateCurrentStockFun(depotItem.getMaterialId(), depotItem.getDepotId());
        if (depotItem.getAnotherDepotId() != null) {
            updateCurrentStockFun(depotItem.getMaterialId(), depotItem.getAnotherDepotId());
        }
    }

    /**
     * 根据商品和仓库来更新当前库存
     *
     * @param mId
     * @param dId
     */
    public void updateCurrentStockFun(Long mId, Long dId) {
        if (mId != null && dId != null) {
            MaterialCurrentStockExample example = new MaterialCurrentStockExample();
            example.createCriteria().andMaterialIdEqualTo(mId).andDepotIdEqualTo(dId)
                    .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            List<MaterialCurrentStock> list = materialCurrentStockMapper.selectByExample(example);
            MaterialCurrentStock materialCurrentStock = new MaterialCurrentStock();
            materialCurrentStock.setMaterialId(mId);
            materialCurrentStock.setDepotId(dId);
            materialCurrentStock.setCurrentNumber(getStockByParam(dId, mId, null, null));
            if (list != null && list.size() > 0) {
                Long mcsId = list.get(0).getId();
                materialCurrentStock.setId(mcsId);
                materialCurrentStockMapper.updateByPrimaryKeySelective(materialCurrentStock);
            } else {
                materialCurrentStockMapper.insertSelective(materialCurrentStock);
            }
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public BigDecimal getFinishNumber(Long mId, Long headerId) {
        String goToType = "";
        DepotHead depotHead = depotHeadMapper.selectByPrimaryKey(headerId);
        String linkNumber = depotHead.getNumber(); //订单号
        if (BusinessConstants.SUB_TYPE_PURCHASE_ORDER.equals(depotHead.getSubType())) {
            goToType = BusinessConstants.SUB_TYPE_PURCHASE;
        }
        if (BusinessConstants.SUB_TYPE_SALES_ORDER.equals(depotHead.getSubType())) {
            goToType = BusinessConstants.SUB_TYPE_SALES;
        }
        BigDecimal count = depotItemMapperEx.getFinishNumber(mId, linkNumber, goToType);
        return count;
    }

    public List<DepotItemVoBatchNumberList> getBatchNumberList(String name, Long depotId, String barCode, String batchNumber) {
        return depotItemMapperEx.getBatchNumberList(name, depotId, barCode, batchNumber);
    }

    private String generateExcel(Long id) {
        if (id == null) {
            return Constants.EMPTY;
        }
        String targetFilePath = Constants.EMPTY;
        try {
            List<DepotItemVo4WithInfoEx> detailList = getDetailList(id);
            DepotHead depotHead = depotHeadMapper.selectByPrimaryKey(id);
            if (CollectionUtils.isEmpty(detailList) || depotHead == null) {
                return Constants.EMPTY;
            }

            //客户信息
            Long customerId = depotHead.getOrganId();
            Supplier customerInfo = new Supplier();
            if (customerId != null) {
                customerInfo = supplierService.getSupplier(depotHead.getOrganId());
            }

//            String templatePath = this.getClass().getClassLoader().getResource("template").getPath();

            System.out.println("templatePath:" + templatePath);
            TemplateExportParams params = new TemplateExportParams(templatePath + File.separator + "orderList.xlsx");
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("date", Tools.getNow());
            map.put("billId", depotHead.getDefaultNumber());
            map.put("billName", depotHead.getName());
            //客户名称  客户联系方式
            map.put("customer", customerInfo != null ? customerInfo.getSupplier() : "");
            map.put("contact", customerInfo != null ? customerInfo.getEmail() : "");

            List<Map<String, Object>> listMap = new ArrayList<Map<String, Object>>();
            int number = 0;
            BigDecimal sumCount = new BigDecimal(0);
            BigDecimal sumPrice = new BigDecimal(0);
            DecimalFormat df = new DecimalFormat("#0.00");

            //商品Ids
            Set<Long> mids = detailList.stream().map(DepotItemVo4WithInfoEx::getMaterialId).collect(Collectors.toSet());
            //商品在电商平台链接
            List<MaterialLink> materialLinks = materialLinkMapper.queryByMaterialIds(mids);
            Map<Long,List<String>> midLinkMap=new HashMap<>();
            if (!CollectionUtils.isEmpty(materialLinks)){
                midLinkMap=materialLinks.stream().collect(Collectors.groupingBy(MaterialLink::getMaterialId,Collectors.mapping(MaterialLink::getLink,Collectors.toList())));
            }

            for (DepotItemVo4WithInfoEx i : detailList) {
                number++;
                Map<String, Object> lm = new HashMap<String, Object>();
                lm.put("id", number);
                lm.put("material", i.getMName());
                lm.put("brand", i.getBrand());
                lm.put("model", i.getMModel());
                lm.put("quoteType", i.getQuoteType() == 1 ? "代发" : "集采");
                lm.put("count", df.format(i.getOperNumber()));
                lm.put("unitPrice", i.getUnitPrice() == null ? "0" : df.format(i.getUnitPrice()));

                if (i.getUnitPrice() == null || i.getOperNumber() == null) {
                    lm.put("price", "0");
                } else {
                    lm.put("price", df.format(i.getUnitPrice().multiply(i.getOperNumber())));
                    sumCount.add(i.getOperNumber());
                    sumPrice.add(i.getUnitPrice().multiply(i.getOperNumber()));
                }
                lm.put("sumCount", sumCount);
                lm.put("sumPrice", sumPrice);

                //图片地址
                ImageEntity image = new ImageEntity();
                image.setHeight(500);
                image.setWidth(500);
                String imgUrl = imgPath + File.separator + i.getImgName();
                if (StringUtil.isNotEmpty(imgUrl)) {
                    image.setUrl(imgUrl);
                    lm.put("img", image);
                }

                //商品链接
                List<String> links = midLinkMap.get(i.getMaterialId());
                lm.put("link",CollectionUtils.isEmpty(links)?"":links.get(0));

                //参数
                StringBuilder stringBuilder = new StringBuilder();
                String mStandard = i.getMStandard();
                String mMfrs = i.getMMfrs();
                String mColor = i.getMColor();
                if (StringUtil.isNotEmpty(mStandard)) {
                    stringBuilder.append("规格：" + mStandard).append(" ");
                }
                if (StringUtil.isNotEmpty(mMfrs)) {
                    stringBuilder.append("制造商：" + mMfrs).append(" ");
                }
                if (StringUtil.isNotEmpty(mColor)) {
                    stringBuilder.append("颜色：" + mColor).append(" ");
                }
                lm.put("param", stringBuilder.toString());

                listMap.add(lm);
            }
            map.put("list", listMap);

            Workbook workbook = ExcelExportUtil.exportExcel(params, map);
            String fileName = depotHead.getDefaultNumber() + ".xlsx";
            String targetPath = templatePath + File.separator + "target";
            File savefile = new File(targetPath);
            if (!savefile.exists()) {
                savefile.mkdirs();
            }
            File excelFile = new File(targetPath + File.separator + fileName);
            if (!excelFile.exists()) {
                excelFile.createNewFile();
            }

            FileOutputStream fos = new FileOutputStream(excelFile);

            workbook.write(fos);
            workbook.close();
            fos.close();

            targetFilePath = targetPath + File.separator + fileName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return targetFilePath;
    }


    public void downloadOrderExcel(Long id, HttpServletResponse response) throws IOException {
        if (id == null) {
            return;
        }
        DepotHead depotHead = depotHeadMapper.selectByPrimaryKey(id);
        if (depotHead == null) {
            return;
        }

        //生成的excel文件路径
        String filePath = generateExcel(id);
        File excelFile = new File(filePath);
        if (!excelFile.exists()) {
            return;
        }
        FileInputStream stream = new FileInputStream(excelFile);
        ServletOutputStream out = response.getOutputStream();

        response.setContentType("application/x-download");
        String fileName = URLEncoder.encode(depotHead.getDefaultNumber(), "UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.addHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

        byte buff[] = new byte[1024];
        int length = 0;

        while ((length = stream.read(buff)) > 0) {
            out.write(buff, 0, length);
        }
        stream.close();
        out.close();
        out.flush();
    }


    public boolean sendExcel(Long id, String formType) throws IOException {
        if (id == null) {
            return false;
        }
        DepotHead depotHead = depotHeadMapper.selectByPrimaryKey(id);
        if (depotHead == null) {
            return false;
        }

        try {
            //客户
            Long customer = depotHead.getOrganId();
            if (customer == null) {
                return false;
            }
            Supplier supplier = supplierService.getSupplier(customer);
            if (supplier == null || StringUtil.isEmpty(supplier.getEmail())) {
                return false;
            }
            //生成的excel文件路径
            String filePath = generateExcel(id);
            File file = new File(filePath);
            if (!file.exists()) {
                return false;
            }
            EmailUtil emailUtil = new EmailUtil();
            emailUtil.sendHtmlMail(supplier.getEmail(), depotHead.getName() + "-" + formType, "", file);
        } catch (Exception e) {

        }
        return true;
    }

}
