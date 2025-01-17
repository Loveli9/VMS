package com.icss.mvp.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;

import com.icss.mvp.entity.FtpInfo;

public class FtpUtil {
    
    private static Logger logger=Logger.getLogger(FtpUtil.class);
    
    private static FTPClient ftp;
    private static Set<String> fileNames;

    static {
        init();
    }
    private static void init(){
        fileNames=new HashSet<>();
        fileNames.add("build_summary.xml");
        fileNames.add("CodeDEX");
        fileNames.add("CodeDEX.xml");
        fileNames.add("UADPGuarding");
        fileNames.add("UADPGuarding.xml");
        fileNames.add("sourcemonitor");
        fileNames.add("sourcemonitor.xml");
        fileNames.add("CsecCheck");
        fileNames.add("CsecCheck.xml");
        fileNames.add("cct");
        fileNames.add("cct.xml");
        fileNames.add("py_complexity");
        fileNames.add("py_complexity.xml");
    }
    /**
     * 获取ftp连接
     * @param f
     * @return
     * @throws IOException 
     * @throws Exception
     */
    public static boolean connectFtp(FtpInfo f){
    	boolean flag = false;
        try {
			ftp=new FTPClient();
			//ftp.enterLocalPassiveMode();//避免ftp服务处于假死状态
			flag = false;
			int reply;
			if (f.getPort()==null) {
			    ftp.connect(f.getIpAddr(),21);
			}else{
			    ftp.connect(f.getIpAddr(),f.getPort());
			}
			ftp.login(f.getUserName(), f.getPwd());
			ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
			reply = ftp.getReplyCode();      
			if (!FTPReply.isPositiveCompletion(reply)) {      
			      ftp.disconnect();      
			      return flag;      
			}      
			ftp.changeWorkingDirectory(f.getPath());      
			flag = true;
		} catch (Exception e) {
			logger.error("服务连接失败："+e.getMessage());
		}
		return flag;
    }
    
    /**
     * 关闭ftp连接
     */
    public static void closeFtp(){
        if (ftp!=null && ftp.isConnected()) {
            try {
                ftp.logout();
                ftp.disconnect();
            } catch (IOException e) {
                logger.error("ftp.disconnect exception, error: "+e.getMessage());
            }
        }
    }
    
    /**
     * ftp上传文件
     * @param f
     * @throws Exception
     */
    public static void upload(File f) throws Exception{
        if (f.isDirectory()) {
            ftp.makeDirectory(f.getName());
            ftp.changeWorkingDirectory(f.getName());
            String[] files=f.list();
            for(String fstr : files){
                File file1=new File(f.getPath()+"/"+fstr);
                if (file1.isDirectory()) {
                    upload(file1);
                    ftp.changeToParentDirectory();
                }else{
                    File file2=new File(f.getPath()+"/"+fstr);
                    FileInputStream input=new FileInputStream(file2);
                    ftp.storeFile(file2.getName(),input);
                    input.close();
                }
            }
        }else{
            File file2=new File(f.getPath());
            FileInputStream input=new FileInputStream(file2);
            ftp.storeFile(file2.getName(),input);
            input.close();
        }
    }
    
    /**
     * 下载链接配置
     * @param f
     * @param localBaseDir 本地目录
     * @param remoteBaseDir 远程目录
     * @throws Exception
     */
    public static int startDown(FtpInfo f,String localBaseDir,String remoteBaseDir,int type) throws Exception{
    	int flag = 0;
    	if (fileNames == null || fileNames.isEmpty() ){
    	    init();
        }
    	if (FtpUtil.connectFtp(f)) {
            try { 
                FTPFile[] files = null; 
               //判断远程服务是否包含此文件 路径
                boolean changedir = ftp.changeWorkingDirectory(remoteBaseDir); 
                if (changedir) { 
                    ftp.setControlEncoding("GBK"); 
                    files = ftp.listFiles(); 
                    for (int i = 0; i < files.length; i++) { 
                        try{ 
                        	if(files[i].getName().contains("build") && files[i].getName().contains("log") && !files[i].isFile()) {
                        		downloadFile(files[i], localBaseDir, remoteBaseDir); 
                        	}else if(files[i].getName().contains("coverage_result.xml")&& files[i].isFile()) {
                        		downloadCoverageFile(files[i], localBaseDir, remoteBaseDir); 
                        	}else{
                        		continue;
                        	}
                        }catch(Exception e){ 
                            logger.error(e); 
                            logger.error("<"+files[i].getName()+">下载失败"); 
                        } 
                    } 
                }else {
                	flag = type;
                	if(type == 1) {
                		logger.error("版本构建文件夹名称不存在，请检查配置"); 
                	}else if(type == 2){
                		logger.error("工程能力文件夹名称不存在，请检查配置"); 
                	}else {
                		logger.info("coverage文件夹不存在，请检查配置");
                	}
                } 
            } catch (Exception e) { 
                logger.error(e); 
                logger.error("下载过程中出现异常"); 
            } 
        }else{
            logger.error("链接失败！");
        }
		return flag;
        
    }
    
