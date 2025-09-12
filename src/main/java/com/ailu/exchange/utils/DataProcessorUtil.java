package com.ailu.exchange.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Slf4j
public class DataProcessorUtil {

    public static void fetchDataAndProcess(String apiUrl, String requestBody, String textFilePath, String zipFilePath) throws IOException {
        // 1. 从第三方接口获取数据
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        // 发送请求
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        // 检查响应状态
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP error code: " + responseCode);
        }
        // 处理响应
        InputStream inputStream = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder responseBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            responseBuilder.append(line);
        }
        reader.close();

        // 解析 JSON 数据并验证
        String responseBody = responseBuilder.toString();
        JSONObject jsonResponse = JSONObject.parseObject(responseBody);
        if (jsonResponse.getInteger("code") == 0) {
            // 2. 写入文本文件
            File textFile = new File(textFilePath);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(textFile))) {
                writer.write(responseBody);
            }

            // 3. 压缩文本文件
            try (FileOutputStream fos = new FileOutputStream(zipFilePath);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                ZipEntry zipEntry = new ZipEntry(textFile.getName());
                zos.putNextEntry(zipEntry);
                try (FileInputStream fis = new FileInputStream(textFile)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                }
                zos.closeEntry();
            }
            // 4. 删除原始文本文件
            if (!textFile.delete()) {
                throw new IOException("Failed to delete temporary file: " + textFile.getAbsolutePath());
            }
        } else {
            log.info("获取接口数据出现异常！！{},{}", responseBody, textFilePath);
        }
    }

    public static String extractAndReadData(String zipFilePath) throws IOException {
        StringBuilder result = new StringBuilder();
        // 1. 解压缩文件
        try (FileInputStream fis = new FileInputStream(zipFilePath);
             ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry zipEntry = zis.getNextEntry();
            if (zipEntry != null) {
                // 2. 读取文本文件内容
                BufferedReader reader = new BufferedReader(new InputStreamReader(zis));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append("\n");
                }
                reader.close();
            }
        }

        return result.toString();
    }


    public static void createUpdatedZipFile(String zipFilePath, String fileNameInZip, String newContent) throws IOException {
        File tempFile = new File(zipFilePath + ".tmp");

        try (FileInputStream fis = new FileInputStream(zipFilePath);
             ZipInputStream zis = new ZipInputStream(fis);
             FileOutputStream fos = new FileOutputStream(tempFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            ZipEntry zipEntry;
            boolean fileUpdated = false;

            // Iterate through the entries in the old ZIP file
            while ((zipEntry = zis.getNextEntry()) != null) {
                String entryName = zipEntry.getName();

                if (entryName.equals(fileNameInZip)) {
                    // Write the updated entry
                    zos.putNextEntry(new ZipEntry(fileNameInZip));
                    zos.write(newContent.getBytes());
                    zos.closeEntry();
                    fileUpdated = true;
                } else {
                    // Copy other entries as they are
                    zos.putNextEntry(new ZipEntry(entryName));
                    copyStream(zis, zos);
                    zos.closeEntry();
                }
                zis.closeEntry();
            }

            // If the file was not updated, add it to the ZIP file
            if (!fileUpdated) {
                zos.putNextEntry(new ZipEntry(fileNameInZip));
                zos.write(newContent.getBytes());
                zos.closeEntry();
            }
        }

        // Replace the original ZIP file with the updated one
        File originalFile = new File(zipFilePath);
        if (originalFile.exists() && !originalFile.delete()) {
            throw new IOException("Failed to delete the original file");
        }
        if (!tempFile.renameTo(originalFile)) {
            throw new IOException("Failed to replace the original file");
        }
    }

    private static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = input.read(buffer)) != -1) {
            output.write(buffer, 0, len);
        }
    }

    public static String readFromFile(String filePath) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content.toString();
    }



    public static void writeToFile(String filePath, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) {
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {

        String apiUrl = "http://api.tushare.pro";
        String requestBody = "{\"api_name\": \"daily\", \"token\": \"0bdf3baa700b2560364af43c4e6fff06fccf601ce4320bf390314f52" +
                "\", \"params\": {\"ts_code\":\"600519.SH\"}}";
        String textFilePath = "D:\\all\\data.txt";
        String zipFilePath = "D:\\all\\data.zip";

        try {
//            // 方法 1: 从接口获取数据并处理
//            fetchDataAndProcess(apiUrl, requestBody, textFilePath, zipFilePath);
//            // 方法 2: 解压缩并读取数据
//            String data = extractAndReadData(zipFilePath);
//            System.out.println(data);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
