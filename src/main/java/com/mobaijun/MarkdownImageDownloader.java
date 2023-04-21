package src.main.java.com.mobaijun;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * software：IntelliJ IDEA 2023.1.1<br>
 * class name: MarkdownImageDownloader<br>
 * class description: markdown 图片下载程序
 *
 * @author MoBaiJun 2023/4/21 17:23
 */
public class MarkdownImageDownloader {

    /**
     * 存储图片的文件夹名称
     */
    private static final String IMG_FOLDER = "img/other/C语言程序设计";

    /**
     * 正则表达式，用于匹配markdown中的图片链接
     */
    private static final String IMG_REGEX = "!\\[.*?\\]\\((http.*?\\.(png|jpe?g|gif|svg))\\)";

    /**
     * 超时时间，单位为毫秒
     */
    private static final int TIMEOUT = 5000;

    /**
     * 线程数
     */
    private static final int THREAD_NUM = 10;

    /**
     * 线程池
     */
    static ExecutorService executorService = Executors.newFixedThreadPool(THREAD_NUM);


    public static void main(String[] args) {
        String dirPath = "D:\\ideaProject\\april-projects\\.docs";
        String img = "D:\\ideaProject\\april-projects\\.docs\\img\\other\\C语言程序设计";
        File dir = new File(dirPath);
        List<File> markdownFiles = new ArrayList<>();
        scanMarkdownFiles(dir, markdownFiles, "src", "img", "_book", "codesytel",
                "JUC", "算法", "out", "node_modules", "push.cmd", ".git", ".idea", "LICENSE", "SpringCloud", "Linux", "jvm", "SUMMARY.md", "README.md", "NIO");
        for (File file : markdownFiles) {
            processFile(file, img);
        }
        executorService.shutdown();
    }

    public static void scanMarkdownFiles(File dir, List<File> markdownFiles, String... ignoreDirs) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (File file : Objects.requireNonNull(files)) {
                boolean ignore = false;
                for (String ignoreDir : ignoreDirs) {
                    if (file.getName().equals(ignoreDir) || file.getAbsolutePath().endsWith(File.separator + ignoreDir)) {
                        ignore = true;
                        break;
                    }
                }
                if (!ignore) {
                    if (file.isDirectory()) {
                        scanMarkdownFiles(file, markdownFiles, ignoreDirs);
                    } else if (file.getName().toLowerCase().endsWith(".md")) {
                        markdownFiles.add(file);
                    }
                }
            }
        }
    }

    private static void processFile(File file, String imgDirPath) {
        SortedMap<Integer, String> imageUrls = getImageUrls(file);
        if (imageUrls.size() == 0) {
            System.out.println("No image urls found in " + file.toPath());
            return;
        }

        // create img/java/{filename} directory
        String folderName = file.getName().replace(".md", "");
        String newImgDirPath = imgDirPath + File.separator + folderName;
        File imgDir = new File(newImgDirPath);
        if (!imgDir.exists()) {
            imgDir.mkdirs();
        }

        for (Map.Entry<Integer, String> entry : imageUrls.entrySet()) {
            Integer index = entry.getKey();
            String imageUrl = entry.getValue();
            // get image extension
            String extension = imageUrl.substring(imageUrl.lastIndexOf("."));
            String imageName = index + extension;
            String imgPath = imgDir.getAbsolutePath() + File.separator + imageName;

            try {
                executorService.submit(() -> downloadImage(imageUrl, imgPath));
                String newImageUrl = "./../../" + IMG_FOLDER + "/" + folderName + "/" + imageName;
                replaceStringInFile(file, imageUrl, newImageUrl);
            } catch (Exception e) {
                System.out.println("Failed to download image " + imageUrl + " in " + file.getName() + ": " + e.getMessage());
            }
        }

        System.out.println("Processed " + imageUrls.size() + " images in " + file.getName());
    }

    private static void downloadImage(String imageUrl, String imagePath) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.setRequestMethod("GET");
            try (InputStream inputStream = connection.getInputStream();
                 OutputStream outputStream = new FileOutputStream(createPath(imagePath).toFile())) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Path createPath(String imagePath) throws Exception {
        Path path = Path.of(imagePath);
        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        return path;
    }


    private static SortedMap<Integer, String> getImageUrls(File file) {
        SortedMap<Integer, String> imageUrls = new TreeMap<>();
        try {
            Stream<String> lines = Files.lines(file.toPath());
            AtomicInteger index = new AtomicInteger(0);
            imageUrls = lines.flatMap(line -> {
                Matcher matcher = Pattern.compile(IMG_REGEX).matcher(line);
                List<String> urls = new ArrayList<>();
                while (matcher.find()) {
                    urls.add(matcher.group(1));
                }
                return urls.stream().map(url -> new AbstractMap.SimpleEntry<>(index.getAndIncrement(), url));
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (u, v) -> u, TreeMap::new));
        } catch (IOException e) {
            System.out.println("Failed to read " + file.getName() + ": " + e.getMessage());
        }
        return imageUrls;
    }

//    private static void downloadImage(String imageUrl, String imagePath) throws Exception {
//        URL url = new URL(imageUrl);
//        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//        connection.setRequestMethod("GET");
//        try (InputStream inputStream = connection.getInputStream();
//             OutputStream outputStream = new FileOutputStream(imagePath)) {
//            byte[] buffer = new byte[4096];
//            int bytesRead;
//            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                outputStream.write(buffer, 0, bytesRead);
//            }
//        }
//    }

    private static void replaceStringInFile(File file, String oldStr, String newStr) {
        try {
            // 指定文件编码
            String encoding = "UTF-8";
            Path path = file.toPath();
            // 读取文件内容
            List<String> lines = Files.readAllLines(path, Charset.forName(encoding));
            ListIterator<String> iterator = lines.listIterator();
            while (iterator.hasNext()) {
                String line = iterator.next();
                if (line.contains(oldStr)) {
                    // 替换指定字符串
                    iterator.set(line.replace(oldStr, newStr));
                }
            }
            // 将新内容写入文件
            Files.write(path, lines, Charset.forName(encoding));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