    /** 
     * 下载FTP文件 
     * 当你需要下载FTP文件的时候，调用此方法 
     * 根据<b>获取的文件名，本地地址，远程地址</b>进行下载 
     * 
     * @param ftpFile 
     * @param relativeLocalPath 
     * @param relativeRemotePath 
     */ 
    private  static void downloadFile(FTPFile ftpFile, String relativeLocalPath,String relativeRemotePath) { 
        if (ftpFile.isFile()) {
            if (ftpFile.getName().indexOf("?") == -1) { 
                OutputStream outputStream = null; 
                try { 
                    File locaFile= new File(relativeLocalPath+ ftpFile.getName()); 
                    //判断文件是否存在，存在则返回 
                    if(locaFile.exists()){
                        return; 
                    }else{ 
                        outputStream = new FileOutputStream(relativeLocalPath+ ftpFile.getName()); 
                        ftp.retrieveFile(ftpFile.getName(), outputStream); 
                        outputStream.flush(); 
                        outputStream.close(); 
                    } 
                } catch (Exception e) { 
                    logger.error(e);
                } finally { 
                    try { 
                        if (outputStream != null){ 
                            outputStream.close(); 
                        }
                    } catch (IOException e) { 
                       logger.error("输出文件流异常"); 
                    } 
                } 
            } 
        } else { 
            String newlocalRelatePath = relativeLocalPath + ftpFile.getName(); 
            String newRemote = new String(relativeRemotePath+ ftpFile.getName().toString()); 
            File fl = new File(newlocalRelatePath); 
            if (!fl.exists()) { 
                fl.mkdirs(); 
            } 
            try { 
                newlocalRelatePath = newlocalRelatePath + '/'; 
                newRemote = newRemote + "/"; 
                String currentWorkDir = ftpFile.getName().toString(); 
                boolean changedir = ftp.changeWorkingDirectory(currentWorkDir); 
                if (changedir) { 
                    FTPFile[] files = null; 
                    files = ftp.listFiles(); 
                    for (int i = 0; i < files.length; i++) {
                    	if(fileNames.contains(files[i].getName()) || (files[i].getName().startsWith("st_")&& files[i].getName().indexOf("-")==-1  && files[i].getName().endsWith(".xml"))) {
                    		downloadFile(files[i], newlocalRelatePath, newRemote); 
                    	}
                    } 
                } 
                if (changedir){
                    ftp.changeToParentDirectory(); 
                } 
            } catch (Exception e) { 
                logger.error(e);
            } 
        } 
    } 
    private  static void downloadCoverageFile(FTPFile ftpFile, String relativeLocalPath,String relativeRemotePath) { 
    	if (ftpFile.isFile()) {
            if (ftpFile.getName().indexOf("?") == -1) { 
                OutputStream outputStream = null; 
                try { 
                    File locaFile = new File(relativeLocalPath+ ftpFile.getName()); 
                    outputStream = new FileOutputStream(locaFile); 
                    ftp.retrieveFile(ftpFile.getName(), outputStream); 
                    outputStream.flush(); 
                    outputStream.close();
                } catch (Exception e) { 
                    logger.error(e);
                } finally { 
                    try { 
                        if (outputStream != null){ 
                            outputStream.close(); 
                        }
                    } catch (IOException e) { 
                       logger.error("输出文件流异常"); 
                    } 
                } 
            } 
        }
    }
    
}
