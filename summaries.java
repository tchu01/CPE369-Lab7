// Author: Timothy Chu & Michael Wong
// Lab 7
// CPE369 - Section 01

import com.alexholmes.json.mapreduce.MultiLineJsonInputFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.json.JSONObject;

import java.io.IOException;

public class summaries extends Configured implements Tool {

   public static class JsonMapper
         extends Mapper<LongWritable, Text, Text, Text> {

      @Override
      public void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {
         try {
            JSONObject json = new JSONObject(value.toString());
            context.write(new Text(json.getInt("game") + ""), value);
         } catch (Exception e) {
            System.out.println(e);
         }
      }
   }

   public static class JsonReducer
         extends Reducer<Text, Text, Text, Text> {

      @Override
      public void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

         try {
            String userID = "";
            int movesSum = 0;
            int regularMovesSum = 0;
            int specialMovesSum = 0;
            String outcome = "In Progress";
            int score = 0;

            for (Text val : values) {
               JSONObject json = new JSONObject(val.toString());
               userID = json.getString("user");
               if (json.has("action")) {
                  movesSum++;
                  JSONObject action = json.getJSONObject("action");
                  if (action.has("actionType")) {
                     String actionType = action.getString("actionType");
                     if (actionType.equals("Move")) {
                        regularMovesSum++;
                     } else if (actionType.equals("SpecialMove")) {
                        specialMovesSum++;
                     } else if (actionType.equals("GameEnd")) {
                        if (action.has("gameStatus")) {
                           outcome = action.getString("gameStatus");
                        }
                     }

                     if (action.has("points")) {
                        score += action.getInt("points");
                     }
                  }
               }
            }

            String val = "user: " + userID + ", moves: " + movesSum + ", regular: " + regularMovesSum + ", special: " + specialMovesSum + ", outcome: " + outcome + ", score: " + score + ", perMove: " + ((score * 1.0) / movesSum);
            context.write(key, new Text(val));

         } catch (Exception e) {
            throw new InterruptedException(e.toString());
//            System.out.println(e);
         }
      }
   }

   @Override
   public int run(String[] args) throws Exception {
      Configuration conf = super.getConf();
      Job job = Job.getInstance(conf, "multiline json job");

      job.setJarByClass(summaries.class);
      job.setMapperClass(JsonMapper.class);
      job.setReducerClass(JsonReducer.class);
      job.setMapOutputKeyClass(Text.class);
      job.setMapOutputValueClass(Text.class);
      job.setOutputKeyClass(Text.class);
      job.setOutputValueClass(Text.class);
      job.setInputFormatClass(MultiLineJsonInputFormat.class);
      MultiLineJsonInputFormat.setInputJsonMember(job, "action");

      FileInputFormat.addInputPath(job, new Path(args[0]));
      FileOutputFormat.setOutputPath(job, new Path(args[1]));

      return job.waitForCompletion(true) ? 0 : 1;
   }

   public static void main(String[] args) throws Exception {
      //RUN JSON MAP-REDUCE JOB
      Configuration conf = new Configuration();
      int res = ToolRunner.run(conf, new summaries(), args);
      System.exit(res);
   }
}