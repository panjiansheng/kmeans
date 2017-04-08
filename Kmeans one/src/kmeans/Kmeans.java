package kmeans;


import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;


public class Kmeans {
    
    public static class Map extends Mapper<LongWritable, Text, IntWritable, Text>{

        //���ļ���
        ArrayList<ArrayList<Double>> centers = null;
        //��k������
        int k = 0;
        
        //��ȡ����
        protected void setup(Context context) throws IOException,
                InterruptedException {
            centers = Utils.getCentersFromHDFS(context.getConfiguration().get("centersPath"),false);
            k = centers.size();
        }


        /**
         * 1.ÿ�ζ�ȡһ��Ҫ���������¼���������Աȣ����ൽ��Ӧ������
         * 2.������IDΪkey�����İ����ļ�¼Ϊvalue���(���磺 1 0.2 ��  1Ϊ�������ĵ�ID��0.2Ϊ�����������ĵ�ĳ��ֵ)
         */
        protected void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {
            //��ȡһ������
            ArrayList<Double> fileds = Utils.textToArray(value);
            int sizeOfFileds = fileds.size();
            
            double minDistance = 99999999;
            int centerIndex = 0;
            
            //����ȡ��k�����ĵ��뵱ǰ��ȡ�ļ�¼������
            for(int i=0;i<k;i++){
                double currentDistance = 0;
                for(int j=0;j<sizeOfFileds;j++){
                    double centerPoint = Math.abs(centers.get(i).get(j));
                    double filed = Math.abs(fileds.get(j));
                    currentDistance += Math.pow((centerPoint - filed) / (centerPoint + filed), 2);
                }
                //ѭ���ҳ�����ü�¼��ӽ������ĵ��ID
                if(currentDistance<minDistance){
                    minDistance = currentDistance;
                    centerIndex = i;
                }
            }
            //�����ĵ�ΪKey ����¼ԭ�����
            context.write(new IntWritable(centerIndex+1), value);
        }
        
    }
    
    //����reduce�Ĺ鲢����������ΪKey����¼�鲢��һ��
    public static class Reduce extends Reducer<IntWritable, Text, Text, Text>{

        /**
         * 1.KeyΪ�������ĵ�ID valueΪ�����ĵļ�¼����
         * 2.�������м�¼Ԫ�ص�ƽ��ֵ������µ�����
         */
        protected void reduce(IntWritable key, Iterable<Text> value,Context context)
                throws IOException, InterruptedException {
            ArrayList<ArrayList<Double>> filedsList = new ArrayList<ArrayList<Double>>();
            
            //���ζ�ȡ��¼����ÿ��Ϊһ��ArrayList<Double>
            for(Iterator<Text> it =value.iterator();it.hasNext();){
                ArrayList<Double> tempList = Utils.textToArray(it.next());
                filedsList.add(tempList);
            }
            
            //�����µ�����
            //ÿ�е�Ԫ�ظ���
            int filedSize = filedsList.get(0).size();
            double[] avg = new double[filedSize];
            for(int i=0;i<filedSize;i++){
                //��û�е�ƽ��ֵ
                double sum = 0;
                int size = filedsList.size();
                for(int j=0;j<size;j++){
                    sum += filedsList.get(j).get(i);
                }
                avg[i] = sum / size;
            }
            context.write(new Text("") , new Text(Arrays.toString(avg).replace("[", "").replace("]", "")));
        }
        
    }
    
    @SuppressWarnings("deprecation")
    public static void run(String centerPath,String dataPath,String newCenterPath,boolean runReduce) throws IOException, ClassNotFoundException, InterruptedException{
        
        Configuration conf = new Configuration();
        conf.set("centersPath", centerPath);
        
        Job job = new Job(conf, "mykmeans");
        job.setJarByClass(Kmeans.class);
        
        job.setMapperClass(Map.class);
        //job.setNumReduceTasks(3);
        job.setMapOutputKeyClass(IntWritable.class);
        job.setMapOutputValueClass(Text.class);

        if(runReduce){
            //��������������Ҫreduce
            job.setReducerClass(Reduce.class);
            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(Text.class);
        }
        
        FileInputFormat.addInputPath(job, new Path(dataPath));
        
        FileOutputFormat.setOutputPath(job, new Path(newCenterPath));
        
        System.out.println(job.waitForCompletion(true));
    }

    public static void main(String[] args) throws ClassNotFoundException, IOException, InterruptedException {
        String centerPath = "hdfs://192.168.37.100:9000/input/oldCenters.data";
        String dataPath = "hdfs://192.168.37.100:9000/input/wine.txt";
        String newCenterPath = "hdfs://192.168.37.100:9000/input/outkmean";
        
        int count = 0;
        
        
        while(true){
            run(centerPath,dataPath,newCenterPath,true);
            System.out.println(" �� " + ++count + " �μ��� ");
            if(Utils.compareCenters(centerPath,newCenterPath )){
                run(centerPath,dataPath,newCenterPath,false);
                break;
            }
        }
    }
    
}