package com.victrix.plugins;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
            int i = 0;
            int len = 0;
            int ret_offset = 0;
            StringBuilder sb = new StringBuilder();
            do {
                byte[] buf = new byte[1024];
                i = fis.read(buf);
                for (byte b : buf) {
                    sb.append(String.format("0x%02X", b));
                    ret_offset++;
                    if (ret_offset > 12) {
                        sb.append(",\n");
                        ret_offset=0;
                    }  else {
                        sb.append(",");
                    }
                }
                fos.write(sb.toString().getBytes());
                sb.setLength(0);
                len += i;
            } while (i != -1);
            fos.write("};\n".getBytes());
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
}
