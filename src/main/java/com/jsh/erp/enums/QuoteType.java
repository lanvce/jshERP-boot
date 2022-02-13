package com.jsh.erp.enums;

public enum QuoteType {
    BATCHPURCHASE(0),DROPSHIPPING(1);

    int type;

    private QuoteType(int type){
        type=type;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

}
