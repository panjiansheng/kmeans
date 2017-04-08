package kmeans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.LineRecordReader.LineReader;

public class Utils {
    
    //��ȡ�����ļ�������
    public static ArrayList<ArrayList<Double>> getCentersFromHDFS(String centersPath,boolean isDirectory) throws IOException{
        
        ArrayList<ArrayList<Double>> result = new ArrayList<ArrayList<Double>>();
        
        Path path = new Path(centersPath);
        
        Configuration conf = new Configuration();
        
        FileSystem fileSystem = path.getFileSystem(conf);

        if(isDirectory){    
            FileStatus[] listFile = fileSystem.listStatus(path);
            for (int i = 0; i < listFile.length; i++) {
                result.addAll(getCentersFromHDFS(listFile[i].getPath().toString(),false));
            }
            return result;
        }
        
        FSDataInputStream fsis = fileSystem.open(path);
        LineReader lineReader = new LineReader(fsis, conf);
        
        Text line = new Text();
        
        while(lineReader.readLine(line) > 0){
            ArrayList<Double> tempList = textToArray(line);
            result.add(tempList);
        }
        lineReader.close();
        return result;
    }
    
    //ɾ���ļ�
    public static void deletePath(String pathStr) throws IOException{
        Configuration conf = new Configuration();
        Path path = new Path(pathStr);
        FileSystem hdfs = path.getFileSystem(conf);
        hdfs.delete(path ,true);
    }
    
    public static ArrayList<Double> textToArray(Text text){
        ArrayList<Double> list = new ArrayList<Double>();
        String[] fileds = text.toString().split(",");
        for(int i=0;i<fileds.length;i++){
            list.add(Double.parseDouble(fileds[i]));
        }
        return list;
    }
    
    public static boolean compareCenters(String centerPath,String newPath) throws IOException{
        
        List<ArrayList<Double>> oldCenters = Utils.getCentersFromHDFS(centerPath,false);
        List<ArrayList<Double>> newCenters = Utils.getCentersFromHDFS(newPath,true);
        
        int size = oldCenters.size();
        int fildSize = oldCenters.get(0).size();
        double distance = 0;
        for(int i=0;i<size;i++){
            for(int j=0;j<fildSize;j++){
                double t1 = Math.abs(oldCenters.get(i).get(j));
                double t2 = Math.abs(newCenters.get(i).get(j));
                distance += Math.pow((t1 - t2) / (t1 + t2), 2);
            }
        }
        
        if(distance == 0.0){
            //ɾ���µ������ļ��Ա�������ι������
            Utils.deletePath(newPath);
            return true;
        }else{
            //����������ļ������µ������ļ����Ƶ������ļ��У���ɾ�������ļ�
            
            Configuration conf = new Configuration();
            Path outPath = new Path(centerPath);
            FileSystem fileSystem = outPath.getFileSystem(conf);
            
            FSDataOutputStream overWrite = fileSystem.create(outPath,true);
            overWrite.writeChars("");
            overWrite.close();
            
            
            Path inPath = new Path(newPath);
            FileStatus[] listFiles = fileSystem.listStatus(inPath);
            for (int i = 0; i < listFiles.length; i++) {                
                FSDataOutputStream out = fileSystem.create(outPath);
                FSDataInputStream in = fileSystem.open(listFiles[i].getPath());
                IOUtils.copyBytes(in, out, 4096, true);
            }
            //ɾ���µ������ļ��Ա�ڶ��������������
            Utils.deletePath(newPath);
        }
        
        return false;
    }
}