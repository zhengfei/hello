package com.test.hello;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Test {

    public static final String PROJECT_DIR = "/Users/zaffy/work/app.shihuimiao/src/";
    public static void main(String[] args) {
        1
                2

//        LintImage lintImage = new LintImage(
//                "E:\\Working3\\Air\\app\\Module\\air_purifier_131",
//                "E:\\Working3\\Air\\app\\Module\\air_purifier_131\\Components\\Images",
//                false);
//
//        if (args.length == 3) {
//            new LintImage(args[0], args[1], Boolean.valueOf(args[2]));
//        } else {
//            new LintImage(args[0], args[1], false);
//        }

        ImageUtil imageUtil = new ImageUtil();
        List<String> list = imageUtil.startFindImagePath();

    }

}

class LintImage implements PatternWebp.FindCallback {
    private String projectPath;
    private String projectImagesPath;
    private boolean delete = false;

    private ConcurrentSkipListSet<String> allImagesMap;

    private List<String> allJSFiles = new ArrayList<>();
    public static AtomicInteger counter = new AtomicInteger(0);
    private ExecutorService threadPool = Executors.newFixedThreadPool(5);

    public LintImage(String projectPath, String projectImagesPath, boolean delete) {
        this.projectPath = projectPath;
        this.projectImagesPath = projectImagesPath;
        this.delete = delete;

        allImagesMap = new ConcurrentSkipListSet<>();

        System.out.println("\n\n");
        System.out.println("==========扫描js文件===========");
        //获取js文件
        getJSFiles(this.projectPath);

        counter.set(this.allJSFiles.size());

        System.out.println("\n\n");
        System.out.println("===========查找png===========");
        //读取文件，查找webp
        for (String path : allJSFiles) {
            String doc = readJSFile(path);
            PatternWebp patternWebp = new PatternWebp(doc, this);
            threadPool.execute(patternWebp);
        }

        threadPool.shutdown();
    }

    private void getJSFiles(String path) {
        File parent = new File(path);
        if (parent.exists()) {
            File[] files = parent.listFiles();
            for (File f : files) {
                if (f.isFile()) {
                    if (f.getName().endsWith(".js") || f.getName().endsWith(".ccs")) {
                        allJSFiles.add(f.getAbsolutePath());
                        System.out.println("add file:\t" + f.getAbsolutePath());
                    }
                } else {
                    getJSFiles(f.getPath());
                }
            }
        } else {
            System.err.println("parent path not exits!");
        }
    }

    public String readJSFile(String path) {
        StringBuffer content = new StringBuffer();
        File jsFile = new File(path);
        if (jsFile.exists()) {
            try {
                FileInputStream inputStream = new FileInputStream(jsFile);
                byte[] buffer = new byte[1024];
                int len = 0;
                while ((len = inputStream.read(buffer)) != -1) {
                    content.append(new String(buffer, 0, len));
                }
                inputStream.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return content.toString();
    }

    @Override
    public void onTarget(String png) {
        System.out.println("find png:\t" + png);
        if (!allImagesMap.contains(png)) {
            allImagesMap.add(png);
        }
    }

    @Override
    public void onFilter() {
        filter();
    }

    //过滤不在集合的图片
    private void filter() {
        System.out.println("\n\n");
        System.out.println("==========需要删除的图片=========");

        //进行images下的图片进行操作
        File sourceImage = new File(this.projectImagesPath);
        if (sourceImage.exists()) {
            File[] fs = sourceImage.listFiles();

            for (File item : fs) {
                if (item.getName().endsWith(".webp") || item.getName().endsWith(".webp")) {
                    if (!allImagesMap.contains(item.getName())) {
                        if (this.delete) {
                            System.out.println("delete file:\t" + item.getAbsolutePath());
                            item.deleteOnExit();
                        } else {
                            System.out.println("should delete:\t" + item.getAbsolutePath());
                        }
                    }
                }

            }
        } else {
            System.err.println("file not exits:" + this.projectImagesPath);
        }
    }

}
class PatternWebp implements Runnable {

    private String doc;
    private FindCallback callback;

    PatternWebp(String doc, FindCallback callback) {
        this.doc = doc;
        this.callback = callback;
    }

    private void clear() {
        this.doc = null;
    }

    @Override
    public void run() {
        Pattern pattern = Pattern.compile("[\"'].[^:\\s,\"']*\\.webp[\"']");
        Matcher matcher = pattern.matcher(this.doc);

        while (matcher.find()) {
            String target = doc.substring(matcher.start(), matcher.end());
            if (this.callback != null) {
                target = target.substring(target.lastIndexOf("/") + 1, target.length() - 1);
                this.callback.onTarget(target);
            }
        }

        if (LintImage.counter.decrementAndGet() == 0) {
            this.callback.onFilter();
        }
    }

    interface FindCallback {

        void onTarget(String png);

        void onFilter();
    }



}

class ImageUtil {
    // 获取所有有图片的路径
    public List<String> startFindImagePath(){
        File file = new File(Test.PROJECT_DIR);
        List<String> webpPath = new ArrayList<>();
        findImagePath(webpPath,file);
        return webpPath;
    }

    private void findImagePath(List<String> webpPath,File file){
        if(file.isDirectory()){
            File[] files = file.listFiles();
            if(files!=null && files.length>0){
                for(File childFile: files){
                    findImagePath(webpPath,childFile);
                }
            }
        }else{
            if(file.getName().toLowerCase().endsWith(".webp")){
                webpPath.add(file.getPath());
                System.out.println(file.getPath());
            }
        }
    }


    class WebpFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            return pathname.getName().toLowerCase().endsWith(".webp");
        }
    }
}

