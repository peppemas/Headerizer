package com.victrix.plugins;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class Headerizer extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        FileChooserDescriptor fcd = new FileChooserDescriptor(true,
        false,
        false,
        false,
        false,
        false).withDescription("test").withTitle("Choose a file").withTreeRootVisible(true);
        FileChooser.chooseFile(fcd, e.getProject(), null, virtualFile -> {
            binary2header(virtualFile.getCanonicalPath(), e.getProject().getBasePath());
        });

    }

    /**
     * create a file like this:
        unsigned char buffer[] = {
                0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x20, 0x57, 0x6f, 0x72, 0x6c, 0x64, 0x21,
                0x0a
        };
        unsigned int buffer_len = 13;
     **/
    private void binary2header(String sourcePath, String destBasePath) {
        try {
            File finput = new File(sourcePath);
            finput.setReadable(true);
            FileInputStream fis = new FileInputStream(finput);

            String filename = Optional.of(getFilenameNoExt(finput.getName()).orElse(finput.getName())).get();
            File foutput = new File(destBasePath+"/"+filename+".h");
            foutput.setWritable(true);
            FileOutputStream fos = new FileOutputStream(foutput);

            fos.write("unsigned char buffer[] = {\n".getBytes());
            int readed = 0;
            int len = 0;
            int ret_offset = 0;
            StringBuilder sb = new StringBuilder();
            StringBuilder ascii = new StringBuilder();
            do {
                byte[] buf = new byte[1024];
                readed = fis.read(buf);
                for (int i=0; i<readed; i++) {
                    ascii.append((char)buf[i]);
                    sb.append(String.format("0x%02X", buf[i]));
                    ret_offset++;
                    if (ret_offset > 12) {
                        sb.append(",").append("\t\t// ").append(PrettyAscii(ascii.toString())).append("\n");
                        ascii = new StringBuilder();
                        ret_offset=0;
                    }  else {
                        sb.append(",");
                    }
                }
                fos.write(sb.toString().getBytes());
                sb.setLength(0);
                len += readed;
            } while (readed != -1);

            if (ascii.length() > 0) {
                fos.write("\t\t// ".getBytes());
                fos.write(PrettyAscii(ascii.toString()).getBytes());
            }
            fos.write("\n};\n".getBytes());
            fos.write("unsigned int buffer_len = ".getBytes());
            fos.write(Integer.toString(len).getBytes());
            fos.write(";\n".getBytes());
            fos.flush();

            fis.close();
            fos.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public Optional<String> getFilenameNoExt(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(0, filename.lastIndexOf(".")));
    }

    private String PrettyAscii(String original) {
        //char[] output = StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(original)).array();

        // manage all non ASCII chars
        original = original.replaceAll("[^\\x00-\\x7F]",".");

        // manage all control chars
        original = original.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]",".");

        // manage all non-printable Unicode
        original = original.replaceAll("\\p{C}", ".");

        return original;
    }
}
