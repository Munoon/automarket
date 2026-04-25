package edu.automarket.common;

public class ArrayUtils {
    public static boolean contains(String[] arr, String value) {
        for (String el : arr) {
            if (el.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
