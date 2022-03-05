package com.jsh.erp.datasource.entities;

import java.math.BigDecimal;
import java.util.List;

public class MaterialVo4Unit extends Material{

    private String unitName;

    private String categoryName;

    private String materialOther;

    private BigDecimal stock;

    private BigDecimal purchaseDecimal;

    private BigDecimal dropshippingDecimal;

    private BigDecimal costDecimal;

    private BigDecimal commodityDecimal;

    private BigDecimal wholesaleDecimal;

    private BigDecimal lowDecimal;

    private BigDecimal billPrice;

    private String mBarCode;

    private String commodityUnit;

    private Long meId;

    private BigDecimal initialStock;

    private BigDecimal currentStock;

    private BigDecimal currentStockPrice;

    private String sku;

    private Long depotId;

    private String links;

    private Long supplierId;

    private String supplierName;

    private String normalTaxRate;

    private String noTaxRate;

    private String specialTaxRate;

    public String getNormalTaxRate() {
        return normalTaxRate;
    }

    public void setNormalTaxRate(String normalTaxRate) {
        this.normalTaxRate = normalTaxRate;
    }

    public String getNoTaxRate() {
        return noTaxRate;
    }

    public void setNoTaxRate(String noTaxRate) {
        this.noTaxRate = noTaxRate;
    }

    public String getSpecialTaxRate() {
        return specialTaxRate;
    }

    public void setSpecialTaxRate(String specialTaxRate) {
        this.specialTaxRate = specialTaxRate;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public BigDecimal getCostDecimal() {
        return costDecimal;
    }

    public void setCostDecimal(BigDecimal costDecimal) {
        this.costDecimal = costDecimal;
    }

    public BigDecimal getDropshippingDecimal() {
        return dropshippingDecimal;
    }

    public void setDropshippingDecimal(BigDecimal dropshippingDecimal) {
        this.dropshippingDecimal = dropshippingDecimal;
    }

    public String getLinks() {
        return links;
    }

    public void setLinks(String links) {
        this.links = links;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getMaterialOther() {
        return materialOther;
    }

    public void setMaterialOther(String materialOther) {
        this.materialOther = materialOther;
    }

    public BigDecimal getStock() {
        return stock;
    }

    public void setStock(BigDecimal stock) {
        this.stock = stock;
    }

    public BigDecimal getPurchaseDecimal() {
        return purchaseDecimal;
    }

    public void setPurchaseDecimal(BigDecimal purchaseDecimal) {
        this.purchaseDecimal = purchaseDecimal;
    }

    public BigDecimal getCommodityDecimal() {
        return commodityDecimal;
    }

    public void setCommodityDecimal(BigDecimal commodityDecimal) {
        this.commodityDecimal = commodityDecimal;
    }

    public BigDecimal getWholesaleDecimal() {
        return wholesaleDecimal;
    }

    public void setWholesaleDecimal(BigDecimal wholesaleDecimal) {
        this.wholesaleDecimal = wholesaleDecimal;
    }

    public BigDecimal getLowDecimal() {
        return lowDecimal;
    }

    public void setLowDecimal(BigDecimal lowDecimal) {
        this.lowDecimal = lowDecimal;
    }

    public BigDecimal getBillPrice() {
        return billPrice;
    }

    public void setBillPrice(BigDecimal billPrice) {
        this.billPrice = billPrice;
    }

    public String getmBarCode() {
        return mBarCode;
    }

    public void setmBarCode(String mBarCode) {
        this.mBarCode = mBarCode;
    }

    public String getCommodityUnit() {
        return commodityUnit;
    }

    public void setCommodityUnit(String commodityUnit) {
        this.commodityUnit = commodityUnit;
    }

    public Long getMeId() {
        return meId;
    }

    public void setMeId(Long meId) {
        this.meId = meId;
    }

    public BigDecimal getInitialStock() {
        return initialStock;
    }

    public void setInitialStock(BigDecimal initialStock) {
        this.initialStock = initialStock;
    }

    public BigDecimal getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(BigDecimal currentStock) {
        this.currentStock = currentStock;
    }

    public BigDecimal getCurrentStockPrice() {
        return currentStockPrice;
    }

    public void setCurrentStockPrice(BigDecimal currentStockPrice) {
        this.currentStockPrice = currentStockPrice;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Long getDepotId() {
        return depotId;
    }

    public void setDepotId(Long depotId) {
        this.depotId = depotId;
    }
}