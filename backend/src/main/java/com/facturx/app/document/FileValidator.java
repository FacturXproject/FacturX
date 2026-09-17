package com.facturx.app.document;

public class FileValidator {

    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes();
    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10 Mo

    public static boolean isPdf(byte[] fileBytes) {
        if (fileBytes.length < PDF_SIGNATURE.length) return false;
        for (int i = 0; i < PDF_SIGNATURE.length; i++) {
            if (fileBytes[i] != PDF_SIGNATURE[i]) return false;
        }
        return true;
    }

    public static boolean isXml(byte[] fileBytes) {
        String start = new String(fileBytes, 0, Math.min(fileBytes.length, 100)).trim();
        return start.startsWith("<?xml") || start.startsWith("<");
    }

    public static boolean isValidType(byte[] fileBytes) {
        return isPdf(fileBytes) || isXml(fileBytes);
    }

    public static boolean isWithinSizeLimit(long size) {
        return size <= MAX_SIZE;
    }
}