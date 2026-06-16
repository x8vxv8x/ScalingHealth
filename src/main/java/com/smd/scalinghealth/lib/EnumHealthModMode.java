package com.smd.scalinghealth.lib;

public enum EnumHealthModMode {
    ADD(0), MULTI(1), MULTI_HALF(1), MULTI_QUARTER(1);

    public final int op;

    EnumHealthModMode(int op) {
        this.op = op;
    }
}
