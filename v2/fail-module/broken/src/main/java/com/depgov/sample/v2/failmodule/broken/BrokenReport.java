package com.depgov.sample.v2.failmodule.broken;

import com.depgov.sample.v2.failmodule.ok.OkReport;
import org.apache.commons.io.FilenameUtils;

public final class BrokenReport {
    private BrokenReport() {
    }

    public static void main(String[] args) {
        String extension = FilenameUtils.getExtension("a.txt");
        FilenameUtils.getExtensionX("a.txt"); // C29: intentional undefined method; delete this line to recover.
        System.out.println(OkReport.title(" report ") + "." + extension);
    }
}
