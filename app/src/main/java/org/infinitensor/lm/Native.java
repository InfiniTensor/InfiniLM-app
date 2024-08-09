package org.infinitensor.lm;

public class Native {
    // 加载模型并启动推理服务，必须最先调用。
    public native static void init(String model_path);
    // 开始对话。
    public native static void start(String prompt);
    // 终止对话。
    public native static void abort();
    // 解码模型反馈。
    public native static String decode();
}
