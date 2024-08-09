package org.infinitensor.lm;

public class Native {
    public native static void init(String model_path);
    public native static void start(String prompt);
    public native static String decode();
}
