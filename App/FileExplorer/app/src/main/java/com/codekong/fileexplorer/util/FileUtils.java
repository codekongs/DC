package com.codekong.fileexplorer.util;

import android.os.Handler;
import android.os.Message;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by szh on 2017/2/8.
 */

public class FileUtils {
    //文件大小
    private static final long KB = 2 << 9;
    private static final long MB = 2 << 19;
    private static final long GB = 2 << 29;

    //日期格式化
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    //当前路径
    private static String mNowPath;
    private static Stack<String> mNowPathStack;

    /**
     * 获得指定文件的大小
     *
     * @param file
     * @return
     */
    public static String getFileSize(File file) {
        if (file.isFile()) {
            long fileLength = file.length();
            if (fileLength < KB) {
                return fileLength + "B";
            } else if (fileLength < MB) {
                return String.format(Locale.getDefault(), "%.2fKB", fileLength / (double) KB);
            } else if (fileLength < GB) {
                return String.format(Locale.getDefault(), "%.2fMB", fileLength / (double) MB);
            } else {
                return String.format(Locale.getDefault(), "%.2fGB", fileLength / (double) GB);
            }
        }
        return null;
    }

    /**
     * 获得文件最近修改的时间
     *
     * @param file
     * @return
     */
    public static String getFileDate(File file) {
        return dateFormat.format(new Date(file.lastModified()));
    }

    /**
     * 获得当前所在的路径栈的字符串表示
     *
     * @return
     */
    public static String getNowStackPathString(Stack<String> nowPathStack) {
        mNowPathStack = nowPathStack;
        String result = "";
        Stack<String> temp = new Stack<>();
        temp.addAll(nowPathStack);
        while (temp.size() != 0) {
            result = temp.pop() + result;
        }
        mNowPath = result;
        return result;
    }

    /**
     * 获得当前路径
     *
     * @return
     */
    public static String getNowPath() {
        return mNowPath;
    }

    /**
     * 返回上级目录
     *
     * @return
     */
    public static String returnToParentDir() {
        mNowPathStack.pop();
        return mNowPath;
    }

    /**
     * 过滤隐藏文件,并按字母序排序文件目录和文件
     *
     * @param path
     * @return
     */
    public static File[] filterSortFileByName(String path, boolean isHideFile) {

        File[] fileArray = null;
        if (isHideFile) {
            //不显示隐藏文件
            fileArray = new File(path).listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    //过滤掉隐藏文件
                    return !file.getName().startsWith(".");
                }
            });
        } else {
            //显示隐藏文件
            fileArray = new File(path).listFiles();
        }

        //文件排序
        Arrays.sort(fileArray, new Comparator<File>() {
            @Override
            public int compare(File file, File t1) {
                if (file.isDirectory() && t1.isFile()) {
                    return -1;
                } else if (file.isFile() && t1.isDirectory()) {
                    return 1;
                }
                return file.getName().compareToIgnoreCase(t1.getName());
            }
        });
        return fileArray;
    }


    /**
     * 过滤隐藏文件,并按文件大小排序
     *
     * @param path
     * @param desc
     * @return
     */
    public static File[] filterSortFileBySize(String path, final boolean desc) {

        //不显示隐藏文件
        File[] fileArray = new File(path).listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                //过滤掉隐藏文件
                return !file.getName().startsWith(".");
            }
        });
        //文件排序
        Arrays.sort(fileArray, new Comparator<File>() {
            @Override
            public int compare(File file, File t1) {
                if (file.isDirectory() && t1.isFile()) {
                    return -1;
                } else if (file.isFile() && t1.isDirectory()) {
                    return 1;
                } else if (file.isFile() && t1.isFile()) {
                    if (desc) {
                        return file.length() - t1.length() > 0 ? -1 : 1;
                    } else {
                        return file.length() - t1.length() > 0 ? 1 : -1;
                    }
                }
                return file.getName().compareToIgnoreCase(t1.getName());
            }
        });
        return fileArray;
    }

    /**
     * 按文件修改时间排序
     *
     * @param path
     * @return
     */
    public static File[] filterSortFileByLastModifiedTime(String path) {

        //不显示隐藏文件
        File[] fileArray = new File(path).listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                //过滤掉隐藏文件
                return !file.getName().startsWith(".");
            }
        });
        //文件排序
        Arrays.sort(fileArray, new Comparator<File>() {
            @Override
            public int compare(File file, File t1) {
                if (file.isDirectory() && t1.isFile()) {
                    return -1;
                } else if (file.isFile() && t1.isDirectory()) {
                    return 1;
                } else if (file.isFile() && t1.isFile() || (file.isDirectory() && t1.isDirectory())) {
                    //最新的文件排在最前面
                    return file.lastModified() - t1.lastModified() > 0 ? -1 : 1;
                }
                return file.getName().compareToIgnoreCase(t1.getName());
            }
        });
        return fileArray;
    }

    /**
     * 扫描统计文件
     *
     * @param path           扫描路径
     * @param categorySuffix key--对应类型,value-对应该类别的后缀集合
     * @return
     */
    public static void scanCountFile(String path, final Map<String, Set<String>> categorySuffix, final Handler handler) {
        final File file = new File(path);

        //非目录或者目录不存在直接返回
        if (!file.exists() || file.isFile()) {
            return;
        }

        final ExecutorService singleExecutor = Executors.newSingleThreadExecutor();
        singleExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Map<String, Integer> countRes = new HashMap<>(categorySuffix.size());
                for (String category : categorySuffix.keySet()) {
                    //将最后统计结果的key设置为类别
                    countRes.put(category, 0);
                }
                LinkedList<File> linkedList = new LinkedList<>();
                File[] files = file.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String s) {
                        //过滤掉隐藏文件
                        return !file.getName().startsWith(".");
                    }
                });

                for (File f : files) {
                    if (f.isDirectory()) {
                        //把目录添加进队列
                        linkedList.add(f);
                    } else {
                        //找到该文件所属的类别
                        for (Map.Entry<String, Set<String>> entry : categorySuffix.entrySet()) {
                            //获取文件后缀
                            String suffix = f.getName().substring(f.getName().indexOf(".") + 1).toLowerCase();
                            //找到了
                            if (entry.getValue().contains(suffix)) {
                                countRes.put(entry.getKey(), countRes.get(entry.getKey()) + 1);
                                break;
                            }
                        }
                    }
                }

                File tmpFile = null;
                while (!linkedList.isEmpty()) {
                    //队头出队列
                    tmpFile = linkedList.removeFirst();
                    files = tmpFile.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File file, String s) {
                            //过滤掉隐藏文件
                            return !file.getName().startsWith(".");
                        }
                    });
                    for (File f : files) {
                        if (f.isDirectory()) {
                            //把目录添加进队列
                            linkedList.add(f);
                        } else {
                            //找到该文件所属的类别
                            for (Map.Entry<String, Set<String>> entry : categorySuffix.entrySet()) {
                                //获取文件后缀
                                String suffix = f.getName().substring(f.getName().indexOf(".") + 1).toLowerCase();
                                //找到了
                                if (entry.getValue().contains(suffix)) {
                                    countRes.put(entry.getKey(), countRes.get(entry.getKey()) + 1);
                                    break;
                                }
                            }
                        }
                    }
                }

                Message msg = Message.obtain();
                msg.obj = countRes;
                handler.sendMessage(msg);
            }
        });
    }
}
