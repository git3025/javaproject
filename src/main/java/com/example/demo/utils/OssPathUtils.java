package com.example.demo.utils;

import com.example.demo.service.OssService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OssPathUtils {

    public String buildPDFPath(String isbn, String filename) {
        return "pdf/" + isbn + "/" + filename;
    }

    public String buildPagePath(String isbn, String filename) {
        return "pdf/" + isbn + "/page/" + filename;
    }

    public String buildQuestionPath(String isbn, String filename) {
        return "pdf/" + isbn + "/page/question/" + filename;
    }

    public String buildZcPath(String isbn, String filename) {
        return "pdf/" + isbn + "/page/zc/" + filename;
    }

    public String buildTempPath(String filename) {
        return "pdf/temp/" + filename;
    }


}
