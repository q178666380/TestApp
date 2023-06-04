package com.tencent.zebra.util;

public class Tea {
    public static String key = "ZeDA32%dkn_va4dAjg";

    static {
        System.loadLibrary("tea");
    }

    public static byte[] decryptUsingTea(byte[] pData){
        return Tea.decryptUsingTea(pData,pData.length,Tea.key.getBytes());
    }

    public static native byte[] decryptUsingTea(byte[] paramArrayOfByte1, int paramInt, byte[] paramArrayOfByte2);

    public static byte[] encryptUsingTea(byte[] pData){
        return Tea.encryptUsingTea(pData,pData.length,Tea.key.getBytes());
    }

    public static native byte[] encryptUsingTea(byte[] paramArrayOfByte1, int paramInt, byte[] paramArrayOfByte2);
}
