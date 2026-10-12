package com.scse.curriculum.syllabus.importer.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class PdfOcrFallbackTest {
    @TempDir Path temporary;

    @Test void defaultOcrCommandDrainsOutputLargerThanTheProcessPipe() throws Exception {
        boolean windows=System.getProperty("os.name").startsWith("Windows");
        String java=Path.of(System.getProperty("java.home"),"bin",windows?"java.exe":"java").toString();
        String classpath=System.getProperty("surefire.test.class.path",System.getProperty("java.class.path"));
        Path executable=temporary.resolve(windows?"noisy-ocr.cmd":"noisy-ocr.sh");
        String command=windows
                ? "@echo off\r\nchcp 65001 >nul\r\n@\""+java+"\" -cp \""+classpath+"\" \""+PdfOcrFallbackTest.class.getName()+"\" emit\r\n"
                : "#!/bin/sh\nexec '"+java.replace("'","'\\''")+"' -cp '"+classpath.replace("'","'\\''")+"' '"+PdfOcrFallbackTest.class.getName()+"' emit\n";
        Files.writeString(executable,command,StandardCharsets.UTF_8);
        if(!windows)assertTrue(executable.toFile().setExecutable(true));
        Path output=temporary.resolve("runner-output.txt");
        ProcessBuilder builder=new ProcessBuilder(java,"-Xmx192m","-Djava.io.tmpdir="+temporary,
                "-Dpdfbox.fontcache="+temporary,"-cp",classpath,PdfOcrFallbackTest.class.getName(),"extract")
                .redirectErrorStream(true).redirectOutput(output.toFile());
        builder.environment().put("TESSERACT_COMMAND",executable.toString());
        Process runner=builder.start();
        try {
            assertTrue(runner.waitFor(25,TimeUnit.SECONDS),"OCR waited on a full stdout pipe");
            assertEquals(0,runner.exitValue(),Files.readString(output));
            assertTrue(Files.readString(output).contains("OCR_OUTPUT_PRESERVED"));
        } finally { runner.destroyForcibly(); }
    }

    public static void main(String[] args) throws Exception {
        String largeOutput="verified source text\n".repeat(32768);
        if(args[0].equals("emit")) { System.out.print(largeOutput); return; }
        try(var document=new PDDocument()) {
            document.addPage(new PDPage());
            String actual=new PdfOcrFallback().extract(document,1,1);
            if(!actual.equals(largeOutput+"\n"))throw new AssertionError("OCR source text changed");
            System.out.println("OCR_OUTPUT_PRESERVED");
        }
    }
}
